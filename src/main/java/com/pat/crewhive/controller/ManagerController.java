package com.pat.crewhive.controller;

import com.pat.crewhive.dto.RegistrationDTO;
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

    @Autowired
    public ManagerController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegistrationDTO rDTO) {

        userService.register(rDTO);

        log.info("User {} registered successfully", rDTO.getUsername());

        return ResponseEntity.ok().build();
    }
}
