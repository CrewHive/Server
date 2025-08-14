package com.pat.crewhive.service;

import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.model.user.entity.role.Role;
import com.pat.crewhive.model.user.entity.role.UserRole;
import com.pat.crewhive.repository.RoleRepository;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserService userService;

    public RoleService(RoleRepository roleRepository,
                       UserService userService) {
        this.roleRepository = roleRepository;
        this.userService = userService;
    }

    /**
     * Creates a new role in the system.
     *
     * @param roleName the name of the role to be created
     */
    @Transactional
    public void createRole(String roleName) {

        String normalizedRole = normalizeRole(roleName);

        if (roleRepository.existsByRoleName(normalizedRole)) {

            log.error("Role {} already exists", roleName);
            throw new IllegalArgumentException("Role already exists");
        }

        roleRepository.save(new Role(normalizedRole));
        log.info("Role {} created successfully", roleName);
    }

    /**
     * Updates the role of a user.
     *
     * @param targetId the ID of the user whose role is to be updated
     * @param newRole  the new role to be assigned to the user
     */
    @Transactional
    public void updateUserRole(Long targetId, String newRole) {

        String normalizedRole = normalizeRole(newRole);

        Role role = roleRepository.findByRoleName(normalizedRole)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        User targetUser = userService.getUserById(targetId);
        if (targetUser == null) {

            log.error("User not found with id: {}", targetId);
            throw new ResourceNotFoundException("User not found");
        }

        UserRole current = targetUser.getRole();
        if (current == null) {

            current = new UserRole(targetUser, role);
            targetUser.setRole(current);

        } else {

            current.setRole(role);
        }
    }

    private String normalizeRole(String raw) {

        String r = raw.trim().toUpperCase();
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }
}

