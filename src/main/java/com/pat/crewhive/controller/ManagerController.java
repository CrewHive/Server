package com.pat.crewhive.controller;

import com.pat.crewhive.dto.Manager.UpdateUserRoleDTO;
import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
import com.pat.crewhive.service.CompanyService;
import com.pat.crewhive.service.RoleService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/manager")
public class ManagerController {

    private final RoleService roleService;

    public ManagerController(RoleService roleService,
                             CompanyService companyService) {

        this.roleService = roleService;
    }

    @PostMapping("/create-role")
    public ResponseEntity<String> createRole(@AuthenticationPrincipal CustomUserDetails cud,
                                             @RequestBody @NotBlank(message = "The role name is required") String roleName) {

        roleService.createRole(roleName, cud.getCompanyId());

        log.info("Role {} created successfully", roleName);

        return ResponseEntity.ok("Role created successfully");
    }


    @PatchMapping("/update-role")
    public ResponseEntity<String> updateUserRole(@AuthenticationPrincipal CustomUserDetails cud,
                                                 @Valid @RequestBody UpdateUserRoleDTO updateUserRoleDTO) {

        Long targetId = updateUserRoleDTO.getUserId();

        roleService.updateUserRole(targetId, updateUserRoleDTO.getNewRole(), cud.getCompanyId());

        log.info("Updated user role for user: {}", cud.getUsername());

        return ResponseEntity.ok("User role updated successfully");
    }
}
