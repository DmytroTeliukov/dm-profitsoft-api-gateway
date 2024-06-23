package com.dteliukov.profitsoftgateway.service;

import com.dteliukov.profitsoftgateway.auth.dto.UserInfo;
import com.dteliukov.profitsoftgateway.data.UserSession;
import com.dteliukov.profitsoftgateway.exception.UnauthorizedException;
import com.dteliukov.profitsoftgateway.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpCookie;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

import static com.dteliukov.profitsoftgateway.filter.AuthenticationFilter.COOKIE_SESSION_ID;

@Service
@RequiredArgsConstructor
public class SessionService {

    public static final Duration SESSION_DURATION = Duration.ofHours(1);

    private final UserSessionRepository userSessionRepository;

    public Mono<UserSession> checkSession(ServerWebExchange exchange) {
        HttpCookie sessionCookie = exchange.getRequest().getCookies().getFirst(COOKIE_SESSION_ID);
        if (sessionCookie == null) {
            return Mono.error(new UnauthorizedException("Session Cookie not found"));
        }
        return userSessionRepository.findById(sessionCookie.getValue())
                .flatMap(session ->
                        session.isExpired()
                                ? Mono.error(new UnauthorizedException("Session expired"))
                                : Mono.just(session)
                ).switchIfEmpty(Mono.error(new UnauthorizedException("Session not found")));
    }

    public Mono<UserSession> saveSession(UserInfo userInfo) {
        return userSessionRepository.createSession(userInfo, Instant.now().plus(SESSION_DURATION));
    }

    public Mono<Void> addSessionCookie(ServerWebExchange exchange, UserSession session) {
        return Mono.fromRunnable(() -> exchange.getResponse().addCookie(ResponseCookie.from(COOKIE_SESSION_ID)
                .value(session.getId())
                .path("/")
                .maxAge(SESSION_DURATION)
               // .secure(true)
               // .httpOnly(true) // Prevents JavaScript from accessing the cookie CI/CD. Огляд - Java https://www.youtube.com/watch?v=V-52W1olwZw
                .build()));
    }


}

