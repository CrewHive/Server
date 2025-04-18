package com.pat.hours_calculator.service;

import com.pat.hours_calculator.dto.RegistrationDTO;
import com.pat.hours_calculator.dto.UserDTO;
import com.pat.hours_calculator.exception.custom.ResourceAlreadyExistsException;
import com.pat.hours_calculator.exception.custom.ResourceNotFoundException;
import com.pat.hours_calculator.model.company.entity.Company;
import com.pat.hours_calculator.model.user.entity.User;
import com.pat.hours_calculator.repository.CompanyRepository;
import com.pat.hours_calculator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;

    private CompanyRepository companyRepository;

    public UserService() {}

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, CompanyRepository companyRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.companyRepository = companyRepository;
    }

    public UserDTO getUserDTOByUsername(String username) {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        return new UserDTO(user.getUser_id(), user.getEmail(), user.getUsername(), user.getCompany().getName());
    }

    public UserDTO getUserDTOById(Long id) {

        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return new UserDTO(user.getUser_id(), user.getEmail(), user.getUsername(), user.getCompany().getName());
    }

    public User getUserByIdIdentity(Long id) {

        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public boolean validatePassword(UserDTO dto) {

        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return passwordEncoder.matches(user.getPassword(), user.getPassword());
    }

    public void register(RegistrationDTO rDTO) {

        Company companyName = companyRepository.findByName(rDTO.getCompanyName()).orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        String password = passwordEncoder.encode(rDTO.getPassword());

        if (userRepository.existsByUsername(rDTO.getUsername())) {throw new ResourceAlreadyExistsException("Username already exists");}

        User user = new User(rDTO.getUsername(), rDTO.getEmail(), password, rDTO.getRole(), companyName, rDTO.getContract());

        userRepository.save(user);
    }
}
