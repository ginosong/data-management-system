package com.datamanagement.system.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;

    public JwtTokenService(@Value("${app.security.jwt.secret}") String secret,
                           @Value("${app.security.jwt.access-token-minutes}") long accessTokenMinutes,
                           @Value("${app.security.jwt.refresh-token-days}") long refreshTokenDays) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }

    public JwtTokenPair issueTokens(CurrentUser currentUser) {
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plus(accessTokenMinutes, ChronoUnit.MINUTES);
        Instant refreshExpiresAt = now.plus(refreshTokenDays, ChronoUnit.DAYS);

        String accessToken = buildToken(currentUser, "access", accessExpiresAt, null);
        String refreshToken = buildToken(currentUser, "refresh", refreshExpiresAt, UUID.randomUUID().toString());
        return new JwtTokenPair(
            accessToken,
            refreshToken,
            toLocalDateTime(accessExpiresAt),
            toLocalDateTime(refreshExpiresAt)
        );
    }

    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return new ParsedToken(
            Long.valueOf(claims.getSubject()),
            claims.get("type", String.class),
            claims.getExpiration().toInstant(),
            claims.getId()
        );
    }

    private String buildToken(CurrentUser currentUser, String type, Instant expiresAt, String tokenId) {
        return Jwts.builder()
            .subject(String.valueOf(currentUser.id()))
            .claim("username", currentUser.username())
            .claim("type", type)
            .id(tokenId)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(expiresAt))
            .signWith(secretKey)
            .compact();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public record JwtTokenPair(
        String accessToken,
        String refreshToken,
        LocalDateTime accessTokenExpiresAt,
        LocalDateTime refreshTokenExpiresAt
    ) {
    }

    public record ParsedToken(
        Long userId,
        String type,
        Instant expiresAt,
        String tokenId
    ) {
    }
}