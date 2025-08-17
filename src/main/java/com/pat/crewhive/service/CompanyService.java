package com.pat.crewhive.service;

import com.pat.crewhive.dto.Company.CompanyRegistrationDTO;
import com.pat.crewhive.dto.Company.SetCompanyDTO;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.CompanyRepository;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    private final UserService userService;

    public CompanyService(CompanyRepository companyRepository,
                          UserService userService) {
        this.companyRepository = companyRepository;
        this.userService = userService;
    }

    /**
     * Registers a new company.
     *
     * @param request The company registration request containing company details.
     */
    @Transactional
    public void registerCompany(CompanyRegistrationDTO request) {

        if(companyRepository.existsByName(request.getCompanyName())) throw new ResourceAlreadyExistsException("Company with name " + request.getCompanyName() + " already exists.");

        Company company = new Company(request);
        companyRepository.save(company);
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
     */
    @Transactional
    public void setCompany(SetCompanyDTO request) {

        Company company = companyRepository.findByName((request.getCompanyName()))
                .orElseThrow(() ->new ResourceAlreadyExistsException("Company with name " + request.getCompanyName() + " does not exist."));

        User user = userService.getUserById(request.getUserId());

        user.setCompany(company);

        userService.updateUser(user);
        log.info("Company {} set for user ID: {}", request.getCompanyName(), request.getUserId());
    }
}
