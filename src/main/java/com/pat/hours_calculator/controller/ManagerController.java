package com.pat.hours_calculator.controller;

import com.pat.hours_calculator.dto.RegistrationDTO;
import com.pat.hours_calculator.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager")
public class ManagerController {

    UserService userService;

    @Autowired
    public ManagerController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegistrationDTO rDTO) {

        userService.register(rDTO);

        return ResponseEntity.ok().build();
    }
}
