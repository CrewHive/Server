package com.pat.crewhive.service;


import com.pat.crewhive.dto.*;
import com.pat.crewhive.model.auth.entity.RefreshToken;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.CompanyRepository;
import com.pat.crewhive.repository.RefreshTokenRepository;
import com.pat.crewhive.repository.UserRepository;
import com.pat.crewhive.security.exception.custom.InvalidTokenException;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.Transient;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Autowired
    public AuthService(UserService userService,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }


    /**
     * Authenticates a user and generates JWT and Refresh Token.
     *
     * @param request The authentication request containing username and password.
     * @return AuthResponseDTO containing JWT and Refresh Token.
     */
    @Transactional(readOnly = true)
    public AuthResponseDTO login(AuthRequestDTO request) {

        User user = userService.getUserByUsername(request.getUsername());

        if (!userService.validatePassword(request)) {
            log.error("Invalid password for user: {}", request.getUsername());

            throw new BadCredentialsException("Invalid credentials");
        }

        return new AuthResponseDTO(
                jwtService.generateToken(user.getUserId(), user.getUsername(), user.getRole().getRole().getRoleName()),
                refreshTokenService.generateRefreshToken(user)
        );
    }

    /**
     * Registers a new user with the provided details.
     *
     * @param request The registration request containing username, email, and password.
     * @throws BadCredentialsException if the username or email already exists.
     */
    @Transactional
    public void register(RegistrationDTO request) {

        //todo crea controlli su username e email
        userService.getUserByUsername(request.getUsername());

        if (userService.getUserByEmail(request.getEmail()) != null) {

            log.error("Email already registered: {}", request.getEmail());
            throw new BadCredentialsException("Email already registered");
        }

        String encodedPassword = userService.encodePassword(request.getPassword());

        User newUser = new User(request.getUsername(), request.getEmail(), encodedPassword);

        userRepository.save(newUser);

        log.info("User registered successfully: {}", request.getUsername());
    }

    /**
     * Rotates the refresh token for a user.
     *
     * @param token The request containing the string of the Refresh Token.
     * @return AuthResponseDTO containing new JWT and Refresh Token.
     */
    @Transactional
    public AuthResponseDTO rotate_token(String token) {

        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        try { UUID.fromString(token); }
        catch (IllegalArgumentException e) { throw new InvalidTokenException("Invalid refresh token"); }


        RefreshToken rt = refreshTokenRepository
                .findByTokenWithUserAndRole(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));


        if (refreshTokenService.isExpired(rt)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        User owner = rt.getUser();
        if (owner == null || owner.getUserId() == null) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        Long userId = owner.getUserId();
        String role = owner.getRole().getRole().getRoleName();
        String username = owner.getUsername();

        String newAccessToken = jwtService.generateToken(userId, role, username);

        String newRefreshToken = refreshTokenService.rotateRefreshToken(rt);

        log.info("Refresh rotated for userId={}", userId);
        return new AuthResponseDTO(newAccessToken, newRefreshToken);
    }

    /**
     * Logs out a user by invalidating their refresh token.
     *
     * @param request The logout request containing the refresh token and username.
     */
    @Transactional
    public void logout(LogoutDTO request) {

        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) throw new InvalidTokenException("Refresh Token is missing");

        RefreshToken rt = refreshTokenService.getRefreshToken(request.getRefreshToken());

        if (rt == null || refreshTokenService.isExpired(rt)) {

            throw new InvalidTokenException("Refresh Token expired or missing");
        }

        User owner = refreshTokenService.getOwner(rt);
        if (owner == null || !owner.getUsername().equals(request.getUsername())) {

            throw new InvalidTokenException("Refresh Token does not belong to user");
        }

        refreshTokenService.invalidateRefreshToken(rt);
        log.info("User {} logged out successfully", request.getUsername());
    }
}

