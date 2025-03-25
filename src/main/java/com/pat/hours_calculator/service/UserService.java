package com.pat.hours_calculator.service;

import com.pat.hours_calculator.dto.UserDTO;
import com.pat.hours_calculator.exception.custom.ResourceNotFoundException;
import com.pat.hours_calculator.model.user.entities.User;
import com.pat.hours_calculator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;

    public UserService() {}

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDTO getUserByEmail(String email) {

        User user = userRepository.findByEmail(email).orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return new UserDTO(user.getUserId(), user.getEmail(), user.getUsername(), user.getCompany().getName());
    }

    public UserDTO getUserById(Long id) {

        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new UserDTO(user.getUserId(), user.getEmail(), user.getUsername(), user.getCompany().getName());
    }

    public User getUserByIdIdentity(Long id) {

        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public boolean validatePassword(UserDTO dto) {

        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return passwordEncoder.matches(user.getPassword(), user.getPassword());
    }
}
