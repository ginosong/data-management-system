package com.datamanagement.system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datamanagement.system.domain.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findAllByEnabledTrueOrderByIdAsc();

    Optional<Role> findByCode(String code);
}
