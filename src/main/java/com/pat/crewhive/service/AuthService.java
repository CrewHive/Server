package com.pat.crewhive.service;


import com.pat.crewhive.dto.*;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.CompanyRepository;
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

@Slf4j
@Service
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthService(UserService userService,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       CompanyRepository companyRepository,
                       PasswordEncoder passwordEncoder,
                       UserRepository userRepository) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }


    /**
     * Authenticates a user and generates JWT and Refresh Token.
     *
     * @param request The authentication request containing username and password.
     * @return AuthResponseDTO containing JWT and Refresh Token.
     */
    public AuthResponseDTO login(AuthRequestDTO request) {

        User user = userService.getUserByUsername(request.getUsername());

        if (!userService.validatePassword(request)) {
            log.error("Invalid password for user: {}", request.getUsername());

            throw new BadCredentialsException("Invalid credentials");
        }

        return new AuthResponseDTO(
                jwtService.generateToken(user),
                refreshTokenService.generateRefreshToken(user)
        );
    }

    /**
     * Registers a new user with the provided details.
     *
     * @param request The registration request containing username, email, and password.
     * @throws BadCredentialsException if the username or email already exists.
     */
    public void register(RegistrationDTO request) {

        if (userService.getUserByUsername(request.getUsername()) != null) {

            log.error("User already exists: {}", request.getUsername());
            throw new BadCredentialsException("User already exists");
        }

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
     * @param request The request containing the refresh token and username.
     * @return AuthResponseDTO containing new JWT and Refresh Token.
     */
    public AuthResponseDTO rotate_token(AuthRequestDTO request) {

        String rt = (request.getRefreshToken() != null) ? request.getRefreshToken().getToken() : null;

        if (rt == null || refreshTokenService.isExpired(rt)) {
            throw new InvalidTokenException("Refresh Token expired or missing");
        }

        User owner = refreshTokenService.getOwner(rt);

        if (owner == null || !owner.getUsername().equals(request.getUsername())) {
            throw new InvalidTokenException("Refresh Token does not belong to user");
        }

        return new AuthResponseDTO(
                jwtService.generateToken(owner),
                refreshTokenService.rotateRefreshToken(rt)
        );
    }

    /**
     * Logs out a user by invalidating their refresh token.
     *
     * @param request The logout request containing the refresh token and username.
     */
    public void logout(LogoutDTO request) {

        String rt = request.getRefreshToken() != null ? request.getRefreshToken() : null;

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

