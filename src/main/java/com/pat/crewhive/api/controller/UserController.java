package com.pat.crewhive.api.controller;


import com.pat.crewhive.api.swagger.interfaces.UserControllerInterface;
import com.pat.crewhive.dto.user.LogoutDTO;
import com.pat.crewhive.dto.user.UpdatePasswordDTO;
import com.pat.crewhive.dto.user.UpdateUsernameOutputDTO;
import com.pat.crewhive.dto.user.UserWithTimeParamsDTO;
import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import com.pat.crewhive.service.AuthService;
import com.pat.crewhive.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController implements UserControllerInterface {

    private final AuthService authService;
    private final UserService userService;

    public UserController(AuthService authService,
                          UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @Override
    @GetMapping(path ="/me", produces = "application/json")
    public ResponseEntity<UserWithTimeParamsDTO> getUser(@AuthenticationPrincipal CustomUserDetails cud) {

        String username = cud.getUsername();

        UserWithTimeParamsDTO dto = userService.getUserWithTimeParamsByUsername(username);
        log.info("User details retrieved for user: {}", cud.getUsername());

        return ResponseEntity.ok(dto);
    }

    @Override
    @PostMapping(path = "/logout", produces = "application/json")
    public ResponseEntity<?> logout(@RequestBody @Valid LogoutDTO request) {

        authService.logout(request);
        log.info("Logout ok for user: {}", request.getUserId());

        return ResponseEntity.ok().build();
    }

    @Override
    @PatchMapping(path = "/update-username", produces = "application/json")
    public ResponseEntity<UpdateUsernameOutputDTO> updateUsername(@AuthenticationPrincipal CustomUserDetails cud,
                                                                  @RequestBody @NoHtml @NotBlank String newUsername) {

        log.info("Updating username for user: {}", cud.getUsername());

        UpdateUsernameOutputDTO dto = userService.updateUsername(newUsername, cud.getUsername());

        return ResponseEntity.ok(dto);
    }

    @Override
    @PatchMapping(path = "/update-password", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> updatePassword(@AuthenticationPrincipal CustomUserDetails cud,
                                            @RequestBody @Valid UpdatePasswordDTO updatePasswordDTO) {

        log.info("Updating password for user: {}", cud.getUsername());

        userService.updatePassword(updatePasswordDTO.getNewPassword(), updatePasswordDTO.getOldPassword(), cud.getUsername());

        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping(path = "/leave-company", produces = "application/json")
    public ResponseEntity<?> leaveCompany(@AuthenticationPrincipal CustomUserDetails cud) {

        log.info("User {} is leaving their company", cud.getUsername());

        userService.leaveCompany(cud.getUserId());

        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping(path = "/delete-account", produces = "application/json")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal CustomUserDetails cud) {

        log.info("Deleting account for user: {}", cud.getUsername());

        userService.deleteAccount(cud.getUserId());

        return ResponseEntity.ok().build();
    }
 }
