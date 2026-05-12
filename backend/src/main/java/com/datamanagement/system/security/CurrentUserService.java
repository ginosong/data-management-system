package com.datamanagement.system.security;

import java.util.Arrays;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.datamanagement.system.exception.ForbiddenException;
import com.datamanagement.system.exception.UnauthorizedException;

@Service
public class CurrentUserService {

    public CurrentUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new UnauthorizedException("请先登录");
        }
        return currentUser;
    }

    public void requirePermission(String permissionCode) {
        CurrentUser currentUser = requireCurrentUser();
        if (!currentUser.hasPermission(permissionCode)) {
            throw new ForbiddenException("当前账号无权限执行该操作");
        }
    }

    public void requireAnyPermission(String... permissionCodes) {
        CurrentUser currentUser = requireCurrentUser();
        if (currentUser.admin()) {
            return;
        }
        boolean matched = Arrays.stream(permissionCodes).anyMatch(currentUser.permissionCodes()::contains);
        if (!matched) {
            throw new ForbiddenException("当前账号无权限执行该操作");
        }
    }

    public void requireCenterAccess(Long centerId) {
        CurrentUser currentUser = requireCurrentUser();
        if (!currentUser.canAccessCenter(centerId)) {
            throw new ForbiddenException("当前账号没有该技术中心的数据权限");
        }
    }
}