package com.pat.crewhive.controller;


import com.pat.crewhive.dto.*;
import com.pat.crewhive.service.AuthService;
import com.pat.crewhive.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthUserController {

    private final AuthService authService;
    private final UserService userService;

    @Autowired
    public AuthUserController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/rotate")
    public ResponseEntity<AuthResponseDTO> rotate(@Valid @RequestBody RotateRequestDTO request) {

        AuthResponseDTO response = authService.rotate_token(request.getRefreshToken());

        log.info("Token ok for user:");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationDTO rDTO) {

        authService.register(rDTO);

        log.info("User {} registered successfully", rDTO.getUsername());

        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {

        AuthResponseDTO response = authService.login(request);

        log.info("Login ok for user: {}", request.getUsername());

        return ResponseEntity.ok(response);
    }

}
