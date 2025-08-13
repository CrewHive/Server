package com.pat.crewhive.service;


import com.pat.crewhive.dto.AuthRequestDTO;
import com.pat.crewhive.dto.AuthResponseDTO;
import com.pat.crewhive.dto.CompanyRegistrationDTO;
import com.pat.crewhive.dto.LogoutDTO;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.CompanyRepository;
import com.pat.crewhive.security.exception.custom.InvalidTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.beans.Transient;

@Slf4j
@Service
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CompanyRepository companyRepository;

    @Autowired
    public AuthService(UserService userService, JwtService jwtService, RefreshTokenService refreshTokenService, CompanyRepository companyRepository) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.companyRepository = companyRepository;
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
     * Registers a new company.
     *
     * @param request The company registration request containing company details.
     */
    //todo consider putting this in a separate service
    @Transactional
    public void registerCompany(CompanyRegistrationDTO request) {

        Company company = new Company(request);
        companyRepository.save(company);

        log.info("Registered company: {}", company.getName());
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

