package com.pat.crewhive.authuser;


import com.pat.crewhive.security.TokenBlackListService;
import com.pat.crewhive.user.LogoutDTO;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.user.User;
import com.pat.crewhive.manager.Role;
import com.pat.crewhive.manager.UserRole;
import com.pat.crewhive.user.UserRepository;
import com.pat.crewhive.security.JwtService;
import com.pat.crewhive.manager.RoleService;
import com.pat.crewhive.security.exception.custom.InvalidTokenException;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.common.PasswordUtil;
import com.pat.crewhive.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RoleService roleService;
    private final PasswordUtil passwordUtil;
    private final EmailUtil emailUtil;
    private final StringUtils stringUtils;
    private final TokenBlackListService tokenBlackListService;

    public AuthService(JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       UserRepository userRepository,
                       RoleService roleService,
                       PasswordUtil passwordUtil,
                       EmailUtil emailUtil,
                       StringUtils stringUtils,
                       TokenBlackListService tokenBlackListService) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordUtil = passwordUtil;
        this.emailUtil = emailUtil;
        this.stringUtils = stringUtils;
        this.tokenBlackListService = tokenBlackListService;
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

        String normalizedEmail = stringUtils.normalizeString(request.email());

        // @SQLRestriction su User nasconde gli account disattivati a findByEmail: se
        // la riga non c'è, verifichiamo separatamente se esiste ma è inattiva, per dare
        // un messaggio distinto. Nota: questo significa che per un account disattivato
        // la password non viene nemmeno controllata - è una conseguenza accettata del
        // filtro automatico, non una scelta di questo metodo.
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> {
                    if (userRepository.existsInactiveByEmail(normalizedEmail)) {
                        log.error("Login attempt for deactivated account: {}", normalizedEmail);
                        throw new BadCredentialsException("Account disabled");
                    }
                    throw new ResourceNotFoundException("User not found");
                });

        if (passwordUtil.NotMatches(request.password(), user.getPassword())) {
            log.error("Invalid password for user: {}", normalizedEmail);

            throw new BadCredentialsException("Invalid credentials");
        }

        log.info("User {} authenticated successfully", normalizedEmail);

        RefreshToken rt = refreshTokenService.getRefreshTokenByUser(user);

        if (rt != null) {

            refreshTokenService.invalidateRefreshToken(rt);
        }

        UUID company = user.getCompany() == null ? null : user.getCompany().getCompanyId();

        return new AuthResponseDTO(
                jwtService.generateToken(
                        user.getUserId(),
                        normalizedEmail,
                        user.getFirstName(),
                        user.getLastName(),
                        user.getRoles().stream().map(r -> r.getRole().getRoleName()).collect(Collectors.toSet()),
                        company
                ),
                refreshTokenService.generateRefreshToken(user)
        );
    }

    /**
     * Registers a new user with the provided details.
     *
     * @param request The registration request containing username, email, and password.
     * @throws BadCredentialsException if the email format is invalid or the password is weak.
     * @throws ResourceAlreadyExistsException if the username or email already exist.
     */
    @Transactional
    public void register(RegistrationDTO request) {

        String normalizedEmail = stringUtils.normalizeString(request.email());
        if (!emailUtil.isValidEmail(normalizedEmail)) {

            log.error("Invalid email format: {}", normalizedEmail);
            throw new BadCredentialsException("Invalid email format");
        }

        if (userRepository.existsByEmail(normalizedEmail)) {

            log.error("Email already registered: {}", normalizedEmail);
            throw new ResourceAlreadyExistsException("Email already registered");
        }

        if (!passwordUtil.isStrong(request.password())) {

            log.error("Weak password provided for user: {}", normalizedEmail);
            throw new BadCredentialsException("Weak password provided");
        }

        String encodedPassword = passwordUtil.encodePassword(request.password());

        User newUser = new User(normalizedEmail, request.firstName(), request.lastName(), encodedPassword);

        Role role = roleService.getOrCreateGlobalRoleUser();

        newUser.addRole(role);

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

        UUID userId = owner.getUserId();
        String normalizedEmail = stringUtils.normalizeString(owner.getEmail());
        String firstName = owner.getFirstName();
        String lastName = owner.getLastName();
        Set<String> roles = owner.getRoles().stream().map(r -> r.getRole().getRoleName()).collect(Collectors.toSet());
        Company company = owner.getCompany();
        UUID companyId = (company != null) ? company.getCompanyId() : null;


        String newAccessToken = jwtService.generateToken(
                userId,
                normalizedEmail,
                firstName,
                lastName,
                roles,
                companyId
        );

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
    public void logout(LogoutDTO request, String jti, Date tokenExpiration) {

        if (request.refreshToken() == null || request.refreshToken().isBlank()) throw new InvalidTokenException("Refresh Token is missing");

        RefreshToken rt = refreshTokenService.getRefreshToken(request.refreshToken());

        if (rt == null || refreshTokenService.isExpired(rt)) {

            throw new InvalidTokenException("Refresh Token expired or missing");
        }

        User owner = refreshTokenService.getOwner(rt);
        if (owner == null || !owner.getUserId().equals(request.userId())) {

            throw new InvalidTokenException("Refresh Token does not belong to user");
        }

        refreshTokenService.invalidateRefreshToken(rt);
        tokenBlackListService.revoke(jti, tokenExpiration);

        log.info("User {} logged out successfully", request.userId());
    }
}

