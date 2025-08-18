package com.pat.crewhive.api.controller;


import com.pat.crewhive.api.controller.interfaces.UserControllerInterface;
import com.pat.crewhive.dto.User.LogoutDTO;
import com.pat.crewhive.dto.User.UserDTO;
import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
import com.pat.crewhive.service.AuthService;
import com.pat.crewhive.service.CompanyService;
import com.pat.crewhive.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController implements UserControllerInterface {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    @GetMapping("/me")
    public ResponseEntity<UserDTO> getUser(@AuthenticationPrincipal CustomUserDetails cud) {

        UserDTO userDTO = new UserDTO(cud.getEmail(), cud.getUsername(), cud.getRole(), cud.getCompanyId());
        log.info("User details retrieved for user: {}", cud.getUsername());

        return ResponseEntity.ok(userDTO);
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody LogoutDTO request) {

        authService.logout(request);
        log.info("Logout ok for user: {}", request.getUsername());

        return ResponseEntity.ok().build();
    }
}
