package com.pat.crewhive.service;


import com.pat.crewhive.dto.AuthRequestDTO;
import com.pat.crewhive.dto.AuthResponseDTO;
import com.pat.crewhive.dto.UserDTO;
import com.pat.crewhive.model.user.entity.User;
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

    @Autowired
    public AuthService(UserService userService, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public AuthResponseDTO login(AuthRequestDTO request) {

        if(!userService.validatePassword(request)) {

            log.error("Invalid password for user: {}", request.getUsername());
            log.error("Invalid password: {}", request.getPassword());

            throw new BadCredentialsException("Invalid password");
        }

        User user = userService.getUserByUsername(request.getUsername());

        return new AuthResponseDTO(jwtService.generateToken(user), refreshTokenService.generateRefreshToken(user));

    }
}

