package com.pat.crewhive.service;

import com.pat.crewhive.dto.AuthRequestDTO;
import com.pat.crewhive.dto.RegistrationDTO;
import com.pat.crewhive.repository.RoleRepository;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.CompanyRepository;
import com.pat.crewhive.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
public class UserService {

    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;

    private CompanyRepository companyRepository;

    private RoleRepository roleRepository;

    public UserService() {}

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, CompanyRepository companyRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.companyRepository = companyRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param id the ID of the user to retrieve
     * @return the User object if found
     * @throws ResourceNotFoundException if no user is found with the given ID
     */
    public User getUserById(Long id) {

        User user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("User found with id: {}", user.getUserId());

        return user;
    }

    /**
     * Retrieves a user by their username.
     *
     * @param username the username of the user to retrieve
     * @return the User object if found
     * @throws ResourceNotFoundException if no user is found with the given username
     */
    public User getUserByUsername(String username) {

        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));

        log.info("User found with username: {}", user.getUsername());

        return user;
    }

    /**
     * Validates the user's password against the stored password.
     *
     * @param dto the AuthRequestDTO containing username and password
     * @return true if the password matches, false otherwise
     * @throws ResourceNotFoundException if no user is found with the given username
     */
    public boolean validatePassword(AuthRequestDTO dto) {

        User user = userRepository.findByUsername(dto.getUsername()).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        log.info("User found with id: {}", user.getUserId());

        boolean match = passwordEncoder.matches(dto.getPassword(), user.getPassword());

        log.info("Password match: {}", match);

        return match;
    }

    //todo: assolutamente da rivedere
    @Transactional
    public void register(RegistrationDTO rDTO) {


    }

}
