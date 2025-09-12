package com.pat.crewhive.service;


import com.pat.crewhive.dto.event.CreateEventDTO;
import com.pat.crewhive.dto.event.PatchEventDTO;
import com.pat.crewhive.model.event.Event;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.model.util.Period;
import com.pat.crewhive.repository.EventRepository;
import com.pat.crewhive.repository.EventUsersRepository;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.service.utils.DateUtils;
import com.pat.crewhive.service.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.pat.crewhive.model.util.EventType.PUBLIC;

@Slf4j
@Service
public class EventService {

    private final EventRepository eventRepository;
    private final EventUsersRepository eventUsersRepository;
    private final UserService userService;
    private final StringUtils stringUtils;
    private final DateUtils dateUtils;

    public EventService(EventRepository eventRepository,
                        EventUsersRepository eventUsersRepository,
                        UserService userService,
                        StringUtils stringUtils,
                        DateUtils dateUtils) {

        this.eventRepository = eventRepository;
        this.eventUsersRepository = eventUsersRepository;
        this.userService = userService;
        this.stringUtils = stringUtils;
        this.dateUtils = dateUtils;
    }


    /**
     * Create a new event and associate it with users.
     * @param createEventDTO The DTO containing event details.
     * @return The ID of the created event.
     * @throws IllegalArgumentException if the start date is after the end date.
     */
    @Transactional
    public Long createEvent(CreateEventDTO createEventDTO, String role) {

        log.info("Creating event with name: {}", createEventDTO.getName());

        String normalizedEventName = stringUtils.normalizeString(createEventDTO.getName());
        createEventDTO.setName(normalizedEventName);

        if (createEventDTO.getStart().isAfter(createEventDTO.getEnd())) {
            throw new IllegalArgumentException("L'inizio deve essere prima della fine");
        }

        if (role.equals("ROLE_USER") && createEventDTO.getEventType() == PUBLIC) {
            throw new AuthorizationDeniedException("Non sei autorizzato a creare eventi pubblici");
        }

        List<User> users = userService.getUsersByIds(createEventDTO.getUserId());

        Event event = new Event();
        event.setEventName(createEventDTO.getName());
        event.setDescription(createEventDTO.getDescription());
        event.setStart(createEventDTO.getStart());
        event.setEnd(createEventDTO.getEnd());
        event.setColor(createEventDTO.getColor());
        event.setEventType(createEventDTO.getEventType());

        for (User user : users) {
            event.addUser(user);
        }

        Event saved = eventRepository.save(event);

        return saved.getEventId();
    }


    /**
     * Fetch events for a user within a specified time period.
     * @param period The time period (DAY, WEEK, MONTH, TRIMESTER, SEMESTER, YEAR).
     * @param userId The ID of the user.
     * @return List of events within the specified period for the user.
     */
    @Transactional(readOnly = true)
    public List<Event> getEventsByPeriodAndUser(Period period, Long userId) {

        //todo ritorna un DTO
        log.info("Fetching events for userId: {} with eventTemp: {}", userId, period);

        LocalDate from = dateUtils.getStartDateForPeriod(period);
        LocalDate to = dateUtils.getEndDateForPeriod(period);

        return eventRepository.findWithParticipantsByUserAndDateBetween(userId, from, to);
    }


    /**
     * Fetch all events associated with a specific user.
     * @param userId The ID of the user.
     * @return List of events associated with the user.
     */
    @Transactional(readOnly = true)
    public List<Event> getUserEvents(Long userId) {

        //todo ritorna un DTO
        log.info("Fetching all events for userId: {}", userId);

        return eventUsersRepository.findEventsByUserId(userId);
    }


    /**
     * Fetch all public events for a specific company.
     * @param companyId The ID of the company.
     * @return List of public events.
     */
    @Transactional(readOnly = true)
    public List<Event> getPublicEventsByCompanyAndPeriod(Long companyId, Period period) {

        //todo ritorna un DTO
        log.info("Fetching all public events");

        LocalDate from = dateUtils.getStartDateForPeriod(period);
        LocalDate to = dateUtils.getEndDateForPeriod(period);

        return eventRepository.findPublicWithParticipantsByCompanyAndDateBetween(PUBLIC, companyId, from, to);
    }


    /**
     * Update an existing event.
     * @param dto The DTO containing updated event details.
     * @return The ID of the updated event.
     * @throws ResourceNotFoundException if the event does not exist.
     * @throws IllegalArgumentException if the start date is after the end date.
     */
    @Transactional
    public Long patchEvent(PatchEventDTO dto) {

        log.info("Patching event with ID: {}", dto.getEventId());

        if (dto.getStart().isAfter(dto.getEnd())) {
            throw new IllegalArgumentException("L'inizio deve essere prima della fine");
        }

        Event event = eventRepository.findByIdWithParticipants(dto.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento non trovato con ID: " + dto.getEventId()));

        String normalizedEventName = stringUtils.normalizeString(dto.getName());
        dto.setName(normalizedEventName);

        event.setEventName(dto.getName());
        event.setDescription(dto.getDescription());
        event.setStart(dto.getStart());
        event.setEnd(dto.getEnd());
        event.setColor(dto.getColor());
        event.setEventType(dto.getEventType());

        Set<Long> newUserIds = dto.getUserId();
        if (newUserIds != null) {

            Set<Long> existingIds = event.getUsers().stream()
                    .map(eu -> eu.getUser().getUserId())
                    .collect(Collectors.toSet());

            // rimuovi i non più presenti
            Set<Long> toRemove = new HashSet<>(existingIds);
            toRemove.removeAll(newUserIds);
            if (!toRemove.isEmpty()) {
                List<User> usersToRemove = userService.getUsersByIds(toRemove);
                usersToRemove.forEach(event::removeUser);
            }

            // aggiungi i nuovi mancanti
            Set<Long> toAdd = new HashSet<>(newUserIds);
            toAdd.removeAll(existingIds);
            if (!toAdd.isEmpty()) {
                List<User> usersToAdd = userService.getUsersByIds(toAdd);
                usersToAdd.forEach(event::addUser);
            }
        }

        Event saved = eventRepository.save(event);

        return saved.getEventId();
    }


    /**
     * Delete an event by its ID.
     * @param eventId The ID of the event to delete.
     * @throws ResourceNotFoundException if the event does not exist.
     */
    @Transactional
    public void deleteEvent(Long eventId) {

        log.info("Deleting event with ID: {}", eventId);

        if (!eventRepository.existsById(eventId)) {
            throw new ResourceNotFoundException("Evento non trovato con ID: " + eventId);
        }

        eventUsersRepository.deleteByEventId(eventId);
        eventRepository.deleteById(eventId);
    }
}
