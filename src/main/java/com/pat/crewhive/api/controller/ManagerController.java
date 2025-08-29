package com.pat.crewhive.api.controller;

import com.pat.crewhive.api.swagger.interfaces.ManagerControllerInterface;
import com.pat.crewhive.dto.manager.UpdateUserRoleDTO;
import com.pat.crewhive.dto.manager.UpdateUserWorkInfoDTO;
import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import com.pat.crewhive.service.RoleService;
import com.pat.crewhive.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/manager")
public class ManagerController implements ManagerControllerInterface {

    private final RoleService roleService;
    private final UserService userService;

    public ManagerController(RoleService roleService,
                             UserService userService) {
        this.roleService = roleService;
        this.userService = userService;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @PostMapping(path = "/create-role", produces = "application/json")
    public ResponseEntity<?> createRole(@AuthenticationPrincipal CustomUserDetails cud,
                                        @RequestBody @NoHtml @NotBlank(message = "The role name is required") String roleName) {

        roleService.createRole(roleName, cud.getCompanyId());

        log.info("Role {} created successfully", roleName);

        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @PatchMapping(path = "/update-user-role", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> updateUserRole(@AuthenticationPrincipal CustomUserDetails cud,
                                            @RequestBody @Valid UpdateUserRoleDTO updateUserRoleDTO) {

        Long targetId = updateUserRoleDTO.getUserId();

        roleService.updateUserRole(targetId, updateUserRoleDTO.getNewRole(), cud.getCompanyId());

        log.info("Updated user role for user: {}", cud.getUsername());

        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @PatchMapping(path = "/update-user-work-info", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> updateUserWorkInfo(@AuthenticationPrincipal CustomUserDetails cud,
                                                @RequestBody @Valid UpdateUserWorkInfoDTO dto) {

        Long companyId = cud.getCompanyId();

        userService.updateUserTimeParams(dto, companyId);

        log.info("Updated user time params for user: {}", cud.getUsername());

        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @DeleteMapping(path = "/delete-role/{roleName}", produces = "application/json")
    public ResponseEntity<?> deleteRole(@AuthenticationPrincipal CustomUserDetails cud,
                                        @PathVariable @NoHtml @NotBlank(message = "The role name is required") String roleName) {

        Long companyId = cud.getCompanyId();

        roleService.deleteRole(roleName, companyId);

        log.info("Role {} deleted successfully", roleName);

        return ResponseEntity.ok().build();
    }

}
