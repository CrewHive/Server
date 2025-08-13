package com.pat.crewhive.controller;

import com.pat.crewhive.dto.CompanyRegistrationDTO;
import com.pat.crewhive.service.CompanyService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/company")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerCompany(@Valid @RequestBody CompanyRegistrationDTO request) {

        companyService.registerCompany(request);

        log.info("Company {} registered successfully", request.getCompanyName());

        return ResponseEntity.ok().build();
    }
}
