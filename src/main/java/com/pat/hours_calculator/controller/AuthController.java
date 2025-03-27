package com.pat.hours_calculator.controller;


import com.pat.hours_calculator.dto.AuthRequestDTO;
import com.pat.hours_calculator.dto.AuthResponseDTO;
import com.pat.hours_calculator.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {

        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);

    }
}
