package com.pat.crewhive.company;

import com.pat.crewhive.authuser.AuthResponseDTO;
import com.pat.crewhive.user.UserWithTimeParamsDTO;
import com.pat.crewhive.manager.Role;
import com.pat.crewhive.user.User;
import com.pat.crewhive.manager.RoleRepository;
import com.pat.crewhive.user.UserService;
import com.pat.crewhive.security.JwtService;
import com.pat.crewhive.authuser.RefreshTokenService;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.common.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserService userService;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;
    private final StringUtils stringUtils;
    private final RefreshTokenService refreshTokenService;
    private final CompanyAccessService companyAccessService;

    public CompanyService(CompanyRepository companyRepository,
                          UserService userService,
                          StringUtils stringUtils,
                          RoleRepository roleRepository,
                          JwtService jwtService,
                          RefreshTokenService refreshTokenService,
                          CompanyAccessService companyAccessService) {
        this.companyRepository = companyRepository;
        this.userService = userService;
        this.stringUtils = stringUtils;
        this.roleRepository = roleRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.companyAccessService = companyAccessService;
    }

    /**
     * Registers a new company and associates it with the manager.
     * @param managerId The ID of the manager registering the company.
     * @param request The company registration request containing company details.
     */
    @Transactional
    public AuthResponseDTO registerCompany(UUID managerId, CompanyRegistrationDTO request) {

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

        return new AuthResponseDTO(
                jwtService.generateToken(manager.getUserId(), stringUtils.normalizeString(manager.getEmail()), manager.getFirstName(), manager.getLastName(), manager.getRole().getRole().getRoleName(), company.getCompanyId()),
                refreshTokenService.getOrIssueRefreshToken(manager));
    }

    /**
     * Retrieves a company by its ID.
     *
     * @param companyId The ID of the company to retrieve.
     * @return The Company object if found.
     * @throws ResourceAlreadyExistsException if the company does not exist.
     */
    public Company getCompanyById(UUID companyId) {
        return companyAccessService.getCompanyById(companyId);
    }


    /**
     * Retrieves a company by the userId
     * @param requestedUserId the ID whom we want to know the company
     * @return the user's company
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "companyByUserId", key = "#requestedUserId")
    public Company getCompanyByUserId(UUID requestedUserId) {

        return userService.getUserById(requestedUserId).getCompany();
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
    @Cacheable(value = "usersInCompany", key = "#companyId")
    public List<UserIdAndNameAndHoursDTO> getAllUsersInCompany(UUID managerId, UUID companyId) {

        log.info("Retrieving all users in company with ID: {}", companyId);

        if(companyAccessService.isNotPartOfCompany(managerId, companyAccessService.getCompanyById(companyId).getCompanyId())) {

            log.error("getAllUsersInCompany: Manager {} may be not part of company {}", managerId, companyId);
            throw new AuthorizationDeniedException("Manager does not belong to the specified company.");
        }

        List<User> users = userService.getAllUsersInCompany(companyId);

        return users.stream()
                .map(user -> new UserIdAndNameAndHoursDTO(user.getUserId(), user.getFirstName(), user.getLastName(), user.getWorkableHoursPerWeek()))
                .toList();
    }


    /**
     * Retrieves detailed information about a specific user within a company.
     *
     * @param managerId The ID of the manager requesting the user information.
     * @param companyId The ID of the company to which the user belongs.
     * @param targetId The ID of the user whose information is to be retrieved.
     * @return A UserWithTimeParamsDTO containing detailed information about the user.
     * @throws AuthorizationDeniedException if the manager does not belong to the specified company.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "userInCompany", key = "#companyId + ':' + #targetId")
    public UserWithTimeParamsDTO getCompanyUserWithInformation(UUID managerId, UUID companyId, UUID targetId) {

        if(companyAccessService.isNotPartOfCompany(managerId, companyAccessService.getCompanyById(companyId).getCompanyId())) {

            log.error("getCompanyUserWithInformation: Manager {} may be not part of company {}", managerId, companyId);
            throw new AuthorizationDeniedException("Manager does not belong to the specified company.");
        }

        User user = userService.getUserById(targetId);

        log.info("getCompanyUserWithInformation: User details retrieved for user: {}", user.getEmail());

        String companyName = (user.getCompany() != null) ? user.getCompany().getName() : null;

        return new UserWithTimeParamsDTO(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                companyName,
                user.getContractType(),
                user.getWorkableHoursPerWeek(),
                user.getOvertimeHours(),
                user.getVacationDaysAccumulated(),
                user.getVacationDaysTaken(),
                user.getLeaveDaysAccumulated(),
                user.getLeaveDaysTaken()
        );
    }


    /**
     * Sets a company for a user.
     *
     * @param request The request containing user ID and company name.
     * @param companyId The ID of the company
     * @param managerId The ID of the manager to check if he's part of the company.
     * @throws ResourceAlreadyExistsException if the company does not exist.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "companyById", key = "#companyId"),
            @CacheEvict(value = "usersInCompany", key = "#companyId"),
            @CacheEvict(value = "companyByUserId", key = "#request.userId")
    })
    public void setCompany(SetCompanyDTO request, UUID companyId, UUID managerId) {

        if(companyAccessService.isNotPartOfCompany(managerId, companyId)) {
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
    @Caching(evict = {
            @CacheEvict(value = "companyById", key = "#companyId"),
            @CacheEvict(value = "usersInCompany", key = "#companyId"),
            @CacheEvict(value = "companyByUserId", allEntries = true)
    })
    public void deleteCompany(UUID companyId, UUID managerId) {

        if(companyAccessService.isNotPartOfCompany(managerId, companyAccessService.getCompanyById(companyId).getCompanyId())) {

            log.error("deleteCompany: Manager {} may be not part of company {}", managerId, companyId);
            throw new AuthorizationDeniedException("Manager does not belong to the specified company.");
        }

        companyAccessService.removeCompanyFromUsers(companyId);

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
    @Caching(evict = {
            @CacheEvict(value = "userInCompany", key = "#companyId + ':' + #userId"),
            @CacheEvict(value = "usersInCompany", key = "#companyId"),
            @CacheEvict(value = "companyByUserId", key = "#userId")
    })
    public void removeUserFromCompany(UUID userId, UUID managerId, UUID companyId) {

        if (userId.equals(managerId)) {
            throw new AuthorizationDeniedException("Managers cannot remove themselves from the company.");
        }

        if (companyAccessService.isNotPartOfCompany(managerId, companyAccessService.getCompanyById(companyId).getCompanyId())) {
            throw new AuthorizationDeniedException("Not allowed");
        }

        User target = userService.getUserById(userId);

        if (target.getCompany() == null || !target.getCompany().getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException("User not in this company");
        }

        userService.leaveCompany(userId);
    }
}
