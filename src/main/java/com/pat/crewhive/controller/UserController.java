package com.pat.crewhive.controller;


import com.pat.crewhive.dto.RegistrationDTO;
import com.pat.crewhive.dto.UserDTO;
import com.pat.crewhive.model.user.entity.CustomUserDetails;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getUser(@AuthenticationPrincipal CustomUserDetails cud) {

        UserDTO userDTO = new UserDTO(cud.getEmail(), cud.getUsername(), cud.getRole(), cud.getCompany());

        return ResponseEntity.ok(userDTO);
    }
}
