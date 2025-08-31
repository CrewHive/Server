package com.pat.crewhive.service;

import com.pat.crewhive.dto.company.CompanyRegistrationDTO;
import com.pat.crewhive.dto.company.RemoveUserFromCompanyOutputDTO;
import com.pat.crewhive.dto.company.SetCompanyDTO;
import com.pat.crewhive.dto.company.UserIdAndUsernameAndHoursDTO;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.role.entity.Role;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.CompanyRepository;
import com.pat.crewhive.repository.RoleRepository;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.service.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;
    private final StringUtils stringUtils;

    public CompanyService(CompanyRepository companyRepository,
                          UserService userService,
                          StringUtils stringUtils,
                          RoleRepository roleRepository,
                          JwtService jwtService) {
        this.companyRepository = companyRepository;
        this.userService = userService;
        this.stringUtils = stringUtils;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new company and associates it with the manager.
     * @param managerId The ID of the manager registering the company.
     * @param request The company registration request containing company details.
     */
    @Transactional
    public void registerCompany(Long managerId, CompanyRegistrationDTO request) {

        log.error("Attempting to register company with name: {}", request.getCompanyName());

        String normalizedCompanyName = stringUtils.normalizeString(request.getCompanyName());

        if(companyRepository.existsByName(normalizedCompanyName)) {

            log.error("Company with name {} already exists", request.getCompanyName());

            throw new ResourceAlreadyExistsException("Company with name " + request.getCompanyName() + " already exists.");
        }

        Company company = new Company(request);
        company.setName(normalizedCompanyName);
        companyRepository.save(company);

        User manager = userService.getUserById(managerId);
        manager.setCompany(company);

        // Duplicate of getOrCreateGlobalRoleManager but here to avoid circular dependency
        String name = "ROLE_MANAGER";
        Role role = roleRepository.findByRoleNameIgnoreCaseAndCompanyIsNull(name)
                .orElseGet(() -> roleRepository.save(new Role(name, null)));
        manager.getRole().setRole(role);

        userService.updateUser(manager);

        log.info("Company {} registered successfully", request.getCompanyName());
    }

    /**
     * Retrieves a company by its ID.
     *
     * @param companyId The ID of the company to retrieve.
     * @return The Company object if found.
     * @throws ResourceAlreadyExistsException if the company does not exist.
     */
    @Transactional(readOnly = true)
    public Company getCompanyById(Long companyId) {

        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceAlreadyExistsException("Company with ID " + companyId + " does not exist."));
    }


    /**
     * Retrieves all users associated with a specific company.
     *
     * @param managerId The ID of the manager requesting the user list.
     * @param companyId The ID of the company whose users are to be retrieved.
     * @return A list of UserIdAndUsernameDTO representing users in the company.
     * @throws AuthorizationDeniedException if the manager does not belong to the specified company.
     */
    @Transactional(readOnly = true)
    public List<UserIdAndUsernameAndHoursDTO> getAllUsersInCompany(Long managerId, Long companyId) {

        log.info("Retrieving all users in company with ID: {}", companyId);

        if(!isPartOfCompany(managerId, getCompanyById(companyId).getName())) {

            log.error("Manager {} may be not part of company {}", managerId, companyId);
            throw new AuthorizationDeniedException("Manager does not belong to the specified company.");
        }

        var users = userService.getAllUsersInCompany(companyId);

        return users.stream()
                .map(user -> new UserIdAndUsernameAndHoursDTO(user.getUserId(), user.getUsername(), user.getWorkableHoursPerWeek()))
                .toList();
    }


    /**
     * Checks if a user is part of a specific company.
     *
     * @param userId The ID of the user to check.
     * @param companyName The name of the company to check against.
     * @return true if the user is part of the company, false otherwise.
     */
    @Transactional(readOnly = true)
    public boolean isPartOfCompany(Long userId, String companyName) {

        User user = userService.getUserById(userId);
        return user.getCompany() != null && user.getCompany().getName().equals(companyName);
    }

    /**
     * Sets a company for a user.
     *
     * @param request The request containing user ID and company name.
     * @throws ResourceAlreadyExistsException if the company does not exist.
     */
    @Transactional
    public void setCompany(SetCompanyDTO request, Long managerId) {

        if(!isPartOfCompany(managerId, request.getCompanyName())) {
            throw new AuthorizationDeniedException("Manager does not belong to the specified company.");
        }

        String normalizedCompanyName = stringUtils.normalizeString(request.getCompanyName());

        Company company = companyRepository.findByName(normalizedCompanyName)
                .orElseThrow(() ->new ResourceAlreadyExistsException("Company with name " + request.getCompanyName() + " does not exist."));

        User user = userService.getUserById(request.getUserId());

        if (user.getCompany() != null) {
            log.info("User ID: {} is already part of company: {}", request.getUserId(), request.getCompanyName());
            return; // User is already part of the specified company
        }

        user.setCompany(company);
        userService.updateUser(user);
        log.info("Company {} set for user ID: {}", request.getCompanyName(), request.getUserId());
    }


    /**
     * Deletes a company by its ID after removing its association from all users.
     *
     * @param companyId The ID of the company to delete.
     * @param managerId The ID of the manager requesting the deletion.
     * @throws AuthorizationDeniedException if the manager does not belong to the specified company.
     */
    @Transactional
    public void deleteCompany(Long companyId, Long managerId) {

        if(!isPartOfCompany(managerId, getCompanyById(companyId).getName())) {

            log.error("Manager {} may be not part of company {}", managerId, companyId);
            throw new AuthorizationDeniedException("Manager does not belong to the specified company.");
        }

        removeCompanyFromUsers(companyId);

        companyRepository.deleteById(companyId);

        log.info("Company with ID {} deleted successfully", companyId);
    }


    /**
     * Removes a user from a company.
     *
     * @param userId The ID of the user to remove from the company.
     * @param managerId The ID of the manager requesting the removal.
     * @param companyId The ID of the company from which to remove the user.
     * @throws AuthorizationDeniedException if the manager does not belong to the specified company.
     * @throws ResourceNotFoundException if the user does not belong to the specified company or doesn't have one.
     */
    @Transactional
    public RemoveUserFromCompanyOutputDTO removeUserFromCompany(Long userId, Long managerId, Long companyId) {

        log.info("Attempting to remove user ID {} from company ID {}", userId, companyId);

        if(!isPartOfCompany(managerId, getCompanyById(companyId).getName())) {

            log.error("Manager {} may be not part of company {}", managerId, companyId);
            throw new AuthorizationDeniedException("Manager does not belong to the specified company.");
        }

        User user = userService.getUserById(userId);

        if(user.getCompany() == null || !user.getCompany().getCompanyId().equals(companyId)) {

            log.error("Company with ID {} does not belong to the specified company.", companyId);
            throw new ResourceNotFoundException("User does not belong to the specified company or doesn't have one.");
        }

        user.setCompany(null);
        userService.updateUser(user);

        log.info("User with ID {} removed from company ID {}", userId, companyId);

        String accessToken = jwtService
                .generateToken(user.getUserId(),
                        user.getUsername(),
                        user.getRole().getRole().getRoleName(),
                        null);

        return new RemoveUserFromCompanyOutputDTO(accessToken);
    }


    /**
     * Removes the association of a company from all users linked to it.
     *
     * @param companyId The ID of the company to remove from users.
     */
    @Transactional
    public void removeCompanyFromUsers(Long companyId) {

        var users = userService.getAllUsersInCompany(companyId);

        for (User user : users) {
            user.setCompany(null);
            userService.updateUser(user);
        }

        log.info("Removed company {} from all associated users", companyId);
    }
}
