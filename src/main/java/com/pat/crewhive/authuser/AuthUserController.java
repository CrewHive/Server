package com.pat.crewhive.authuser;


import com.pat.crewhive.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthUserController implements AuthUserControllerInterface {

    private static final Logger log = LoggerFactory.getLogger(AuthUserController.class);

    private final AuthService authService;

    public AuthUserController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    @PostMapping("/rotate")
    public ResponseEntity<AuthResponseDTO> rotate(@AuthenticationPrincipal CustomUserDetails cud,
                                                  @RequestBody @Valid RotateRequestDTO request) {

        AuthResponseDTO response = authService.rotate_token(request.refreshToken());

        log.info("Token ok for user: {}", cud.getUsername());

        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegistrationDTO rDTO) {

        authService.register(rDTO);

        log.info("User {} registered successfully", rDTO.email());

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid AuthRequestDTO request) {

        AuthResponseDTO response = authService.login(request);

        log.info("Login ok for user: {}", request.email());

        return ResponseEntity.ok(response);
    }

}
