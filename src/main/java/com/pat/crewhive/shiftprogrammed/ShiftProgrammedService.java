package com.pat.crewhive.shiftprogrammed;


import com.pat.crewhive.user.User;
import com.pat.crewhive.common.Period;
import com.pat.crewhive.user.UserService;
import com.pat.crewhive.company.CompanyService;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.common.DateUtils;
import com.pat.crewhive.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShiftProgrammedService {

    private static final Logger log = LoggerFactory.getLogger(ShiftProgrammedService.class);

    private final ShiftProgrammedRepository shiftProgrammedRepository;
    private final ShiftUserRepository shiftUserRepository;
    private final StringUtils stringUtils;
    private final UserService userService;
    private final DateUtils dateUtils;
    private final CompanyService companyService;

    public ShiftProgrammedService(ShiftProgrammedRepository shiftProgrammedRepository,
                                  ShiftUserRepository shiftUserRepository,
                                  StringUtils stringUtils,
                                  UserService userService,
                                  DateUtils dateUtils,
                                  CompanyService companyService) {
        this.shiftProgrammedRepository = shiftProgrammedRepository;
        this.shiftUserRepository = shiftUserRepository;
        this.stringUtils = stringUtils;
        this.userService = userService;
        this.dateUtils = dateUtils;
        this.companyService = companyService;
    }


    /**
     * Create a new programmed shift and associate it with users.
     * @param creatorUserId the ID of the user who created the shift
     * @param dto The DTO containing shift details.
     * @return The ID of the created shift.
     * @throws IllegalArgumentException if the start time is after the end time.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "shiftsByUser",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByUser(#creatorUserId, T(com.pat.crewhive.common.Period).DAY)"),
            @CacheEvict(value = "shiftsByUser",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByUser(#creatorUserId, T(com.pat.crewhive.common.Period).WEEK)"),
            @CacheEvict(value = "shiftsByUser",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByUser(#creatorUserId, T(com.pat.crewhive.common.Period).MONTH)"),

            @CacheEvict(value = "shiftsByCompany",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByCompany(#creatorUserId, T(com.pat.crewhive.common.Period).DAY)"),
            @CacheEvict(value = "shiftsByCompany",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByCompany(#creatorUserId, T(com.pat.crewhive.common.Period).WEEK)"),
            @CacheEvict(value = "shiftsByCompany",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByCompany(#creatorUserId, T(com.pat.crewhive.common.Period).MONTH)")
    })
    public UUID createShift(
            UUID creatorUserId,
            CreateShiftProgrammedDTO dto) {

        log.info("Creating shift with name: {}", dto.name());

        String normalizedShiftName = stringUtils.normalizeString(dto.name());

        if (dto.start().isAfter(dto.end())) {
            throw new IllegalArgumentException("Shift start time cannot be after end time");
        }

        List<User> users = userService.getUsersByIds(dto.userId());

        ShiftProgrammed shiftProgrammed = new ShiftProgrammed();
        shiftProgrammed.setShiftName(normalizedShiftName);
        shiftProgrammed.setDescription(dto.description());
        shiftProgrammed.setStart(dto.start());
        shiftProgrammed.setEnd(dto.end());
        shiftProgrammed.setColor(dto.color());

        for (User u: users) {
            shiftProgrammed.addUser(u);
        }

        ShiftProgrammed savedShift = shiftProgrammedRepository.save(shiftProgrammed);

        return savedShift.getShiftProgrammedId();
    }


    //todo refactor function after the exam to avoid that horrible list and dtos
    /**
     * Retrieve shifts for a specific user within a defined period.
     * @param period The period to filter shifts (DAY, WEEK, MONTH, TRIMESTER, SEMESTER, YEAR).
     * @param userId The ID of the user whose shifts are to be retrieved.
     * @return A list of ShiftProgrammed entities matching the criteria.
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = "shiftsByUser",
            key = "#userId + ':' + #period"
    )
    public ShiftProgrammedOutputDTO getShiftsByPeriodAndUser(
            Period period,
            UUID userId) {

        log.info("Fetching shifts for user ID: {}", userId);

        LocalDate from = dateUtils.getStartDateForPeriod(period);
        LocalDate to = dateUtils.getEndDateForPeriod(period);

        List<ShiftProgrammed> dbList = shiftProgrammedRepository.findByUserAndDateBetween(userId, from, to);

        List<NameAndUserIdForShiftProgrammedDTO> result = new ArrayList<>();

        dbList.forEach(shiftProgrammed -> {

            List<String> firstNames = new ArrayList<>();
            List<String>  lastNames = new ArrayList<>();
            List<UUID> userIds   = new ArrayList<>();

            shiftProgrammed.getUsers().forEach(su -> {
                var u = su.getUser();
                userIds.add(u.getUserId());
                firstNames.add(u.getFirstName());
                lastNames.add(u.getLastName());
            });

            result.add(new NameAndUserIdForShiftProgrammedDTO(firstNames, lastNames, userIds, shiftProgrammed.getShiftProgrammedId()));
        });

        return new ShiftProgrammedOutputDTO(dbList, result);
    }


    /**
     * Retrieve shifts for a specific company within a defined period.
     * @param period The period to filter shifts (DAY, WEEK, MONTH, TRIMESTER, SEMESTER, YEAR).
     * @param requesterUserId The ID of the user in the company.
     * @return A list of ShiftProgrammed entities matching the criteria.
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = "shiftsByCompany",
            key = "#requesterUserId + ':' + #period"
    )
    public ShiftProgrammedOutputDTO getShiftsByPeriodAndCompany(
            Period period,
            UUID requesterUserId) {

        UUID companyId = companyService.getCompanyByUserId(requesterUserId).getCompanyId();

        log.info("Fetching shifts for company ID: {}", companyId);

        LocalDate from = dateUtils.getStartDateForPeriod(period);
        LocalDate to = dateUtils.getEndDateForPeriod(period);

        List<ShiftProgrammed> dbList = shiftProgrammedRepository.findByCompanyAndDateBetween(companyId, from, to);

        List<NameAndUserIdForShiftProgrammedDTO> result = new ArrayList<>();

        dbList.forEach(shiftProgrammed -> {

            List<String> firstNames = new ArrayList<>();
            List<String>  lastNames = new ArrayList<>();
            List<UUID> userIds   = new ArrayList<>();

            shiftProgrammed.getUsers().forEach(su -> {
                var u = su.getUser();
                userIds.add(u.getUserId());
                firstNames.add(u.getFirstName());
                lastNames.add(u.getLastName());
            });

            result.add(new NameAndUserIdForShiftProgrammedDTO(firstNames, lastNames, userIds, shiftProgrammed.getShiftProgrammedId()));
        });

        return new ShiftProgrammedOutputDTO(dbList, result);
    }


    /**
     * Retrieve all users assigned to a specific shift.
     * @param shiftId The ID of the shift.
     * @return A list of User entities assigned to the shift.
     * @throws ResourceNotFoundException if the shift does not exist.
     */
    @Transactional(readOnly = true)
    @Cacheable(
            value = "usersInShift",
            key = "#shiftId"
    )
    public List<User> getUsersInShift(UUID shiftId) {

        // todo ritorna un dto
        log.info("getUsersInShift: Fetching users in shift with id: {}", shiftId);

        if (!shiftProgrammedRepository.existsById(shiftId)) {

            log.error("getUsersInShift: Shift with id {} does not exist", shiftId);
            throw new ResourceNotFoundException("Shift not found with ID: " + shiftId);
        }

        return shiftUserRepository.findUsersByShiftId(shiftId);
    }


    /**
     * Update an existing programmed shift with new details.
     * @param requesterUserId the ID of the manager who patched the shift
     * @param dto The DTO containing updated shift details.
     * @return The ID of the updated shift.
     * @throws ResourceNotFoundException if the shift does not exist.
     * @throws IllegalArgumentException if the start time is after the end time.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "usersInShift", key = "#dto.shiftProgrammedId"),

            @CacheEvict(value = "shiftsByUser",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByUser(#requesterUserId, T(com.pat.crewhive.common.Period).DAY)"),
            @CacheEvict(value = "shiftsByUser",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByUser(#requesterUserId, T(com.pat.crewhive.common.Period).WEEK)"),
            @CacheEvict(value = "shiftsByUser",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByUser(#requesterUserId, T(com.pat.crewhive.common.Period).MONTH)"),

            @CacheEvict(value = "shiftsByCompany",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByCompany(#requesterUserId, T(com.pat.crewhive.common.Period).DAY)"),
            @CacheEvict(value = "shiftsByCompany",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByCompany(#requesterUserId, T(com.pat.crewhive.common.Period).WEEK)"),
            @CacheEvict(value = "shiftsByCompany",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByCompany(#requesterUserId, T(com.pat.crewhive.common.Period).MONTH)")
    })
    public UUID patchShift(
            UUID requesterUserId,
            PatchShiftProgrammedDTO dto) {

        log.info("Patching shift with id: {}", dto.shiftProgrammedId());

        ShiftProgrammed shift = shiftProgrammedRepository.findByIdWithWorkers(dto.shiftProgrammedId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift not found with ID: " + dto.shiftProgrammedId()));


        shift.setShiftName(stringUtils.normalizeString(dto.name()));

        if (dto.start().isAfter(dto.end())) {
            throw new IllegalArgumentException("Shift start time cannot be after end time");
        }

        shift.setStart(dto.start());
        shift.setEnd(dto.end());
        shift.setDescription(dto.description());
        shift.setColor(dto.color());


        if (dto.userId() != null) {

            Set<UUID> newIds = dto.userId();

            Set<UUID> current = shift.getUsers().stream()
                    .map(su -> su.getUser().getUserId())
                    .collect(Collectors.toSet());

            // Rimuovi quelli non più presenti
            if (!current.isEmpty()) {
                Set<UUID> toRemove = new HashSet<>(current);
                toRemove.removeAll(newIds);
                if (!toRemove.isEmpty()) {
                    userService.getUsersByIds(toRemove).forEach(shift::removeUser);
                }
            }
            // Aggiungi i nuovi
            if (!newIds.isEmpty()) {
                Set<UUID> toAdd = new HashSet<>(newIds);
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
     * @param requesterUserId the ID of the user who wants to delete the shift
     * @param shiftId The ID of the shift to delete.
     * @throws ResourceNotFoundException if the shift does not exist.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "usersInShift", key = "#shiftId"),

            @CacheEvict(value = "shiftsByUser",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByUser(#requesterUserId, T(com.pat.crewhive.common.Period).DAY)"),
            @CacheEvict(value = "shiftsByUser",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByUser(#requesterUserId, T(com.pat.crewhive.common.Period).WEEK)"),
            @CacheEvict(value = "shiftsByUser",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByUser(#requesterUserId, T(com.pat.crewhive.common.Period).MONTH)"),

            @CacheEvict(value = "shiftsByCompany",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByCompany(#requesterUserId, T(com.pat.crewhive.common.Period).DAY)"),
            @CacheEvict(value = "shiftsByCompany",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByCompany(#requesterUserId, T(com.pat.crewhive.common.Period).WEEK)"),
            @CacheEvict(value = "shiftsByCompany",
                    key = "T(com.pat.crewhive.shiftprogrammed.CacheKeys).shiftsByCompany(#requesterUserId, T(com.pat.crewhive.common.Period).MONTH)")
    })
    public void deleteShift(
            UUID requesterUserId,
            UUID shiftId) {

        log.info("deleteShift: Deleting shift with id: {}", shiftId);

        if (!shiftProgrammedRepository.existsById(shiftId)) {

            log.error("deleteShift: Shift with id {} does not exist", shiftId);
            throw new ResourceNotFoundException("Shift not found with ID: " + shiftId);
        }

        shiftUserRepository.deleteByShiftId(shiftId);
        shiftProgrammedRepository.deleteById(shiftId);
    }
}
