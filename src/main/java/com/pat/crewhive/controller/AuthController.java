package com.pat.crewhive.controller;


import com.pat.crewhive.dto.AuthRequestDTO;
import com.pat.crewhive.dto.AuthResponseDTO;
import com.pat.crewhive.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {

        AuthResponseDTO response = authService.login(request);

        log.info("Login ok for user: {}", request.getUsername());

        return ResponseEntity.ok(response);

    }
}
