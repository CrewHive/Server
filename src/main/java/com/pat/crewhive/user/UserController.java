package com.pat.crewhive.user;


import com.pat.crewhive.authuser.AuthResponseDTO;
import com.pat.crewhive.security.CustomUserDetails;
import com.pat.crewhive.authuser.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/user")
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

        UUID userId = cud.getUserId();
        UserWithTimeParamsDTO dto = userService.getUserWithTimeParamsByUsername(userId);

        log.info("User details retrieved for user: {}", cud.getEmail());

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
    @PatchMapping(path = "/update-password", consumes = "application/json", produces = "application/json")
    public ResponseEntity<?> updatePassword(@AuthenticationPrincipal CustomUserDetails cud,
                                            @RequestBody @Valid UpdatePasswordDTO updatePasswordDTO) {

        log.info("Updating password for user: {}", cud.getEmail());

        userService.updatePassword(updatePasswordDTO.getNewPassword(), updatePasswordDTO.getOldPassword(), cud.getEmail());

        return ResponseEntity.ok().build();
    }

    @Override
    @DeleteMapping(path = "/leave-company", produces = "application/json")
    public ResponseEntity<AuthResponseDTO> leaveCompany(@AuthenticationPrincipal CustomUserDetails cud) {

        log.info("User {} is leaving their company", cud.getEmail());

        AuthResponseDTO dto = userService.leaveCompany(cud.getUserId());

        return ResponseEntity.ok(dto);
    }

    @Override
    @DeleteMapping(path = "/delete-account", produces = "application/json")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal CustomUserDetails cud) {

        log.info("Deleting account for user: {}", cud.getEmail());

        userService.deleteAccount(cud.getUserId());

        return ResponseEntity.ok().build();
    }
 }
