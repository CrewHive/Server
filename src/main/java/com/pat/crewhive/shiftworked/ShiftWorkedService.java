package com.pat.crewhive.shiftworked;

import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserService;
import com.pat.crewhive.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

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
     * Creates a new ShiftWorked entry in the database.
     *
     * @param dto Data transfer object containing shift details.
     */
    @Transactional
    public void createShiftWorked(CreateShiftWorkedDTO dto) {

        log.info("Creating ShiftWorked {} for user {}", dto.shiftName(), dto.userId());

        User user = userService.getUserById(dto.userId());

        String normalizedShiftName = stringUtils.normalizeString(dto.shiftName());

        ShiftWorked sw = new ShiftWorked(
                normalizedShiftName,
                dto.start(),
                dto.end(),
                dto.breakTime(),
                dto.extraHours(),
                user
        );

        BigDecimal oldOvertime = user.getOvertimeHours();
        BigDecimal newOvertime = oldOvertime.add(dto.extraHours());
        user.setOvertimeHours(newOvertime);

        userService.updateUser(user);

        repo.save(sw);
    }


}
