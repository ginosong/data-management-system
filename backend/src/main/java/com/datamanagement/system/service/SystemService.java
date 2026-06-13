package com.datamanagement.system.service;

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
import com.datamanagement.system.domain.UserCenterPermission;
import com.datamanagement.system.domain.UserRole;
import com.datamanagement.system.exception.ForbiddenException;
import com.datamanagement.system.repository.AppUserRepository;
import com.datamanagement.system.repository.RoleRepository;
import com.datamanagement.system.repository.TechnicalCenterRepository;
import com.datamanagement.system.repository.UserCenterPermissionRepository;
import com.datamanagement.system.repository.UserRoleRepository;
import com.datamanagement.system.security.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SystemService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserCenterPermissionRepository userCenterPermissionRepository;
    private final TechnicalCenterRepository technicalCenterRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public Map<String, Object> getAccessOverview() {
        currentUserService.requirePermission("system:user:view");
        List<AppUser> users = appUserRepository.findAll().stream()
            .sorted(java.util.Comparator.comparing(AppUser::getId))
            .toList();
        List<Role> roles = roleRepository.findAll().stream()
            .sorted(java.util.Comparator.comparing(Role::getId))
            .toList();
        List<Long> userIds = users.stream().map(AppUser::getId).filter(Objects::nonNull).toList();
        Map<Long, List<Long>> userRoleMap = userRoleRepository.findAllByUserIdIn(userIds).stream()
            .collect(Collectors.groupingBy(UserRole::getUserId, LinkedHashMap::new,
                Collectors.mapping(UserRole::getRoleId, Collectors.toList())));
        Map<Long, String> roleNameMap = roles.stream()
            .collect(Collectors.toMap(Role::getId, Role::getName, (left, right) -> left, LinkedHashMap::new));
        Map<Long, List<Long>> centerPermissionMap = userCenterPermissionRepository.findAllByUserIdIn(userIds).stream()
            .collect(Collectors.groupingBy(UserCenterPermission::getUserId, LinkedHashMap::new,
                Collectors.mapping(UserCenterPermission::getCenterId, Collectors.toList())));
        Map<Long, String> centerNameMap = technicalCenterRepository.findAllByEnabledTrueOrderBySortOrderAscIdAsc().stream()
            .collect(Collectors.toMap(TechnicalCenter::getId, TechnicalCenter::getName, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<String>> rolePermissions = queryRolePermissions();
        Map<String, List<String>> rolePermissionCodes = queryRolePermissionCodes();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("defaultAdmin", Map.of("username", "admin", "password", "admin123"));
        payload.put("dataRuleNote", "页面权限由角色控制，数据权限按技术中心授权；张三当前只可维护两个被授权中心。\n");
        payload.put("users", users.stream().map(user -> buildUserItem(user, userRoleMap, centerPermissionMap, roleNameMap, centerNameMap)).toList());
        payload.put("roles", roles.stream().map(role -> buildRoleItem(role, rolePermissions, rolePermissionCodes)).toList());
        payload.put("permissionCatalog", getPermissionCatalog());
        return payload;
    }

    @Transactional
    public Map<String, Object> saveUser(Long userId,
                                        String username,
                                        String displayName,
                                        String password,
                                        Boolean admin,
                                        Boolean enabled,
                                        List<Long> roleIds,
                                        List<Long> centerIds) {
        currentUserService.requirePermission("system:user:edit");
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("账号不能为空");
        }
        if (!StringUtils.hasText(displayName)) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        if (userId == null && !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("新建账号时必须设置初始密码");
        }

        String normalizedUsername = username.trim();
        appUserRepository.findByUsername(normalizedUsername).ifPresent(existing -> {
            if (userId == null || !existing.getId().equals(userId)) {
                throw new IllegalArgumentException("账号已存在");
            }
        });

        AppUser user = userId == null
            ? new AppUser()
            : appUserRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("账号不存在"));

        List<Role> roles = loadRoles(roleIds);
        List<TechnicalCenter> centers = loadCenters(centerIds);

        user.setUsername(normalizedUsername);
        user.setDisplayName(displayName.trim());
        user.setAdmin(Boolean.TRUE.equals(admin));
        user.setEnabled(enabled == null || enabled);
        if (userId == null) {
            user.setPasswordHash(passwordEncoder.encode(password.trim()));
        }

        AppUser savedUser = appUserRepository.save(user);
        replaceUserRoles(savedUser.getId(), roles);
        replaceUserCenters(savedUser.getId(), savedUser.getAdmin() ? List.of() : centers);
        return buildUserItem(savedUser);
    }

    @Transactional
    public Map<String, Object> resetPassword(Long userId, String password) {
        currentUserService.requirePermission("system:user:edit");
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("新密码不能为空");
        }
        AppUser user = appUserRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("账号不存在"));
        user.setPasswordHash(passwordEncoder.encode(password.trim()));
        return buildUserItem(appUserRepository.save(user));
    }

    @Transactional
    public Map<String, Object> saveRole(Long roleId, String code, String name, Boolean enabled, List<String> permissionCodes) {
        currentUserService.requirePermission("system:user:edit");
        if (!StringUtils.hasText(code) || !StringUtils.hasText(name)) {
            throw new IllegalArgumentException("角色编码和名称不能为空");
        }

        String normalizedCode = code.trim().toUpperCase();
        roleRepository.findByCode(normalizedCode).ifPresent(existing -> {
            if (roleId == null || !existing.getId().equals(roleId)) {
                throw new IllegalArgumentException("角色编码已存在");
            }
        });

        Role role = roleId == null
            ? new Role()
            : roleRepository.findById(roleId).orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        role.setCode(normalizedCode);
        role.setName(name.trim());
        role.setEnabled(enabled == null || enabled);
        Role savedRole = roleRepository.save(role);
        replaceRolePermissions(savedRole.getId(), permissionCodes == null ? List.of() : permissionCodes);
        return buildRoleItem(savedRole);
    }

    @Transactional
    public void disableUser(Long userId) {
        currentUserService.requirePermission("system:user:edit");
        AppUser user = appUserRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("账号不存在"));
        long activeAdminCount = appUserRepository.findAllByEnabledTrueOrderByIdAsc().stream()
            .filter(item -> Boolean.TRUE.equals(item.getAdmin()))
            .count();
        if (Boolean.TRUE.equals(user.getAdmin()) && activeAdminCount <= 1) {
            throw new ForbiddenException("至少保留一个启用中的管理员账号");
        }
        user.setEnabled(Boolean.FALSE);
    }

    @Transactional
    public void disableRole(Long roleId) {
        currentUserService.requirePermission("system:user:edit");
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        role.setEnabled(Boolean.FALSE);
    }

    private Map<String, List<String>> queryRolePermissions() {
        Map<String, List<String>> permissionMap = new LinkedHashMap<>();
        jdbcTemplate.query(
            "SELECT r.code AS role_code, p.name AS permission_name FROM sys_role_permission rp JOIN sys_role r ON r.id = rp.role_id JOIN sys_permission p ON p.id = rp.permission_id ORDER BY r.id, p.id",
            resultSet -> {
                while (resultSet.next()) {
                    permissionMap.computeIfAbsent(resultSet.getString("role_code"), ignored -> new java.util.ArrayList<>())
                        .add(resultSet.getString("permission_name"));
                }
                return null;
            }
        );
        return permissionMap;
    }

    private Map<String, List<String>> queryRolePermissionCodes() {
        Map<String, List<String>> permissionMap = new LinkedHashMap<>();
        jdbcTemplate.query(
            "SELECT r.code AS role_code, p.code AS permission_code FROM sys_role_permission rp JOIN sys_role r ON r.id = rp.role_id JOIN sys_permission p ON p.id = rp.permission_id ORDER BY r.id, p.id",
            resultSet -> {
                while (resultSet.next()) {
                    permissionMap.computeIfAbsent(resultSet.getString("role_code"), ignored -> new java.util.ArrayList<>())
                        .add(resultSet.getString("permission_code"));
                }
                return null;
            }
        );
        return permissionMap;
    }

    private List<Map<String, Object>> getPermissionCatalog() {
        return jdbcTemplate.query(
            "SELECT id, code, name, permission_type, route_path FROM sys_permission ORDER BY id",
            (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", rs.getLong("id"));
                row.put("code", rs.getString("code"));
                row.put("name", rs.getString("name"));
                row.put("permissionType", rs.getString("permission_type"));
                row.put("routePath", rs.getString("route_path"));
                return row;
            }
        );
    }

    private List<Role> loadRoles(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        Set<Long> uniqueRoleIds = new LinkedHashSet<>(roleIds);
        List<Role> roles = roleRepository.findAllById(uniqueRoleIds).stream()
            .filter(role -> Boolean.TRUE.equals(role.getEnabled()))
            .toList();
        if (roles.size() != uniqueRoleIds.size()) {
            throw new IllegalArgumentException("存在不可用的角色配置");
        }
        return roles;
    }

    private List<TechnicalCenter> loadCenters(List<Long> centerIds) {
        if (centerIds == null || centerIds.isEmpty()) {
            return List.of();
        }
        Set<Long> uniqueCenterIds = new LinkedHashSet<>(centerIds);
        List<TechnicalCenter> centers = technicalCenterRepository.findAllById(uniqueCenterIds).stream()
            .filter(center -> Boolean.TRUE.equals(center.getEnabled()))
            .toList();
        if (centers.size() != uniqueCenterIds.size()) {
            throw new IllegalArgumentException("存在不可用的技术中心配置");
        }
        return centers;
    }

    private void replaceUserRoles(Long userId, List<Role> roles) {
        userRoleRepository.deleteByUserId(userId);
        userRoleRepository.flush();
        if (roles.isEmpty()) {
            return;
        }
        userRoleRepository.saveAll(roles.stream()
            .map(role -> UserRole.builder().userId(userId).roleId(role.getId()).build())
            .toList());
    }

    private void replaceUserCenters(Long userId, List<TechnicalCenter> centers) {
        userCenterPermissionRepository.deleteByUserId(userId);
        userCenterPermissionRepository.flush();
        if (centers.isEmpty()) {
            return;
        }
        userCenterPermissionRepository.saveAll(centers.stream()
            .map(center -> UserCenterPermission.builder().userId(userId).centerId(center.getId()).build())
            .toList());
    }

    private void replaceRolePermissions(Long roleId, List<String> permissionCodes) {
        jdbcTemplate.update("DELETE FROM sys_role_permission WHERE role_id = ?", roleId);
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return;
        }
        Set<String> allowedCodes = getPermissionCatalog().stream()
            .map(item -> String.valueOf(item.get("code")))
            .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> normalizedCodes = permissionCodes.stream()
            .filter(StringUtils::hasText)
            .map(code -> code.trim())
            .distinct()
            .toList();
        if (!allowedCodes.containsAll(normalizedCodes)) {
            throw new IllegalArgumentException("存在无效的权限编码");
        }
        normalizedCodes.forEach(code -> jdbcTemplate.update(
            "INSERT INTO sys_role_permission (role_id, permission_id) SELECT ?, id FROM sys_permission WHERE code = ?",
            roleId,
            code
        ));
    }

    private Map<String, Object> buildUserItem(AppUser user) {
        Map<Long, List<Long>> userRoleMap = userRoleRepository.findAllByUserIdIn(List.of(user.getId())).stream()
            .collect(Collectors.groupingBy(UserRole::getUserId, LinkedHashMap::new, Collectors.mapping(UserRole::getRoleId, Collectors.toList())));
        Map<Long, List<Long>> centerPermissionMap = userCenterPermissionRepository.findAllByUserIdIn(List.of(user.getId())).stream()
            .collect(Collectors.groupingBy(UserCenterPermission::getUserId, LinkedHashMap::new, Collectors.mapping(UserCenterPermission::getCenterId, Collectors.toList())));
        Map<Long, String> roleNameMap = roleRepository.findAll().stream()
            .collect(Collectors.toMap(Role::getId, Role::getName, (left, right) -> left, LinkedHashMap::new));
        Map<Long, String> centerNameMap = technicalCenterRepository.findAllByEnabledTrueOrderBySortOrderAscIdAsc().stream()
            .collect(Collectors.toMap(TechnicalCenter::getId, TechnicalCenter::getName, (left, right) -> left, LinkedHashMap::new));
        return buildUserItem(user, userRoleMap, centerPermissionMap, roleNameMap, centerNameMap);
    }

    private Map<String, Object> buildUserItem(AppUser user,
                                              Map<Long, List<Long>> userRoleMap,
                                              Map<Long, List<Long>> centerPermissionMap,
                                              Map<Long, String> roleNameMap,
                                              Map<Long, String> centerNameMap) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", user.getId());
        item.put("username", user.getUsername());
        item.put("displayName", user.getDisplayName());
        item.put("admin", user.getAdmin());
        item.put("enabled", user.getEnabled());
        item.put("roleIds", userRoleMap.getOrDefault(user.getId(), List.of()));
        item.put("roles", resolveNames(userRoleMap.get(user.getId()), roleNameMap));
        item.put("centerIds", centerPermissionMap.getOrDefault(user.getId(), List.of()));
        item.put("centers", resolveNames(centerPermissionMap.get(user.getId()), centerNameMap));
        return item;
    }

    private Map<String, Object> buildRoleItem(Role role) {
        return buildRoleItem(role, queryRolePermissions(), queryRolePermissionCodes());
    }

    private Map<String, Object> buildRoleItem(Role role,
                                              Map<String, List<String>> rolePermissions,
                                              Map<String, List<String>> rolePermissionCodes) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", role.getId());
        item.put("code", role.getCode());
        item.put("name", role.getName());
        item.put("enabled", role.getEnabled());
        item.put("permissions", rolePermissions.getOrDefault(role.getCode(), List.of()));
        item.put("permissionCodes", rolePermissionCodes.getOrDefault(role.getCode(), List.of()));
        return item;
    }

    private List<String> resolveNames(Collection<Long> ids, Map<Long, String> nameMap) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().map(nameMap::get).filter(Objects::nonNull).toList();
    }
}
