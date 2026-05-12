package com.datamanagement.system.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datamanagement.system.domain.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findAllByUserId(Long userId);

    List<UserRole> findAllByUserIdIn(Collection<Long> userIds);

    void deleteByUserId(Long userId);
}
