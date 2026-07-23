package com.pat.crewhive.company;

import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Cached/transactional operations that {@link CompanyService} needs to call on itself.
 * Extracted into a separate bean so those calls go through the Spring proxy
 * (caching and transactions don't apply to same-class method calls) instead of
 * relying on self-injection.
 */
@Slf4j
@Service
public class CompanyAccessService {

    private final CompanyRepository companyRepository;
    private final UserService userService;

    public CompanyAccessService(CompanyRepository companyRepository, UserService userService) {
        this.companyRepository = companyRepository;
        this.userService = userService;
    }

    /**
     * Retrieves a company by its ID.
     *
     * @param companyId The ID of the company to retrieve.
     * @return The Company object if found.
     * @throws ResourceAlreadyExistsException if the company does not exist.
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "companyById", key = "#companyId")
    public Company getCompanyById(UUID companyId) {

        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceAlreadyExistsException("Company with ID " + companyId + " does not exist."));
    }

    /**
     * Checks if a user is part of a specific company.
     *
     * @param userId The ID of the user to check.
     * @param companyId The id of the company to check.
     * @return True if the user is part of the company, false otherwise.
     */
    @Transactional(readOnly = true)
    public boolean isNotPartOfCompany(UUID userId, UUID companyId) {
        User user = userService.getUserById(userId);
        return user.getCompany() == null ||
                !user.getCompany().getCompanyId().equals(companyId);
    }

    /**
     * Removes the association of a company from all users linked to it.
     *
     * @param companyId The ID of the company to remove from users.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "usersInCompany", key = "#companyId"),
            @CacheEvict(value = "companyByUserId", allEntries = true)
    })
    public void removeCompanyFromUsers(UUID companyId) {

        var users = userService.getAllUsersInCompany(companyId);

        for (User user : users) {
            user.setCompany(null);
            userService.updateUser(user);
        }

        log.info("Removed company {} from all associated users", companyId);
    }
}
