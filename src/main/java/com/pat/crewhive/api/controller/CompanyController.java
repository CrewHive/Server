package com.pat.crewhive.api.controller;

import com.pat.crewhive.api.swagger.interfaces.CompanyControllerInterface;
import com.pat.crewhive.dto.response.auth.AuthResponseDTO;
import com.pat.crewhive.dto.request.company.CompanyRegistrationDTO;
import com.pat.crewhive.dto.request.company.SetCompanyDTO;
import com.pat.crewhive.dto.response.company.UserIdAndNameAndHoursDTO;
import com.pat.crewhive.dto.response.user.UserWithTimeParamsDTO;
import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
import com.pat.crewhive.service.CompanyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @GetMapping(path = "/{companyId}/users", produces = "application/json")
    public ResponseEntity<List<UserIdAndNameAndHoursDTO>> getCompanyUsers(@AuthenticationPrincipal CustomUserDetails cud,
                                                                          @PathVariable @NotNull Long companyId) {

        Long managerId = cud.getUserId();

        List<UserIdAndNameAndHoursDTO> users = companyService.getAllUsersInCompany(managerId, companyId);

        log.info("Fetched {} users for company ID: {}", users.size(), companyId);

        return ResponseEntity.ok(users);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @GetMapping(path = "/{companyId}/user/{targetId}/info", produces = "application/json")
    public ResponseEntity<UserWithTimeParamsDTO> getUserInformation(@AuthenticationPrincipal CustomUserDetails cud,
                                                                          @PathVariable @NotNull Long companyId,
                                                                          @PathVariable @NotNull Long targetId) {

        Long managerId = cud.getUserId();

        UserWithTimeParamsDTO user = companyService.getCompanyUserWithInformation(managerId, companyId, targetId);

        log.info("Fetched {} user for company ID: {}", user, companyId);

        return ResponseEntity.ok(user);
    }

    @Override
    @PostMapping(path = "/register", consumes = "application/json", produces = "application/json")
    public ResponseEntity<AuthResponseDTO> registerCompany(@AuthenticationPrincipal CustomUserDetails cud,
                                             @RequestBody @Valid CompanyRegistrationDTO request) {

        Long managerId = cud.getUserId();

        AuthResponseDTO dto = companyService.registerCompany(managerId, request);

        log.info("Company {} registered successfully", request.getCompanyName());

        return ResponseEntity.ok(dto);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @PutMapping(path = "/set", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> setCompany(@AuthenticationPrincipal CustomUserDetails cud,
                                        @RequestBody @Valid SetCompanyDTO request) {

        Long managerId = cud.getUserId();

        companyService.setCompany(request, managerId);

        log.info("Company set for user ID: {}", request.getUserId());

        return ResponseEntity.ok().build();
    }


    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @DeleteMapping(path = "/{companyId}/remove/{userId}", produces = "application/json")
    public ResponseEntity<?> removeFromCompany(@AuthenticationPrincipal CustomUserDetails cud,
                                                                            @PathVariable @NotNull Long userId,
                                                                            @PathVariable @NotNull Long companyId) {

        Long managerId = cud.getUserId();

        companyService.removeUserFromCompany(userId, managerId, companyId);

        log.info("Company {} removed for user ID: {} by manager ID: {}", companyId, userId, managerId);

        return ResponseEntity.ok().build();
    }


    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @DeleteMapping(path = "/{companyId}/delete", produces = "application/json")
    public ResponseEntity<?> deleteCompany(@AuthenticationPrincipal CustomUserDetails cud,
                                           @PathVariable @NotNull Long companyId) {

        Long managerId = cud.getUserId();

        companyService.deleteCompany(companyId, managerId);

        log.info("Company with ID: {} deleted by manager ID: {}", companyId, managerId);

        return ResponseEntity.ok().build();
    }
}
