package com.pat.hours_calculator.controller;


import com.pat.hours_calculator.dto.AuthRequestDTO;
import com.pat.hours_calculator.dto.AuthResponseDTO;
import com.pat.hours_calculator.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class AuthController {

    private final AuthService authService;

    @Autowired
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO request) {

        try {

            AuthResponseDTO response = authService.login(request);

            if(response == null) {

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            return ResponseEntity.ok(response);

        } catch(Exception e) {

            return ResponseEntity.badRequest().build();
        }

    }
}
