package com.pat.hours_calculator.service;


import com.pat.hours_calculator.dto.AuthRequestDTO;
import com.pat.hours_calculator.dto.AuthResponseDTO;
import com.pat.hours_calculator.dto.UserDTO;
import com.pat.hours_calculator.model.user.entity.User;
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

        UserDTO userDTO = userService.getUserDTOByUsername(request.getUsername());

        if(!userService.validatePassword(userDTO)) {

            log.error("Invalid password for user: {}", request.getUsername());
            log.error("Invalid password: {}", request.getPassword());

            throw new BadCredentialsException("Invalid password");
        }

        User user = userService.getUserByIdIdentity(userDTO.getUserId());

        return new AuthResponseDTO(jwtService.generateToken(user), refreshTokenService.generateRefreshToken(user));

    }
}

