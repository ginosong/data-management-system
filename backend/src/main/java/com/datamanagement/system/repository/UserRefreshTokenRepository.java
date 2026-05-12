package com.datamanagement.system.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datamanagement.system.domain.UserRefreshToken;

public interface UserRefreshTokenRepository extends JpaRepository<UserRefreshToken, Long> {

    void deleteByExpiresAtBefore(LocalDateTime expiresAt);

    void deleteByUserId(Long userId);

    Optional<UserRefreshToken> findByTokenAndRevokedFalse(String token);
}