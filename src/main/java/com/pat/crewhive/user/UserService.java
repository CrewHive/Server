package com.pat.crewhive.user;

import com.pat.crewhive.authuser.AuthResponseDTO;
import com.pat.crewhive.common.audit.SoftDeleteSupport;
import com.pat.crewhive.manager.RoleAssignmentService;
import com.pat.crewhive.manager.UpdateUserWorkInfoDTO;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.shiftprogrammed.ShiftUserRepository;
import com.pat.crewhive.authuser.RefreshTokenService;
import com.pat.crewhive.security.JwtService;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.common.PasswordUtil;
import com.pat.crewhive.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final RefreshTokenService refreshTokenService;
    private final ShiftUserRepository shiftUserRepository;
    private final UserRepository userRepository;
    private final PasswordUtil passwordUtil;
    private final StringUtils stringUtils;
    private final JwtService jwtService;
    private final RoleAssignmentService roleAssignmentService;

    public UserService(UserRepository userRepository,
                       ShiftUserRepository shiftUserRepository,
                       PasswordUtil passwordUtil,
                       StringUtils stringUtils,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService,
                       RoleAssignmentService roleAssignmentService) {
        this.userRepository = userRepository;
        this.shiftUserRepository = shiftUserRepository;
        this.passwordUtil = passwordUtil;
        this.refreshTokenService = refreshTokenService;
        this.stringUtils = stringUtils;
        this.jwtService = jwtService;
        this.roleAssignmentService = roleAssignmentService;
    }


    /**
     * Updates the user information in the database.
     *
     * @param user the User object containing updated information
     */
    @Transactional
    public void updateUser(User user) {

        log.info("User {} updated successfully", user.getEmail());

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
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }


    /**
     * Retrieves a list of Users by their IDs.
     *
     * @param ids a set of user IDs to retrieve
     * @return a list of User objects corresponding to the provided IDs
     * @throws ResourceNotFoundException if any of the users are not found
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByIds(Set<UUID> ids) {

        if (ids == null || ids.isEmpty()) {

            log.info("Retrieving users: empty id set -> returning empty list");
            return List.of();
        }

        log.info("Retrieving users {}", ids);

        List<User> users = userRepository.findAllByIds(ids);
        if (users.size() != ids.size()) {
            // calcola gli ID mancanti per un messaggio più utile
            Set<UUID> foundIds = users.stream()
                    .map(User::getUserId)
                    .collect(Collectors.toSet());

            Set<UUID> missing = new HashSet<>(ids);
            missing.removeAll(foundIds);
            //TODO: L'ID dev'essere lasciato solo nel log
            throw new ResourceNotFoundException("Users not found: " + missing);
        }

        return users;
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

        email = stringUtils.normalizeString(email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }


    /**
     * Retrieves user details along with time parameters by username.
     *
     * @param userId the ID of the user
     * @return a UserWithTimesParamDTO containing user details and time parameters
     * @throws ResourceNotFoundException if the user is not found
     */
    @Transactional(readOnly = true)
    public UserWithTimeParamsDTO getUserWithTimeParamsByUsername(UUID userId) {

        User user = getUserById(userId);

        log.info("User details retrieved for user: {}", user.getEmail());

        String companyName = (user.getCompany() != null) ? user.getCompany().getName() : null;

        return new UserWithTimeParamsDTO(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                companyName,
                user.getContractType(),
                user.getWorkableHoursPerWeek(),
                user.getOvertimeHours(),
                user.getVacationDaysAccumulated(),
                user.getVacationDaysTaken(),
                user.getLeaveDaysAccumulated(),
                user.getLeaveDaysTaken()
        );
    }


    /**
     * Retrieves all users belonging to a specific company.
     *
     * @param companyId the ID of the company
     * @return a list of User objects belonging to the specified company
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsersInCompany(UUID companyId) {

        log.info("Retrieving all users in company with ID: {}", companyId);

        return userRepository.findAllByCompany_CompanyId(companyId);
    }


    /**
     * Updates the user's password.
     *
     * @param newPassword the new password to set
     * @param oldPassword the current password of the user
     * @param email the email of the user
     */
    @Transactional
    public void updatePassword(String newPassword, String oldPassword, String email) {

        email = stringUtils.normalizeString(email);

        User user = getUserByEmail(email);

        if(!passwordUtil.isStrong(newPassword)) {

            log.info("New password is not strong enough for user: {}", email);

            throw new BadCredentialsException("Invalid password");
        }

        if(passwordUtil.NotMatches(oldPassword, user.getPassword())) {

            log.info("Old password does not match for user: {}", email);

            throw new BadCredentialsException("Old password does not match");
        }

        user.setPassword(passwordUtil.encodePassword(newPassword));

        userRepository.save(user);
        log.info("Updated password for user: {}", email);
    }


    /**
     * Updates the time-related parameters of a user.
     *
     * @param dto       the UpdateUserWorkInfoDTO containing new time parameters
     * @param companyId the ID of the company to which the user belongs
     * @throws ResourceNotFoundException if the user is not found in the specified company
     */
    @Transactional
    public void updateUserTimeParams(UpdateUserWorkInfoDTO dto, UUID companyId) {

        User user = getUserById(dto.targetUserId());

        if (!user.getCompany().getCompanyId().equals(companyId)) throw new ResourceNotFoundException("User not found in the specified company");

        user.setContractType(dto.contractType());
        user.setWorkableHoursPerWeek(dto.workableHoursPerWeek());
        user.setOvertimeHours(dto.overtimeHours());
        user.setVacationDaysAccumulated(dto.vacationDaysAccumulated());
        user.setVacationDaysTaken(dto.vacationDaysTaken());
        user.setLeaveDaysAccumulated(dto.leaveDaysAccumulated());
        user.setLeaveDaysTaken(dto.leaveDaysTaken());

        userRepository.save(user);

        log.info("Updated time parameters for user: {}", user.getEmail());
    }


    /**
     * Allows a user to leave their current company.
     *
     * @param userId the ID of the user who wants to leave the company
     * @throws ResourceAlreadyExistsException if the user is not part of any company
     */
    @Transactional
    public AuthResponseDTO leaveCompany(UUID userId) {

        User user = getUserById(userId);

        if (user.getCompany() == null) throw new ResourceNotFoundException("User has no company");

        Company c = user.getCompany();
        c.getUsers().remove(user);
        user.setCompany(null);
        roleAssignmentService.resetToBaseRole(user);

        shiftUserRepository.deleteByUserId(userId, OffsetDateTime.now());

        updateUser(user);

        return new AuthResponseDTO(
                jwtService.generateToken(
                        user.getUserId(),
                        stringUtils.normalizeString(user.getEmail()),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getRoles().stream().map(r -> r.getRole().getRoleName()).collect(Collectors.toSet()),
                        null),
                refreshTokenService.getOrIssueRefreshToken(user));
    }


    /**
     * Deactivates a user account (soft-delete).
     * <p>
     * The row is not physically removed: historical records tied to the user
     * (e.g. worked shifts) must keep their reference for payroll/audit purposes.
     * The refresh token is revoked so the account can no longer obtain new access
     * tokens, and the account is rejected at login while deactivated.
     *
     * @param userId the ID of the user to deactivate
     */
    @Transactional
    public void deleteAccount(UUID userId) {

        User user = getUserById(userId);

        roleAssignmentService.resetToBaseRole(user);

        refreshTokenService.deleteTokenByUser(user);

        SoftDeleteSupport.softDelete(userRepository, user, user);

        log.info("Deactivated account for user: {}", userId);
    }


}
