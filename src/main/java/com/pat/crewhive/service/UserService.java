package com.pat.crewhive.service;

import com.pat.crewhive.dto.response.auth.AuthResponseDTO;
import com.pat.crewhive.dto.request.manager.UpdateUserWorkInfoDTO;
import com.pat.crewhive.dto.response.user.UserWithTimeParamsDTO;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.ShiftUserRepository;
import com.pat.crewhive.repository.UserRepository;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.service.utils.PasswordUtil;
import com.pat.crewhive.service.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserService {

    private final RefreshTokenService refreshTokenService;
    private final ShiftUserRepository shiftUserRepository;
    private final UserRepository userRepository;
    private final PasswordUtil passwordUtil;
    private final StringUtils stringUtils;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       ShiftUserRepository shiftUserRepository,
                       PasswordUtil passwordUtil,
                       StringUtils stringUtils,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.shiftUserRepository = shiftUserRepository;
        this.passwordUtil = passwordUtil;
        this.refreshTokenService = refreshTokenService;
        this.stringUtils = stringUtils;
        this.jwtService = jwtService;
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
    public User getUserById(Long id) {
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
    public List<User> getUsersByIds(Set<Long> ids) {

        if (ids == null || ids.isEmpty()) {

            log.info("Retrieving users: empty id set -> returning empty list");
            return List.of();
        }

        log.info("Retrieving users {}", ids);

        List<User> users = userRepository.findAllByIds(ids);
        if (users.size() != ids.size()) {
            // calcola gli ID mancanti per un messaggio più utile
            Set<Long> foundIds = users.stream()
                    .map(User::getUserId)
                    .collect(Collectors.toSet());

            Set<Long> missing = new HashSet<>(ids);
            missing.removeAll(foundIds);
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
    public UserWithTimeParamsDTO getUserWithTimeParamsByUsername(Long userId) {

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
    public List<User> getAllUsersInCompany(Long companyId) {

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

        if(!passwordUtil.matches(oldPassword, user.getPassword())) {

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
    public void updateUserTimeParams(UpdateUserWorkInfoDTO dto, Long companyId) {

        User user = getUserById(dto.getTargetUserId());

        if (!user.getCompany().getCompanyId().equals(companyId)) throw new ResourceNotFoundException("User not found in the specified company");

        user.setContractType(dto.getContractType());
        user.setWorkableHoursPerWeek(dto.getWorkableHoursPerWeek());
        user.setOvertimeHours(dto.getOvertimeHours());
        user.setVacationDaysAccumulated(dto.getVacationDaysAccumulated());
        user.setVacationDaysTaken(dto.getVacationDaysTaken());
        user.setLeaveDaysAccumulated(dto.getLeaveDaysAccumulated());
        user.setLeaveDaysTaken(dto.getLeaveDaysTaken());

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
    public AuthResponseDTO leaveCompany(Long userId) {

        User user = getUserById(userId);

        if (user.getCompany() == null) throw new ResourceNotFoundException("User has no company");

        Company c = user.getCompany();
        c.getUsers().remove(user);
        user.setCompany(null);

        shiftUserRepository.deleteByUserId(userId);

        updateUser(user);

        //todo rimuovi una volta implementato il rotate token nel frontend
        refreshTokenService.deleteTokenByUser(user);
        String rt = refreshTokenService.generateRefreshToken(user);

        return new AuthResponseDTO(
                jwtService.generateToken(
                        user.getUserId(),
                        stringUtils.normalizeString(user.getEmail()),
                        user.getFirstName(),
                        user.getLastName(),
                        "ROLE_USER",
                        null),
                rt);
    }


    /**
     * Deletes a user account.
     *
     * @param userId the username of the user to delete
     */
    @Transactional
    public void deleteAccount(Long userId) {

        User user = getUserById(userId);

        refreshTokenService.deleteTokenByUser(user);

        userRepository.delete(user);

        log.info("Deleted account for user: {}", userId);
    }


}
