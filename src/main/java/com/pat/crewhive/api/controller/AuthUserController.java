package com.pat.crewhive.api.controller;


import com.pat.crewhive.api.swagger.interfaces.AuthUserControllerInterface;
import com.pat.crewhive.dto.Auth.AuthRequestDTO;
import com.pat.crewhive.dto.Auth.AuthResponseDTO;
import com.pat.crewhive.dto.Auth.RegistrationDTO;
import com.pat.crewhive.dto.Auth.RotateRequestDTO;
import com.pat.crewhive.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AuthResponseDTO> rotate(@Valid @RequestBody RotateRequestDTO request) {

        AuthResponseDTO response = authService.rotate_token(request.getRefreshToken());

        log.info("Token ok for user:");

        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationDTO rDTO) {

        authService.register(rDTO);

        log.info("User {} registered successfully", rDTO.getUsername());

        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody AuthRequestDTO request) {

        AuthResponseDTO response = authService.login(request);

        log.info("Login ok for user: {}", request.getUsername());

        return ResponseEntity.ok(response);
    }

}
