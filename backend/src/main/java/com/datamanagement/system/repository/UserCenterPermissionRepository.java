package com.datamanagement.system.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datamanagement.system.domain.UserCenterPermission;

public interface UserCenterPermissionRepository extends JpaRepository<UserCenterPermission, Long> {

    List<UserCenterPermission> findAllByUserId(Long userId);

    List<UserCenterPermission> findAllByUserIdIn(Collection<Long> userIds);

    void deleteByUserId(Long userId);
}
