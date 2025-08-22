package com.pat.crewhive.api.controller;

import com.pat.crewhive.api.swagger.interfaces.ManagerControllerInterface;
import com.pat.crewhive.dto.Manager.UpdateUserRoleDTO;
import com.pat.crewhive.dto.Manager.UpdateUserWorkInfoDTO;
import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
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
    @PostMapping("/create-role")
    public ResponseEntity<?> createRole(@AuthenticationPrincipal CustomUserDetails cud,
                                             @RequestBody @NotBlank(message = "The role name is required") String roleName) {

        roleService.createRole(roleName, cud.getCompanyId());

        log.info("Role {} created successfully", roleName);

        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @PatchMapping("/update-role")
    public ResponseEntity<?> updateUserRole(@AuthenticationPrincipal CustomUserDetails cud,
                                                 @Valid @RequestBody UpdateUserRoleDTO updateUserRoleDTO) {

        Long targetId = updateUserRoleDTO.getUserId();

        roleService.updateUserRole(targetId, updateUserRoleDTO.getNewRole(), cud.getCompanyId());

        log.info("Updated user role for user: {}", cud.getUsername());

        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @PatchMapping("/update-user-time-params")
    public ResponseEntity<?> updateUserWorkInfo(@AuthenticationPrincipal CustomUserDetails cud,
                                                       @RequestBody @Valid UpdateUserWorkInfoDTO dto) {

        Long companyId = cud.getCompanyId();

        userService.updateUserTimeParams(dto, companyId);

        log.info("Updated user time params for user: {}", cud.getUsername());

        return ResponseEntity.ok().build();
    }

}
