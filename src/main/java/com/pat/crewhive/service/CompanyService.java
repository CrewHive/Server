package com.pat.crewhive.service;

import com.pat.crewhive.dto.Company.CompanyRegistrationDTO;
import com.pat.crewhive.dto.Company.SetCompanyDTO;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.CompanyRepository;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.service.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserService userService;
    private final StringUtils stringUtils;

    public CompanyService(CompanyRepository companyRepository,
                          UserService userService,
                          StringUtils stringUtils) {
        this.companyRepository = companyRepository;
        this.userService = userService;
        this.stringUtils = stringUtils;
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

        SetCompanyDTO setCompanyDTO = new SetCompanyDTO(company.getName(), managerId);
        setCompany(setCompanyDTO);

        User manager = userService.getUserById(managerId);
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
     * Sets a company for a user.
     *
     * @param request The request containing user ID and company name.
     * @throws ResourceAlreadyExistsException if the company does not exist.
     */
    @Transactional
    public void setCompany(SetCompanyDTO request) {

        String normalizedCompanyName = stringUtils.normalizeString(request.getCompanyName());

        Company company = companyRepository.findByName(normalizedCompanyName)
                .orElseThrow(() ->new ResourceAlreadyExistsException("Company with name " + request.getCompanyName() + " does not exist."));

        User user = userService.getUserById(request.getUserId());

        user.setCompany(company);

        userService.updateUser(user);
        log.info("Company {} set for user ID: {}", request.getCompanyName(), request.getUserId());
    }
}
