package com.pat.hours_calculator.controller;


import com.pat.hours_calculator.dto.UserDTO;
import com.pat.hours_calculator.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
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

        log.info("User with id {} retrieved successfully", id);

        return ResponseEntity.ok(user);
    }
}
