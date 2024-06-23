package com.dteliukov.profitsoftgateway.filter;

import com.dteliukov.profitsoftgateway.exception.UnauthorizedException;
import com.dteliukov.profitsoftgateway.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthorizationFilter implements GlobalFilter, Ordered {

    public static final String PREFIX_API = "/api";
    private final SessionService sessionService;

    private static final List<String> FREE_ENDPOINTS = List.of(
            "/api/dishes/_list",
            "/api/reviews/_counts",
            "/api/categories"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isFreeEndpoint(path)) {
            return chain.filter(exchange);
        }
        if (exchange.getRequest().getPath().value().startsWith(PREFIX_API)) {
            return sessionService.checkSession(exchange)
                    .then(chain.filter(exchange))
                    .onErrorResume(UnauthorizedException.class, e -> sendUnauthorized(exchange));
        }
        return chain.filter(exchange);
    }

    public static Mono<Void> sendUnauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private boolean isFreeEndpoint(String path) {
        return FREE_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return -5;
    }


}

