package com.pat.crewhive.service;

import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.model.user.entity.role.Role;
import com.pat.crewhive.model.user.entity.role.UserRole;
import com.pat.crewhive.repository.RoleRepository;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserService userService;
    private final CompanyService companyService;

    public RoleService(RoleRepository roleRepository,
                       UserService userService,
                       CompanyService companyService) {
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.companyService = companyService;
    }

    /**
     * Creates a new role in the system.
     *
     * @param roleName the name of the role to be created
     * @param companyId the ID of the company to which the role belongs
     * @throws ResourceAlreadyExistsException if the role already exists for the given company
     */
    @Transactional
    public void createRole(String roleName, Long companyId) {

        String normalizedRole = normalizeRole(roleName);

        Company company = companyService.getCompanyById(companyId);

        if (roleRepository.existsByRoleNameIgnoreCaseAndCompany(normalizedRole, company)) {

            log.error("Role {} already exists", roleName);
            throw new ResourceAlreadyExistsException("Role already exists");
        }

        Role newRole = new Role(normalizedRole, company);

        roleRepository.save(newRole);
        log.info("Role {} created successfully", roleName);
    }

    /**
     * Updates the role of a user.
     *
     * @param targetId the ID of the user whose role is to be updated
     * @param newRole  the new role to be assigned to the user
     * @param companyId the ID of the company to which the user belongs
     * @throws ResourceNotFoundException if the role is not found
     */
    @Transactional
    public void updateUserRole(Long targetId, String newRole, Long companyId) {

        String normalizedRole = normalizeRole(newRole);

        Company company = companyService.getCompanyById(companyId);

        Role role = roleRepository.findByRoleNameIgnoreCaseAndCompany(normalizedRole, company)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        User targetUser = userService.getUserById(targetId);

        // Posso farlo perché al momento della registrazione viene dato un ruolo di default
        UserRole current = targetUser.getRole();
        current.setRole(role);
    }

    /**
     * Retrieves or creates a global role for users.
     *
     * @return the global role for users
     */
    @Transactional
    public Role getOrCreateGlobalRoleUser() {
        String name = "ROLE_USER";
        return roleRepository.findByRoleNameIgnoreCaseAndCompanyIsNull(name)
                .orElseGet(() -> roleRepository.save(new Role(name, null)));
    }

    private String normalizeRole(String raw) {

        String r = raw.trim().toUpperCase();
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }
}

