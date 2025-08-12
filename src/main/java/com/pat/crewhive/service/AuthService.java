package com.pat.crewhive.service;


import com.pat.crewhive.dto.AuthRequestDTO;
import com.pat.crewhive.dto.AuthResponseDTO;
import com.pat.crewhive.dto.CompanyRegistrationDTO;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.CompanyRepository;
import com.pat.crewhive.security.exception.custom.InvalidTokenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

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


    public void registerCompany(CompanyRegistrationDTO request) {

        Company company = new Company(request);
        companyRepository.save(company);

        log.info("Registered company: {}", company.getName());
    }
}

