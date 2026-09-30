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
import com.pat.crewhive.common.PasswordUtil;
import com.pat.crewhive.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

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
    private final PendingRegistrationService pendingRegistrationService;
    private final MailService mailService;

    public AuthService(JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       UserRepository userRepository,
                       RoleService roleService,
                       PasswordUtil passwordUtil,
                       EmailUtil emailUtil,
                       StringUtils stringUtils,
                       TokenBlackListService tokenBlackListService,
                       PendingRegistrationService pendingRegistrationService,
                       MailService mailService) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userRepository = userRepository;
        this.roleService = roleService;
        this.passwordUtil = passwordUtil;
        this.emailUtil = emailUtil;
        this.stringUtils = stringUtils;
        this.tokenBlackListService = tokenBlackListService;
        this.pendingRegistrationService = pendingRegistrationService;
        this.mailService = mailService;
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

        // @SQLRestriction su User nasconde gli account disattivati a findByEmail. Per non rivelare
        // se l'account esiste (M3) i tre casi "inesistente", "disattivato" e "password errata"
        // escono con la stessa eccezione e, grazie a burnMatch, con lo stesso costo di bcrypt.
        // Solo il log interno li distingue. Per un account disattivato la password non viene
        // controllata: e' una conseguenza del filtro automatico, non una scelta di questo metodo.
        Optional<User> found = userRepository.findByEmail(normalizedEmail);

        if (found.isEmpty()) {
            passwordUtil.burnMatch(request.password());

            if (userRepository.existsInactiveByEmail(normalizedEmail)) {
                log.error("Login attempt for deactivated account: {}", normalizedEmail);
            } else {
                log.error("Login attempt for unknown account: {}", normalizedEmail);
            }
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = found.get();

        if (passwordUtil.NotMatches(request.password(), user.getPassword())) {
            log.error("Invalid password for user: {}", normalizedEmail);

            throw new BadCredentialsException("Invalid credentials");
        }

        log.info("User {} authenticated successfully", normalizedEmail);

        UUID company = user.getCompany() == null ? null : user.getCompany().getCompanyId();

        return new AuthResponseDTO(
                jwtService.generateToken(
                        user.getUserId(),
                        normalizedEmail,
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEffectiveRoleNames(),
                        company
                ),
                refreshTokenService.issueNewFamily(user)
        );
    }

    /**
     * Starts the registration of a new user. The response to the caller is the same whether the
     * email is new or already registered (M3): in both cases an email is sent, and the user is
     * created only when the link it contains is confirmed (see {@link #verifyEmail(String)}).
     *
     * @param request The registration request containing email, names and password.
     * @throws BadCredentialsException if the email format is invalid or the password is weak.
     */
    public void register(RegistrationDTO request) {

        String normalizedEmail = stringUtils.normalizeString(request.email());
        if (!emailUtil.isValidEmail(normalizedEmail)) {

            log.error("Invalid email format: {}", normalizedEmail);
            throw new BadCredentialsException("Invalid email format");
        }

        if (!passwordUtil.isStrong(request.password())) {

            log.error("Weak password provided for user: {}", normalizedEmail);
            throw new BadCredentialsException("Weak password provided");
        }

        // Calcolato anche se l'email esiste, per non differenziare i tempi dei due rami.
        String encodedPassword = passwordUtil.encodePassword(request.password());

        if (emailTaken(normalizedEmail)) {

            log.info("Registration requested for an already registered email: {}", normalizedEmail);
            mailService.sendAccountAlreadyExists(normalizedEmail);
            return;
        }

        String token = pendingRegistrationService.create(
                normalizedEmail, request.firstName(), request.lastName(), encodedPassword);
        mailService.sendVerification(normalizedEmail, token);

        log.info("Registration pending confirmation: {}", normalizedEmail);
    }

    /**
     * Completes a registration: consumes the single-use token received by email and creates the user.
     *
     * @param token The token contained in the verification link.
     * @throws InvalidTokenException if the token is unknown, expired, already used, or the email
     *                               has been registered in the meantime.
     */
    @Transactional
    public void verifyEmail(String token) {

        PendingRegistrationService.PendingRegistration pending = pendingRegistrationService.consume(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (emailTaken(pending.email())) {
            log.error("Verification for an email registered in the meantime: {}", pending.email());
            throw new InvalidTokenException("Invalid verification token");
        }

        User newUser = new User(pending.email(), pending.firstName(), pending.lastName(), pending.encodedPassword());
        newUser.addRole(roleService.getOrCreateGlobalRoleUser());

        try {
            userRepository.saveAndFlush(newUser);
        } catch (DataIntegrityViolationException e) {
            // Race con un'altra conferma per la stessa email: il vincolo unique ha vinto l'altra.
            log.error("Concurrent verification for the same email: {}", pending.email());
            throw new InvalidTokenException("Invalid verification token");
        }

        log.info("User registered successfully: userId={}", newUser.getUserId());
    }

    /**
     * True if the email belongs to an active or a deactivated account (the unique constraint on
     * {@code email} covers both, while {@code existsByEmail} sees only the active ones).
     */
    private boolean emailTaken(String normalizedEmail) {
        return userRepository.existsByEmail(normalizedEmail) || userRepository.existsInactiveByEmail(normalizedEmail);
    }

    /**
     * Rotates the refresh token for a user.
     *
     * @param token The request containing the string of the Refresh Token.
     * @return AuthResponseDTO containing new JWT and Refresh Token.
     * @throws InvalidTokenException if the refresh token is invalid or expired.
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public AuthResponseDTO rotate_token(String token) {

        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        try { UUID.fromString(token); }
        catch (IllegalArgumentException e) { throw new InvalidTokenException("Invalid refresh token"); }

        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(token);

        User owner = rotation.user();
        if (owner == null || owner.getUserId() == null) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        UUID userId = owner.getUserId();
        String normalizedEmail = stringUtils.normalizeString(owner.getEmail());
        String firstName = owner.getFirstName();
        String lastName = owner.getLastName();
        Set<String> roles = owner.getEffectiveRoleNames();
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

        log.info("Refresh rotated for userId={}", userId);
        return new AuthResponseDTO(newAccessToken, rotation.refreshToken());
    }

    /**
     * Logs out a user by revoking the whole family of their refresh token.
     *
     * @param request The logout request containing the refresh token and username.
     * @throws InvalidTokenException if the refresh token is invalid or does not belong to the user.
     */
    @Transactional
    public void logout(LogoutDTO request, String jti, Date tokenExpiration) {

        if (request.refreshToken() == null || request.refreshToken().isBlank()) throw new InvalidTokenException("Refresh Token is missing");

        RefreshToken rt = refreshTokenService.getValidToken(request.refreshToken());

        User owner = rt.getUser();
        if (owner == null || !owner.getUserId().equals(request.userId())) {

            throw new InvalidTokenException("Refresh Token does not belong to user");
        }

        refreshTokenService.revokeFamily(rt);
        tokenBlackListService.revoke(jti, tokenExpiration);

        log.info("User {} logged out successfully", request.userId());
    }
}

