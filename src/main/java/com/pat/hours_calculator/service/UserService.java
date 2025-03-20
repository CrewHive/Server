package com.pat.hours_calculator.service;

import com.pat.hours_calculator.dto.UserDTO;
import com.pat.hours_calculator.model.user.entities.User;
import com.pat.hours_calculator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public UserDTO getUserById(Long id) {

        User user = userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        return new UserDTO(user.getUserId(), user.getEmail(), user.getUsername(), user.getCompany().getName());
    }
}
