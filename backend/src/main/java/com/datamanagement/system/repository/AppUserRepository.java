package com.datamanagement.system.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.datamanagement.system.domain.AppUser;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    long countByEnabledTrue();

    List<AppUser> findAllByEnabledTrueOrderByIdAsc();

    Optional<AppUser> findByIdAndEnabledTrue(Long id);

    Optional<AppUser> findByUsername(String username);
}
