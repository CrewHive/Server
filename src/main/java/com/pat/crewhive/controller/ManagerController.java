package com.pat.crewhive.controller;

import com.pat.crewhive.dto.CompanyRegistrationDTO;
import com.pat.crewhive.dto.RegistrationDTO;
import com.pat.crewhive.service.AuthService;
import com.pat.crewhive.service.UserService;
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
@RequestMapping("/manager")
public class ManagerController {

    UserService userService;
    AuthService authService;

    @Autowired
    public ManagerController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }


    @PostMapping("/company/register")
    public ResponseEntity<?> registerCompany(@Valid @RequestBody CompanyRegistrationDTO request) {

        authService.registerCompany(request);

        log.info("Company {} registered successfully", request.getCompanyName());

        return ResponseEntity.ok().build();
    }
}
