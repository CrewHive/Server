package com.pat.crewhive.service;

import com.pat.crewhive.dto.RegistrationDTO;
import com.pat.crewhive.dto.UserDTO;
import com.pat.crewhive.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.CompanyRepository;
import com.pat.crewhive.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
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

        log.info("User found with username: {}", user.getUsername());

        return new UserDTO(user.getUserId(), user.getEmail(), user.getUsername(), user.getRole(), user.getContract(), user.getCompany().getName());
    }

    public UserDTO getUserDTOById(Long id) {

        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("User found with id: {}", user.getUserId());

        return new UserDTO(user.getUserId(), user.getEmail(), user.getUsername(), user.getRole(), user.getContract(), user.getCompany().getName());
    }

    public User getUserByIdIdentity(Long id) {

        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("User found with id: {}", user.getUserId());

        return user;
    }

    public boolean validatePassword(UserDTO dto) {

        User user = userRepository.findById(dto.getUserId()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("User found with id: {}", user.getUserId());

        boolean match = passwordEncoder.matches(user.getPassword(), user.getPassword());

        log.info("Password match: {}", match);

        return match;
    }

    public void register(RegistrationDTO rDTO) {

        Company companyName = companyRepository.findByName(rDTO.getCompanyName()).orElseThrow(() -> new ResourceNotFoundException("Company not found"));

        log.info("Company found with name: {}", companyName.getName());

        String password = passwordEncoder.encode(rDTO.getPassword());

        if (userRepository.existsByUsername(rDTO.getUsername())) {

            log.error("Username already exists: {}", rDTO.getUsername());
            throw new ResourceAlreadyExistsException("Username already exists");
        }

        User user = new User(rDTO.getUsername(), rDTO.getEmail(), password, rDTO.getRole(), companyName, rDTO.getContract());

        log.info("User created with id: {}", user.getUserId());

        userRepository.save(user);
    }
}
