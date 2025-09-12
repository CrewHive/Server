package com.pat.crewhive.service;

import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.model.role.entity.Role;
import com.pat.crewhive.model.role.UserRole;
import com.pat.crewhive.repository.RoleRepository;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.service.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserService userService;
    private final CompanyService companyService;
    private final StringUtils stringUtils;

    public RoleService(RoleRepository roleRepository,
                       UserService userService,
                       CompanyService companyService,
                       StringUtils stringUtils) {
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.companyService = companyService;
        this.stringUtils = stringUtils;
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

        String normalizedRole = stringUtils.normalizeRole(roleName);

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

        String normalizedRole = stringUtils.normalizeRole(newRole);

        Company company = companyService.getCompanyById(companyId);

        Role role = roleRepository.findByRoleNameIgnoreCaseAndCompany(normalizedRole, company)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        User targetUser = userService.getUserById(targetId);

        // Posso farlo perché al momento della registrazione viene dato un ruolo di default
        UserRole current = targetUser.getRole();
        current.setRole(role);
    }


    /**
     * Deletes a role from the system.
     *
     * @param roleName the name of the role to be deleted
     * @param companyId the ID of the company to which the role belongs
     * @throws ResourceNotFoundException if the role/company is not found
     * @throws IllegalStateException if the role is assigned to users
     */
    @Transactional
    public void deleteRole(String roleName, Long companyId) {

        String normalizedRole = stringUtils.normalizeRole(roleName);

        Company company = companyService.getCompanyById(companyId);

        Role role = roleRepository.findByRoleNameIgnoreCaseAndCompany(normalizedRole, company)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (role.getUsers() != null && !role.getUsers().isEmpty()) {

            log.error("Cannot delete role {} because it is assigned to users", roleName);
            throw new IllegalStateException("Cannot delete role because it is assigned to users");
        }

        roleRepository.delete(role);
        log.info("Role {} deleted successfully", roleName);
    }


    /**
     * Retrieves or creates a global role for users.
     *
     * @return the global role for users
     */
    @Transactional
    public Role getOrCreateGlobalRoleUser() {

        //todo Ritorna un DTO
        String name = "ROLE_USER";
        return roleRepository.findByRoleNameIgnoreCaseAndCompanyIsNull(name)
                .orElseGet(() -> roleRepository.save(new Role(name, null)));
    }

    /**
     * Retrieves or creates a global role for managers.
     *
     * @return the global role for managers
     */
    @Transactional
    public Role getOrCreateGlobalRoleManager() {

        //todo Ritorna un DTO
        String name = "ROLE_MANAGER";
        return roleRepository.findByRoleNameIgnoreCaseAndCompanyIsNull(name)
                .orElseGet(() -> roleRepository.save(new Role(name, null)));
    }
}

