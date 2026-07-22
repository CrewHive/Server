package com.pat.crewhive.authuser;


import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthUserController implements AuthUserControllerInterface {

    private final AuthService authService;

    @Autowired
    public AuthUserController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    @PostMapping("/rotate")
    public ResponseEntity<AuthResponseDTO> rotate(@RequestBody @Valid RotateRequestDTO request) {

        AuthResponseDTO response = authService.rotate_token(request.getRefreshToken());

        log.info("Token ok for user:");

        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegistrationDTO rDTO) {

        authService.register(rDTO);

        log.info("User {} registered successfully", rDTO.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid AuthRequestDTO request) {

        AuthResponseDTO response = authService.login(request);

        log.info("Login ok for user: {}", request.getEmail());

        return ResponseEntity.ok(response);
    }

}
