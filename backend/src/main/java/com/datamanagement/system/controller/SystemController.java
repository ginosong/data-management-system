package com.datamanagement.system.controller;

import java.util.Map;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.datamanagement.system.service.SystemService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Validated
public class SystemController {

    private final SystemService systemService;

    @GetMapping("/access-overview")
    public Map<String, Object> accessOverview() {
        return systemService.getAccessOverview();
    }

    @PostMapping("/users")
    public Map<String, Object> createUser(@Valid @RequestBody SaveUserRequest request) {
        return systemService.saveUser(
            null,
            request.username(),
            request.displayName(),
            request.password(),
            request.admin(),
            request.enabled(),
            request.roleIds(),
            request.centerIds()
        );
    }

    @PutMapping("/users/{userId}")
    public Map<String, Object> updateUser(@PathVariable Long userId, @Valid @RequestBody UpdateUserRequest request) {
        return systemService.saveUser(
            userId,
            request.username(),
            request.displayName(),
            null,
            request.admin(),
            request.enabled(),
            request.roleIds(),
            request.centerIds()
        );
    }

    @PostMapping("/users/{userId}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable Long userId, @Valid @RequestBody ResetPasswordRequest request) {
        return systemService.resetPassword(userId, request.password());
    }

    @DeleteMapping("/users/{userId}")
    public void disableUser(@PathVariable Long userId) {
        systemService.disableUser(userId);
    }

    @PostMapping("/roles")
    public Map<String, Object> createRole(@Valid @RequestBody SaveRoleRequest request) {
        return systemService.saveRole(null, request.code(), request.name(), request.enabled(), request.permissionCodes());
    }

    @PutMapping("/roles/{roleId}")
    public Map<String, Object> updateRole(@PathVariable Long roleId, @Valid @RequestBody SaveRoleRequest request) {
        return systemService.saveRole(roleId, request.code(), request.name(), request.enabled(), request.permissionCodes());
    }

    @DeleteMapping("/roles/{roleId}")
    public void disableRole(@PathVariable Long roleId) {
        systemService.disableRole(roleId);
    }

    public record SaveUserRequest(
        @NotBlank(message = "不能为空") String username,
        @NotBlank(message = "不能为空") String displayName,
        @NotBlank(message = "不能为空") String password,
        Boolean admin,
        Boolean enabled,
        java.util.List<Long> roleIds,
        java.util.List<Long> centerIds
    ) {
    }

    public record UpdateUserRequest(
        @NotBlank(message = "不能为空") String username,
        @NotBlank(message = "不能为空") String displayName,
        Boolean admin,
        Boolean enabled,
        java.util.List<Long> roleIds,
        java.util.List<Long> centerIds
    ) {
    }

    public record ResetPasswordRequest(@NotBlank(message = "不能为空") String password) {
    }

    public record SaveRoleRequest(
        @NotBlank(message = "不能为空") String code,
        @NotBlank(message = "不能为空") String name,
        Boolean enabled,
        java.util.List<String> permissionCodes
    ) {
    }
}
