package com.pat.crewhive.controller;


import com.pat.crewhive.dto.User.LogoutDTO;
import com.pat.crewhive.dto.User.UserDTO;
import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
import com.pat.crewhive.service.AuthService;
import com.pat.crewhive.service.CompanyService;
import com.pat.crewhive.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final CompanyService companyService;

    public UserController(UserService userService,
                          AuthService authService,
                          CompanyService companyService) {

        this.userService = userService;
        this.authService = authService;
        this.companyService = companyService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getUser(@AuthenticationPrincipal CustomUserDetails cud) {

        UserDTO userDTO = new UserDTO(cud.getEmail(), cud.getUsername(), cud.getRole(), cud.getCompanyId());

        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@Valid @RequestBody LogoutDTO request) {

        authService.logout(request);
        log.info("Logout ok for user: {}", request.getUsername());

        return ResponseEntity.ok().build();
    }
}
