package com.pat.crewhive.service;

import com.pat.crewhive.dto.AuthRequestDTO;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.UserRepository;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Retrieves a User by its ID.
     *
     * @param id the ID of the user to retrieve
     * @return the User object if found
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Retrieves a User by its username.
     *
     * @param username the username of the user to retrieve
     * @return the User object if found
     */
    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Retrieves a User by its email.
     *
     * @param email the email of the user to retrieve
     * @return the User object if found
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Validates the user's password against the stored password.
     *
     * @param dto the AuthRequestDTO containing username and password
     * @return true if the password matches, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean validatePassword(AuthRequestDTO dto) {

        User user = getUserByUsername(dto.getUsername());

        boolean match = passwordEncoder.matches(dto.getPassword(), user.getPassword());

        log.info("Password match for userId {}: {}", user.getUserId(), match);

        return match;
    }

    /**
     * Encodes a raw password using the configured PasswordEncoder.
     *
     * @param raw the raw password to encode
     * @return the encoded password
     */
    public String encodePassword(String raw) {
        return passwordEncoder.encode(raw);
    }
}
