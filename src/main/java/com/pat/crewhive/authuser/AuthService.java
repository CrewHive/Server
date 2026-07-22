package com.pat.crewhive.authuser;


import com.pat.crewhive.user.LogoutDTO;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.user.User;
import com.pat.crewhive.manager.Role;
import com.pat.crewhive.manager.UserRole;
import com.pat.crewhive.user.UserRepository;
import com.pat.crewhive.user.UserService;
import com.pat.crewhive.security.JwtService;
import com.pat.crewhive.manager.RoleService;
import com.pat.crewhive.security.exception.custom.InvalidTokenException;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.common.PasswordUtil;
import com.pat.crewhive.common.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RoleService roleService;
    private final PasswordUtil passwordUtil;
    private final EmailUtil emailUtil;
    private final StringUtils stringUtils;

    public AuthService(UserService userService,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       UserRepository userRepository,
                       RoleService roleService,
                       PasswordUtil passwordUtil,
                       EmailUtil emailUtil,
                       StringUtils stringUtils) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordUtil = passwordUtil;
        this.emailUtil = emailUtil;
        this.stringUtils = stringUtils;
    }


    /**
     * Authenticates a user and generates JWT and Refresh Token.
     *
     * @param request The authentication request containing username and password.
     * @return AuthResponseDTO containing JWT and Refresh Token.
     * @throws BadCredentialsException if the username or password is invalid.
     */
    @Transactional
    public AuthResponseDTO login(AuthRequestDTO request) {

        String normalizedEmail = stringUtils.normalizeString(request.getEmail());
        User user = userService.getUserByEmail(normalizedEmail);

        if (passwordUtil.NotMatches(request.getPassword(), user.getPassword())) {
            log.error("Invalid password for user: {}", normalizedEmail);

            throw new BadCredentialsException("Invalid credentials");
        }

        log.info("User {} authenticated successfully", normalizedEmail);

        RefreshToken rt = refreshTokenService.getRefreshTokenByUser(user);

        if (rt != null) {

            refreshTokenService.invalidateRefreshToken(rt);
        }

        Long company = user.getCompany() == null ? null : user.getCompany().getCompanyId();

        return new AuthResponseDTO(
                jwtService.generateToken(user.getUserId(), normalizedEmail, user.getFirstName(), user.getLastName(), user.getRole().getRole().getRoleName(), company),
                refreshTokenService.generateRefreshToken(user)
        );
    }

    /**
     * Registers a new user with the provided details.
     *
     * @param request The registration request containing username, email, and password.
     * @throws BadCredentialsException if the email format is invalid or the password is weak.
     * @throws ResourceAlreadyExistsException if the username or email already exist.
     * @return The ID of the newly registered user.
     */
    @Transactional
    public void register(RegistrationDTO request) {

        String normalizedEmail = stringUtils.normalizeString(request.getEmail());
        if (!emailUtil.isValidEmail(normalizedEmail)) {

            log.error("Invalid email format: {}", normalizedEmail);
            throw new BadCredentialsException("Invalid email format");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {

            log.error("Email already registered: {}", normalizedEmail);
            throw new ResourceAlreadyExistsException("Email already registered");
        }

        if (!passwordUtil.isStrong(request.getPassword())) {

            log.error("Weak password provided for user: {}", normalizedEmail);
            throw new BadCredentialsException("Weak password provided");
        }

        String encodedPassword = passwordUtil.encodePassword(request.getPassword());

        User newUser = new User(normalizedEmail, request.getFirstName(), request.getLastName(), encodedPassword);

        Role role = roleService.getOrCreateGlobalRoleUser();

        newUser.setRole(new UserRole(newUser, role));

        userRepository.save(newUser);

        log.info("User registered successfully: {}", normalizedEmail);
    }

    /**
     * Rotates the refresh token for a user.
     *
     * @param token The request containing the string of the Refresh Token.
     * @return AuthResponseDTO containing new JWT and Refresh Token.
     * @throws InvalidTokenException if the refresh token is invalid or expired.
     */
    @Transactional
    public AuthResponseDTO rotate_token(String token) {

        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        try { UUID.fromString(token); }
        catch (IllegalArgumentException e) { throw new InvalidTokenException("Invalid refresh token"); }

        RefreshToken rt = refreshTokenService.getRefreshTokenByTokenWithUserAndRole(token);

        if (refreshTokenService.isExpired(rt)) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        User owner = rt.getUser();
        if (owner == null || owner.getUserId() == null) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        Long userId = owner.getUserId();
        String normalizedEmail = stringUtils.normalizeString(owner.getEmail());
        String firstName = owner.getFirstName();
        String lastName = owner.getLastName();
        String role = owner.getRole().getRole().getRoleName();
        Company company = owner.getCompany();
        Long companyId = (company != null) ? company.getCompanyId() : null;


        String newAccessToken = jwtService.generateToken(userId, normalizedEmail, firstName, lastName, role, companyId);

        String newRefreshToken = refreshTokenService.rotateRefreshToken(rt);

        log.info("Refresh rotated for userId={}", userId);
        return new AuthResponseDTO(newAccessToken, newRefreshToken);
    }

    /**
     * Logs out a user by invalidating their refresh token.
     *
     * @param request The logout request containing the refresh token and username.
     * @throws InvalidTokenException if the refresh token is invalid or does not belong to the user.
     */
    @Transactional
    public void logout(LogoutDTO request) {

        if (request.getRefreshToken() == null || request.getRefreshToken().isBlank()) throw new InvalidTokenException("Refresh Token is missing");

        RefreshToken rt = refreshTokenService.getRefreshToken(request.getRefreshToken());

        if (rt == null || refreshTokenService.isExpired(rt)) {

            throw new InvalidTokenException("Refresh Token expired or missing");
        }

        User owner = refreshTokenService.getOwner(rt);
        if (owner == null || !owner.getUserId().equals(request.getUserId())) {

            throw new InvalidTokenException("Refresh Token does not belong to user");
        }

        refreshTokenService.invalidateRefreshToken(rt);
        log.info("User {} logged out successfully", request.getUserId());
    }
}

