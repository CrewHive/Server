package com.pat.crewhive.api.controller;

import com.pat.crewhive.api.controller.interfaces.CompanyControllerInterface;
import com.pat.crewhive.dto.Company.CompanyRegistrationDTO;
import com.pat.crewhive.dto.Company.SetCompanyDTO;
import com.pat.crewhive.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/company")
public class CompanyController implements CompanyControllerInterface {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<?> registerCompany(@Valid @RequestBody CompanyRegistrationDTO request) {

        companyService.registerCompany(request);

        log.info("Company {} registered successfully", request.getCompanyName());

        return ResponseEntity.ok().build();
    }

    @Override
    @PutMapping("/set-company")
    public ResponseEntity<?> setCompany(@Valid @RequestBody SetCompanyDTO request) {

        companyService.setCompany(request);

        log.info("Company set for user ID: {}", request.getUserId());

        return ResponseEntity.ok().build();
    }
}
