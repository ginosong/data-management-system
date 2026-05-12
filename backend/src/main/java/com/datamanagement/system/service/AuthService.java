package com.datamanagement.system.service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.datamanagement.system.domain.AppUser;
import com.datamanagement.system.domain.Role;
import com.datamanagement.system.domain.TechnicalCenter;
import com.datamanagement.system.domain.UserRefreshToken;
import com.datamanagement.system.domain.UserRole;
import com.datamanagement.system.exception.UnauthorizedException;
import com.datamanagement.system.repository.AppUserRepository;
import com.datamanagement.system.repository.RoleRepository;
import com.datamanagement.system.repository.TechnicalCenterRepository;
import com.datamanagement.system.repository.UserCenterPermissionRepository;
import com.datamanagement.system.repository.UserRefreshTokenRepository;
import com.datamanagement.system.repository.UserRoleRepository;
import com.datamanagement.system.security.CurrentUser;
import com.datamanagement.system.security.CurrentUserService;
import com.datamanagement.system.security.JwtTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserCenterPermissionRepository userCenterPermissionRepository;
    private final UserRefreshTokenRepository userRefreshTokenRepository;
    private final TechnicalCenterRepository technicalCenterRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final CurrentUserService currentUserService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public Map<String, Object> login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        AppUser user = appUserRepository.findByUsername(username.trim())
            .filter(item -> Boolean.TRUE.equals(item.getEnabled()))
            .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));

        if (!passwordMatches(password.trim(), user.getPasswordHash())) {
            throw new UnauthorizedException("用户名或密码错误");
        }

        upgradeLegacyPasswordIfNeeded(user, password.trim());
        CurrentUser currentUser = loadCurrentUser(user.getId());
        JwtTokenService.JwtTokenPair tokenPair = issueTokens(currentUser);
        return buildAuthPayload(currentUser, tokenPair);
    }

    @Transactional
    public Map<String, Object> refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new UnauthorizedException("刷新令牌不能为空");
        }

        JwtTokenService.ParsedToken parsedToken = jwtTokenService.parse(refreshToken.trim());
        if (!"refresh".equals(parsedToken.type())) {
            throw new UnauthorizedException("刷新令牌无效");
        }

        UserRefreshToken storedToken = userRefreshTokenRepository.findByTokenAndRevokedFalse(refreshToken.trim())
            .orElseThrow(() -> new UnauthorizedException("刷新令牌已失效，请重新登录"));
        if (!Objects.equals(storedToken.getUserId(), parsedToken.userId()) || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            storedToken.setRevoked(Boolean.TRUE);
            throw new UnauthorizedException("刷新令牌已失效，请重新登录");
        }

        storedToken.setRevoked(Boolean.TRUE);
        CurrentUser currentUser = loadCurrentUser(parsedToken.userId());
        JwtTokenService.JwtTokenPair tokenPair = issueTokens(currentUser);
        return buildAuthPayload(currentUser, tokenPair);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> currentUser() {
        return Map.of("user", toUserPayload(currentUserService.requireCurrentUser()));
    }

    @Transactional
    public void logout(String refreshToken) {
        CurrentUser currentUser = currentUserService.requireCurrentUser();
        userRefreshTokenRepository.deleteByUserId(currentUser.id());
        if (StringUtils.hasText(refreshToken)) {
            userRefreshTokenRepository.findByTokenAndRevokedFalse(refreshToken.trim()).ifPresent(token -> token.setRevoked(Boolean.TRUE));
        }
    }

    @Transactional(readOnly = true)
    public CurrentUser loadCurrentUser(Long userId) {
        AppUser user = appUserRepository.findByIdAndEnabledTrue(userId)
            .orElseThrow(() -> new UnauthorizedException("账号不存在或已停用"));
        List<UserRole> userRoles = userRoleRepository.findAllByUserId(userId);
        List<Role> roles = roleRepository.findAllById(userRoles.stream().map(UserRole::getRoleId).toList()).stream()
            .sorted(java.util.Comparator.comparing(Role::getId))
            .toList();
        Set<String> permissionCodes = queryPermissionCodes(userId);
        Set<Long> centerIds = user.getAdmin()
            ? Set.of()
            : userCenterPermissionRepository.findAllByUserId(userId).stream()
                .map(item -> item.getCenterId())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return new CurrentUser(
            user.getId(),
            user.getUsername(),
            user.getDisplayName(),
            Boolean.TRUE.equals(user.getAdmin()),
            roles.stream().map(Role::getCode).collect(Collectors.toCollection(LinkedHashSet::new)),
            roles.stream().map(Role::getName).collect(Collectors.toCollection(LinkedHashSet::new)),
            permissionCodes,
            centerIds
        );
    }

    private JwtTokenService.JwtTokenPair issueTokens(CurrentUser currentUser) {
        userRefreshTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        JwtTokenService.JwtTokenPair tokenPair = jwtTokenService.issueTokens(currentUser);
        userRefreshTokenRepository.save(UserRefreshToken.builder()
            .userId(currentUser.id())
            .token(tokenPair.refreshToken())
            .expiresAt(tokenPair.refreshTokenExpiresAt())
            .revoked(Boolean.FALSE)
            .build());
        return tokenPair;
    }

    private Map<String, Object> buildAuthPayload(CurrentUser currentUser, JwtTokenService.JwtTokenPair tokenPair) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accessToken", tokenPair.accessToken());
        payload.put("refreshToken", tokenPair.refreshToken());
        payload.put("accessTokenExpiresAt", tokenPair.accessTokenExpiresAt().toString());
        payload.put("refreshTokenExpiresAt", tokenPair.refreshTokenExpiresAt().toString());
        payload.put("user", toUserPayload(currentUser));
        return payload;
    }

    private Map<String, Object> toUserPayload(CurrentUser currentUser) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", currentUser.id());
        payload.put("username", currentUser.username());
        payload.put("displayName", currentUser.displayName());
        payload.put("admin", currentUser.admin());
        payload.put("roles", currentUser.roleNames());
        payload.put("roleCodes", currentUser.roleCodes());
        payload.put("permissions", currentUser.permissionCodes());
        payload.put("centerIds", currentUser.admin() ? List.of() : currentUser.centerIds());
        payload.put("centers", currentUser.admin() ? List.of() : resolveCenters(currentUser.centerIds()));
        return payload;
    }

    private List<Map<String, Object>> resolveCenters(Collection<Long> centerIds) {
        if (centerIds == null || centerIds.isEmpty()) {
            return List.of();
        }
        return technicalCenterRepository.findAllById(centerIds).stream()
            .sorted(java.util.Comparator.comparing(TechnicalCenter::getSortOrder).thenComparing(TechnicalCenter::getId))
            .map(center -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", center.getId());
                item.put("name", center.getName());
                item.put("unitId", center.getUnit().getId());
                item.put("unitName", center.getUnit().getName());
                return item;
            })
            .toList();
    }

    private Set<String> queryPermissionCodes(Long userId) {
        return new LinkedHashSet<>(jdbcTemplate.query(
            "SELECT DISTINCT p.code FROM sys_user_role ur JOIN sys_role_permission rp ON rp.role_id = ur.role_id JOIN sys_permission p ON p.id = rp.permission_id WHERE ur.user_id = ? ORDER BY p.code",
            (resultSet, rowNum) -> resultSet.getString(1),
            userId
        ));
    }

    private boolean passwordMatches(String rawPassword, String storedPassword) {
        if (!StringUtils.hasText(storedPassword)) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        return Objects.equals(rawPassword, storedPassword);
    }

    private void upgradeLegacyPasswordIfNeeded(AppUser user, String rawPassword) {
        if (user.getPasswordHash() != null && user.getPasswordHash().startsWith("$2")) {
            return;
        }
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        appUserRepository.save(user);
    }
}