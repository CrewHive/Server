package com.pat.crewhive.controller;

import com.pat.crewhive.dto.Company.CompanyRegistrationDTO;
import com.pat.crewhive.dto.Company.SetCompanyDTO;
import com.pat.crewhive.service.CompanyService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/set-company")
    public ResponseEntity<?> setCompany(@Valid @RequestBody SetCompanyDTO request) {

        companyService.setCompany(request);

        log.info("Company set for user ID: {}", request.getUserId());

        return ResponseEntity.ok().build();
    }
}
