package com.pat.crewhive.service;

import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.UserRepository;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.service.utils.PasswordUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordUtil passwordUtil;

    public UserService(UserRepository userRepository,
                       PasswordUtil passwordUtil) {
        this.userRepository = userRepository;
        this.passwordUtil = passwordUtil;
    }

    /**
     * Updates the user information in the database.
     *
     * @param user the User object containing updated information
     */
    @Transactional
    public void updateUser(User user) {
        userRepository.save(user);
    }


    /**
     * Retrieves a User by its ID.
     *
     * @param id the ID of the user to retrieve
     * @return the User object if found
     * @throws ResourceNotFoundException if the user is not found
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
     * @throws ResourceNotFoundException if the user is not found
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
     * @throws ResourceNotFoundException if the user is not found
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Updates the username of a user.
     *
     * @param newUsername the new username to set
     * @param oldUsername the current username of the user
     * @throws ResourceAlreadyExistsException if the new username already exists
     */
    @Transactional
    public void updateUsername(String newUsername, String oldUsername) {

        if (userRepository.findByUsername(newUsername).isPresent()) {
            throw new ResourceAlreadyExistsException("Username already exists");
        }

        User user = getUserByUsername(oldUsername);
        user.setUsername(newUsername);

        userRepository.save(user);
        log.info("Updated username from {} to {}", oldUsername, newUsername);
    }

    /**
     * Updates the user's password.
     *
     * @param newPassword the new password to set
     * @param oldPassword the current password of the user
     * @param username    the username of the user
     */
    @Transactional
    public void updatePassword(String newPassword, String oldPassword, String username) {

        User user = getUserByUsername(username);

        if(!passwordUtil.isStrong(newPassword)) {

            log.info("New password is not strong enough for user: {}", username);

            throw new BadCredentialsException("Invalid password");
        }

        if(!passwordUtil.matches(oldPassword, user.getPassword())) {

            log.info("Old password does not match for user: {}", username);

            throw new BadCredentialsException("Old password does not match");
        }

        user.setPassword(passwordUtil.encodePassword(newPassword));

        userRepository.save(user);
        log.info("Updated password for user: {}", username);
    }

}
