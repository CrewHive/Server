package com.pat.crewhive.manager;

import com.pat.crewhive.security.CustomUserDetails;
import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import com.pat.crewhive.user.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/manager")
public class ManagerController implements ManagerControllerInterface {

    private static final Logger log = LoggerFactory.getLogger(ManagerController.class);

    private final RoleService roleService;
    private final UserService userService;

    public ManagerController(RoleService roleService,
                             UserService userService) {
        this.roleService = roleService;
        this.userService = userService;
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping(path = "/create-role", produces = "application/json")
    public ResponseEntity<?> createRole(@AuthenticationPrincipal CustomUserDetails cud,
                                        @RequestBody @NoHtml @NotBlank(message = "The role name is required") String roleName) {

        roleService.createRole(roleName, cud.getCompanyId());

        log.info("Role {} created successfully", roleName);

        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping(path = "/update-user-role", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> updateUserRole(@AuthenticationPrincipal CustomUserDetails cud,
                                            @RequestBody @Valid UpdateUserRoleDTO updateUserRoleDTO) {

        UUID targetId = updateUserRoleDTO.userId();

        roleService.updateUserRole(targetId, updateUserRoleDTO.newRole(), cud.getCompanyId());

        log.info("Updated user role for user: {}", cud.getEmail());

        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping(path = "/update-user-work-info", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> updateUserWorkInfo(@AuthenticationPrincipal CustomUserDetails cud,
                                                @RequestBody @Valid UpdateUserWorkInfoDTO dto) {

        UUID companyId = cud.getCompanyId();

        userService.updateUserTimeParams(dto, companyId);

        log.info("Updated user time params for user: {}", cud.getEmail());

        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping(path = "/delete-role/{roleName}", produces = "application/json")
    public ResponseEntity<?> deleteRole(@AuthenticationPrincipal CustomUserDetails cud,
                                        @PathVariable @NoHtml @NotBlank(message = "The role name is required") String roleName) {

        UUID companyId = cud.getCompanyId();

        roleService.deleteRole(roleName, companyId, cud.getUserId());

        log.info("Role {} deleted successfully", roleName);

        return ResponseEntity.ok().build();
    }

}
