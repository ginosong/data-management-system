package com.datamanagement.system.security;

import java.util.Set;

public record CurrentUser(
    Long id,
    String username,
    String displayName,
    boolean admin,
    Set<String> roleCodes,
    Set<String> roleNames,
    Set<String> permissionCodes,
    Set<Long> centerIds
) {

    public boolean hasPermission(String permissionCode) {
        return admin || permissionCodes.contains(permissionCode);
    }

    public boolean canAccessCenter(Long centerId) {
        return admin || centerId == null || centerIds.contains(centerId);
    }
}