package com.pat.hours_calculator.controller;


import com.pat.hours_calculator.dto.RegistrationDTO;
import com.pat.hours_calculator.dto.UserDTO;
import com.pat.hours_calculator.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
                this.userService = userService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserDTOById(id);
        return ResponseEntity.ok(user);
    }
}
