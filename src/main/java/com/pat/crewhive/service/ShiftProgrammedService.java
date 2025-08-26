package com.pat.crewhive.service;


import com.pat.crewhive.dto.shift.shift_programmed.CreateShiftProgrammedDTO;
import com.pat.crewhive.dto.shift.shift_programmed.PatchShiftProgrammedDTO;
import com.pat.crewhive.model.shift.shiftprogrammed.entity.ShiftProgrammed;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.model.util.Period;
import com.pat.crewhive.repository.ShiftProgrammedRepository;
import com.pat.crewhive.repository.ShiftUserRepository;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.service.utils.DateUtils;
import com.pat.crewhive.service.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ShiftProgrammedService {

    private final ShiftProgrammedRepository shiftProgrammedRepository;
    private final ShiftUserRepository shiftUserRepository;
    private final StringUtils stringUtils;
    private final UserService userService;
    private final DateUtils dateUtils;

    public ShiftProgrammedService(ShiftProgrammedRepository shiftProgrammedRepository,
                                    ShiftUserRepository shiftUserRepository,
                                  StringUtils stringUtils,
                                  UserService userService,
                                  DateUtils dateUtils) {
        this.shiftProgrammedRepository = shiftProgrammedRepository;
        this.shiftUserRepository = shiftUserRepository;
        this.stringUtils = stringUtils;
        this.userService = userService;
        this.dateUtils = dateUtils;
    }


    /**
     * Create a new programmed shift and associate it with users.
     * @param createShiftProgrammedDTO The DTO containing shift details.
     * @return The ID of the created shift.
     * @throws IllegalArgumentException if the start time is after the end time.
     */
    @Transactional
    public Long createShift(CreateShiftProgrammedDTO createShiftProgrammedDTO) {

        log.info("Creating shift with name: {}", createShiftProgrammedDTO.getName());

        String normalizedShiftName = stringUtils.normalizeString(createShiftProgrammedDTO.getName());
        createShiftProgrammedDTO.setName(normalizedShiftName);

        if (createShiftProgrammedDTO.getStart().isAfter(createShiftProgrammedDTO.getEnd())) {
            throw new IllegalArgumentException("Shift start time cannot be after end time");
        }

        List<User> users = userService.getUsersByIds(createShiftProgrammedDTO.getUserId());

        ShiftProgrammed shiftProgrammed = new ShiftProgrammed();
        shiftProgrammed.setShiftName(createShiftProgrammedDTO.getName());
        shiftProgrammed.setDescription(createShiftProgrammedDTO.getDescription());
        shiftProgrammed.setStart(createShiftProgrammedDTO.getStart());
        shiftProgrammed.setEnd(createShiftProgrammedDTO.getEnd());
        shiftProgrammed.setColor(createShiftProgrammedDTO.getColor());

        for (User u: users) {
            shiftProgrammed.addUser(u);
        }

        ShiftProgrammed savedShift = shiftProgrammedRepository.save(shiftProgrammed);

        return savedShift.getShiftProgrammedId();
    }


    /**
     * Retrieve shifts for a specific user within a defined period.
     * @param period The period to filter shifts (DAY, WEEK, MONTH, TRIMESTER, SEMESTER, YEAR).
     * @param userId The ID of the user whose shifts are to be retrieved.
     * @return A list of ShiftProgrammed entities matching the criteria.
     */
    @Transactional(readOnly = true)
    public List<ShiftProgrammed> getShiftByPeriodAndUser(Period period, Long userId) {

        log.info("Fetching shifts for user ID: {}", userId);

        LocalDate from = dateUtils.getStartDateForPeriod(period);
        LocalDate to = dateUtils.getEndDateForPeriod(period);

        return shiftProgrammedRepository.findByUserAndDateBetween(userId, from, to);
    }


    /**
     * Retrieve shifts for a specific company within a defined period.
     * @param period The period to filter shifts (DAY, WEEK, MONTH, TRIMESTER, SEMESTER, YEAR).
     * @param companyId The ID of the company whose shifts are to be retrieved.
     * @return A list of ShiftProgrammed entities matching the criteria.
     */
    @Transactional(readOnly = true)
    public List<ShiftProgrammed> getShiftByPeriodAndCompany(Period period, Long companyId) {

        log.info("Fetching shifts for company ID: {}", companyId);

        LocalDate today = LocalDate.now();
        LocalDate from;
        LocalDate to;

        switch (period) {
            case DAY -> {
                from = today;
                to = today;
            }
            case WEEK -> {
                from = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                to = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            }
            case MONTH -> {
                from = today.with(TemporalAdjusters.firstDayOfMonth());
                to = today.with(TemporalAdjusters.lastDayOfMonth());
            }
            case TRIMESTER -> {
                int q = ((today.getMonthValue() - 1) / 3) + 1;
                int startMonth = (q - 1) * 3 + 1;
                from = LocalDate.of(today.getYear(), startMonth, 1);
                to = from.plusMonths(3).minusDays(1);
            }
            case SEMESTER -> {
                int startMonth = (today.getMonthValue() <= 6) ? 1 : 7;
                from = LocalDate.of(today.getYear(), startMonth, 1);
                to = from.plusMonths(6).minusDays(1);
            }
            case YEAR -> {
                from = LocalDate.of(today.getYear(), 1, 1);
                to = LocalDate.of(today.getYear(), 12, 31);
            }
            default -> {
                log.warn("Unknown EventTemp: {}. Returning empty list.", period);
                return List.of();
            }
        }

        return shiftProgrammedRepository.findByCompanyAndDateBetween(companyId, from, to);

    }


    /**
     * Retrieve all users assigned to a specific shift.
     * @param shiftId The ID of the shift.
     * @return A list of User entities assigned to the shift.
     * @throws ResourceNotFoundException if the shift does not exist.
     */
    @Transactional(readOnly = true)
    public List<User> getUsersInShift(Long shiftId) {

        log.info("Fetching users in shift with id: {}", shiftId);

        if (!shiftProgrammedRepository.existsById(shiftId)) {

            log.error("Shift with id {} does not exist", shiftId);
            throw new ResourceNotFoundException("Shift not found with ID: " + shiftId);
        }

        return shiftUserRepository.findUsersByShiftId(shiftId);
    }


    /**
     * Update an existing programmed shift with new details.
     * @param dto The DTO containing updated shift details.
     * @return The ID of the updated shift.
     * @throws ResourceNotFoundException if the shift does not exist.
     * @throws IllegalArgumentException if the start time is after the end time.
     */
    @Transactional
    public Long patchShift(PatchShiftProgrammedDTO dto) {
        log.info("Patching shift with id: {}", dto.getShiftProgrammedId());

        ShiftProgrammed shift = shiftProgrammedRepository.findByIdWithWorkers(dto.getShiftProgrammedId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with ID: " + dto.getShiftProgrammedId()));


        shift.setShiftName(stringUtils.normalizeString(dto.getName()));

        if (dto.getStart().isAfter(dto.getEnd())) {
            throw new IllegalArgumentException("Shift start time cannot be after end time");
        }

        shift.setStart(dto.getStart());
        shift.setEnd(dto.getEnd());
        shift.setDescription(dto.getDescription());
        shift.setColor(dto.getColor());


        if (dto.getUserId() != null) {

            Set<Long> newIds = dto.getUserId();

            Set<Long> current = shift.getUsers().stream()
                    .map(su -> su.getUser().getUserId())
                    .collect(Collectors.toSet());

            // Rimuovi quelli non più presenti
            if (!current.isEmpty()) {
                Set<Long> toRemove = new HashSet<>(current);
                toRemove.removeAll(newIds);
                if (!toRemove.isEmpty()) {
                    userService.getUsersByIds(toRemove).forEach(shift::removeUser);
                }
            }
            // Aggiungi i nuovi
            if (!newIds.isEmpty()) {
                Set<Long> toAdd = new HashSet<>(newIds);
                toAdd.removeAll(current);
                if (!toAdd.isEmpty()) {
                    userService.getUsersByIds(toAdd).forEach(shift::addUser);
                }
            } else {

                new HashSet<>(shift.getUsers())
                        .forEach(su -> shift.removeUser(su.getUser()));
            }
        }

        return shift.getShiftProgrammedId();
    }


    /**
     * Delete a programmed shift by its ID.
     * @param shiftId The ID of the shift to delete.
     * @throws ResourceNotFoundException if the shift does not exist.
     */
    @Transactional
    public void deleteShift(Long shiftId) {

        log.info("Deleting shift with id: {}", shiftId);

        if (!shiftProgrammedRepository.existsById(shiftId)) {

            log.error("Shift with id {} does not exist", shiftId);
            throw new ResourceNotFoundException("Shift not found with ID: " + shiftId);
        }

        shiftUserRepository.deleteByShiftId(shiftId);
        shiftProgrammedRepository.deleteById(shiftId);
    }
}
