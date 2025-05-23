package com.pat.crewhive.repository;

import com.pat.crewhive.model.user.entity.role.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleName(String name);

}
