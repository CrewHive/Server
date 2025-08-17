package com.pat.crewhive.service;

import com.pat.crewhive.dto.CompanyRegistrationDTO;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.repository.CompanyRepository;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
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
    }

    @Transactional(readOnly = true)
    public Company getCompanyById(Long companyId) {

        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceAlreadyExistsException("Company with ID " + companyId + " does not exist."));
    }

}
