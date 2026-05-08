package com.ga.leave.repository;

import com.ga.leave.model.Role;
import com.ga.leave.model.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Long> {
    boolean existsByName(RoleName name);
    Optional<Role> findByName(RoleName name);
}
