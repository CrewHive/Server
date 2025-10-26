package com.pat.crewhive.service;

import com.pat.crewhive.dto.request.shift.worked.CreateShiftWorkedDTO;
import com.pat.crewhive.model.shift.shiftworked.entity.ShiftWorked;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.repository.ShiftWorkedRepository;
import com.pat.crewhive.service.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
public class ShiftWorkedService {

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

        log.info("Creating ShiftWorked {} for user {}", dto.getShiftName(), dto.getUserId());

        User user = userService.getUserById(dto.getUserId());

        String normalizedShiftName = stringUtils.normalizeString(dto.getShiftName());

        ShiftWorked sw = new ShiftWorked(
                normalizedShiftName,
                dto.getStart(),
                dto.getEnd(),
                dto.getBreakTime(),
                dto.getExtraHours(),
                user
        );

        BigDecimal oldOvertime = user.getOvertimeHours();
        BigDecimal newOvertime = oldOvertime.add(dto.getExtraHours());
        user.setOvertimeHours(newOvertime);

        userService.updateUser(user);

        repo.save(sw);
    }


}
