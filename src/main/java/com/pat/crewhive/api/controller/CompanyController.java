package com.pat.crewhive.api.controller;

import com.pat.crewhive.api.swagger.interfaces.CompanyControllerInterface;
import com.pat.crewhive.dto.Company.CompanyRegistrationDTO;
import com.pat.crewhive.dto.Company.SetCompanyDTO;
import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
import com.pat.crewhive.service.CompanyService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @PostMapping("/register")
    public ResponseEntity<?> registerCompany(@AuthenticationPrincipal CustomUserDetails cud,
                                             @Valid @RequestBody CompanyRegistrationDTO request) {

        Long managerId = cud.getUserId();

        companyService.registerCompany(managerId, request);

        log.info("Company {} registered successfully", request.getCompanyName());

        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @PutMapping("/set-company")
    public ResponseEntity<?> setCompany(@Valid @RequestBody SetCompanyDTO request) {

        companyService.setCompany(request);

        log.info("Company set for user ID: {}", request.getUserId());

        return ResponseEntity.ok().build();
    }
}
