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

import java.util.Optional;


@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final CompanyRepository companyRepository;

    private final RoleRepository roleRepository;


    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       CompanyRepository companyRepository,
                       RoleRepository roleRepository) {
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
    public User getUserByUsername(String username) throws  ResourceNotFoundException {

        Optional<User> user = userRepository.findByUsername(username);

        if (user.isEmpty()) {

            log.error("User not found with username: {}", username);
            return null;
        }

        log.info("User found with username: {}", user.get().getUsername());

        return user.get();
    }

    /**
     * Retrieves a user by their email.
     *
     * @param email the email of the user to retrieve
     * @return the User object if found
     * @throws ResourceNotFoundException if no user is found with the given email
     */
    public User getUserByEmail(String email) {

        Optional<User> user = userRepository.findByEmail(email);

        if (user.isEmpty()) {

            log.error("User not found with email: {}", email);
            return null;
        }

        log.info("User found with email: {}", user.get().getEmail());

        return user.get();
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

    /**
     * Encodes the password using the configured PasswordEncoder.
     *
     * @param password the raw password to encode
     * @return the encoded password
     */
    public String encodePassword(String password) {

        return passwordEncoder.encode(password);

    }
}
