package com.pat.crewhive.shiftworked;

import com.pat.crewhive.company.Company;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserService;
import com.pat.crewhive.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class ShiftWorkedService {

    private static final Logger log = LoggerFactory.getLogger(ShiftWorkedService.class);

    private final ShiftWorkedRepository repo;
    private final StringUtils stringUtils;
    private final UserService userService;

    public ShiftWorkedService(ShiftWorkedRepository repo,
                              StringUtils stringUtils,
                              UserService userService) {
        this.repo = repo;
        this.stringUtils = stringUtils;
        this.userService = userService;
    }


    /**
     * Creates a new ShiftWorked entry for the given user (the authenticated caller).
     *
     * @param dto    Data transfer object containing shift details.
     * @param userId ID of the user the shift belongs to, taken from the JWT (self-only).
     */
    @Transactional
    public void createShiftWorked(CreateShiftWorkedDTO dto, UUID userId) {

        log.info("Creating ShiftWorked {} for user {}", dto.shiftName(), userId);

        User user = userService.getUserById(userId);

        Company company = user.getCompany();
        if (company == null) {
            throw new ResourceNotFoundException("User has no company");
        }

        String normalizedShiftName = stringUtils.normalizeString(dto.shiftName());

        ShiftWorked sw = new ShiftWorked(
                normalizedShiftName,
                dto.start(),
                dto.end(),
                dto.breakTime(),
                dto.extraHours(),
                user,
                company
        );

        BigDecimal oldOvertime = user.getOvertimeHours();
        BigDecimal newOvertime = oldOvertime.add(dto.extraHours());
        user.setOvertimeHours(newOvertime);

        userService.updateUser(user);

        repo.save(sw);
    }


}
