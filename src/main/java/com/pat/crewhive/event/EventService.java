package com.pat.crewhive.event;

import com.pat.crewhive.common.audit.SoftDeleteSupport;
import com.pat.crewhive.user.User;
import com.pat.crewhive.common.Period;
import com.pat.crewhive.user.UserService;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.common.DateUtils;
import com.pat.crewhive.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.pat.crewhive.event.EventType.PUBLIC;

@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final EventRepository eventRepository;
    private final EventUsersRepository eventUsersRepository;
    private final EventTypeRepository eventTypeRepository;
    private final UserService userService;
    private final StringUtils stringUtils;
    private final DateUtils dateUtils;

    public EventService(EventRepository eventRepository,
                        EventUsersRepository eventUsersRepository,
                        EventTypeRepository eventTypeRepository,
                        UserService userService,
                        StringUtils stringUtils,
                        DateUtils dateUtils) {

        this.eventRepository = eventRepository;
        this.eventUsersRepository = eventUsersRepository;
        this.eventTypeRepository = eventTypeRepository;
        this.userService = userService;
        this.stringUtils = stringUtils;
        this.dateUtils = dateUtils;
    }


    /**
     * Resolve a domain {@link EventType} to a managed {@link EventTypeEntity} reference,
     * without hitting the database (the id is fixed and known at compile time).
     * @param eventType the domain event type
     * @return a JPA reference usable to set the event_type_id FK
     */
    private EventTypeEntity toEntityReference(EventType eventType) {
        return eventTypeRepository.getReferenceById(eventType.getId());
    }


    /**
     * Create a new event and associate it with users.
     * @param createEventDTO The DTO containing event details.
     * @param creatorId The ID of the authenticated user creating the event.
     * @param callerCompanyId The company of the authenticated user; every participant must belong to it.
     * @param roles The roles of the authenticated user.
     * @return The ID of the created event.
     * @throws IllegalArgumentException if the start date is after the end date.
     * @throws AuthorizationDeniedException if a participant belongs to another company,
     *         or a non-manager tries to create a public event.
     */
    @Transactional
    public UUID createEvent(CreateEventDTO createEventDTO, UUID creatorId, UUID callerCompanyId, Set<String> roles) {

        log.info("Creating event with name: {}", createEventDTO.name());

        String normalizedEventName = stringUtils.normalizeString(createEventDTO.name());

        if (createEventDTO.start().isAfter(createEventDTO.end())) {
            throw new IllegalArgumentException("L'inizio deve essere prima della fine");
        }

        if (!roles.contains("ROLE_MANAGER") && createEventDTO.eventType() == PUBLIC) {
            throw new AuthorizationDeniedException("Non sei autorizzato a creare eventi pubblici");
        }

        User creator = userService.getUserById(creatorId);
        List<User> users = resolveParticipantsSameCompany(createEventDTO.userId(), callerCompanyId);

        Event event = new Event();
        event.setEventName(normalizedEventName);
        event.setDescription(createEventDTO.description());
        event.setStart(createEventDTO.start());
        event.setEnd(createEventDTO.end());
        event.setColor(createEventDTO.color());
        event.setEventType(toEntityReference(createEventDTO.eventType()));
        event.setCreator(creator);

        for (User user : users) {
            event.addUser(user);
        }
        // the creator always takes part in the event they created (addUser dedupes)
        event.addUser(creator);

        Event saved = eventRepository.save(event);

        return saved.getEventId();
    }


    /**
     * Resolve the given user IDs and assert that every one of them belongs to {@code callerCompanyId}.
     * Prevents injecting participants from another tenant via create/patch.
     * @param userIds the user IDs to resolve (may be empty, never null)
     * @param callerCompanyId the company the caller belongs to
     * @return the resolved users
     * @throws AuthorizationDeniedException if any user is outside the caller's company
     */
    private List<User> resolveParticipantsSameCompany(Set<UUID> userIds, UUID callerCompanyId) {

        List<User> users = userService.getUsersByIds(userIds);

        boolean foreign = users.stream().anyMatch(u ->
                u.getCompany() == null || !u.getCompany().getCompanyId().equals(callerCompanyId));

        if (foreign) {
            throw new AuthorizationDeniedException("Un partecipante non appartiene alla tua company");
        }

        return users;
    }


    /**
     * Assert that the caller may mutate (patch/delete) the given event: the caller must belong to
     * the same company as the event's creator, and must be either that creator or a manager.
     * @param event the event being mutated (its {@code creator} and {@code creator.company} must be loaded)
     * @param callerId the authenticated user's ID
     * @param callerCompanyId the authenticated user's company
     * @param roles the authenticated user's roles
     * @throws AuthorizationDeniedException if the caller is not entitled to mutate the event
     */
    private void assertCanMutate(Event event, UUID callerId, UUID callerCompanyId, Set<String> roles) {

        User creator = event.getCreator();
        UUID eventCompanyId = creator.getCompany() != null ? creator.getCompany().getCompanyId() : null;

        if (eventCompanyId == null || !eventCompanyId.equals(callerCompanyId)) {
            log.warn("User {} (company {}) denied access to event {} (company {})",
                    callerId, callerCompanyId, event.getEventId(), eventCompanyId);
            throw new AuthorizationDeniedException("Non hai accesso a questo evento");
        }

        if (!roles.contains("ROLE_MANAGER") && !creator.getUserId().equals(callerId)) {
            throw new AuthorizationDeniedException("Solo il creatore o un manager può modificare l'evento");
        }
    }


    /**
     * Fetch events for a user within a specified time period.
     * @param period The time period (DAY, WEEK, MONTH, TRIMESTER, SEMESTER, YEAR).
     * @param userId The ID of the user.
     * @return List of events within the specified period for the user.
     */
    @Transactional(readOnly = true)
    public List<EventOutputDTO> getEventsByPeriodAndUser(Period period, UUID userId) {

        log.info("Fetching events for userId: {} with eventTemp: {}", userId, period);

        LocalDate from = dateUtils.getStartDateForPeriod(period);
        LocalDate to = dateUtils.getEndDateForPeriod(period);

        return eventRepository.findWithParticipantsByUserAndDateBetween(userId, from, to).stream()
                .map(EventOutputDTO::from)
                .toList();
    }


    /**
     * Fetch all events associated with a specific user.
     * @param userId The ID of the user.
     * @return List of events associated with the user.
     */
    @Transactional(readOnly = true)
    public List<EventOutputDTO> getUserEvents(UUID userId) {

        log.info("Fetching all events for userId: {}", userId);

        return eventUsersRepository.findEventsByUserId(userId).stream()
                .map(EventOutputDTO::from)
                .toList();
    }


    /**
     * Fetch all public events for a specific company.
     * @param companyId The ID of the company.
     * @return List of public events.
     */
    @Transactional(readOnly = true)
    public List<EventOutputDTO> getPublicEventsByCompanyAndPeriod(UUID companyId, Period period) {

        log.info("Fetching all public events");

        LocalDate from = dateUtils.getStartDateForPeriod(period);
        LocalDate to = dateUtils.getEndDateForPeriod(period);

        return eventRepository.findPublicWithParticipantsByCompanyAndDateBetween(PUBLIC.getId(), companyId, from, to).stream()
                .map(EventOutputDTO::from)
                .toList();
    }


    /**
     * Update an existing event.
     * @param dto The DTO containing updated event details.
     * @param callerId The ID of the authenticated user requesting the update.
     * @param callerCompanyId The company of the authenticated user.
     * @param roles The roles of the authenticated user.
     * @return The ID of the updated event.
     * @throws ResourceNotFoundException if the event does not exist.
     * @throws IllegalArgumentException if the start date is after the end date.
     * @throws AuthorizationDeniedException if the caller may not mutate this event,
     *         or adds a participant from another company.
     */
    @Transactional
    public UUID patchEvent(PatchEventDTO dto, UUID callerId, UUID callerCompanyId, Set<String> roles) {

        log.info("Patching event with ID: {}", dto.eventId());

        if (dto.start().isAfter(dto.end())) {
            throw new IllegalArgumentException("L'inizio deve essere prima della fine");
        }

        Event event = eventRepository.findByIdWithParticipants(dto.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento non trovato con ID: " + dto.eventId()));

        assertCanMutate(event, callerId, callerCompanyId, roles);

        String normalizedEventName = stringUtils.normalizeString(dto.name());

        event.setEventName(normalizedEventName);
        event.setDescription(dto.description());
        event.setStart(dto.start());
        event.setEnd(dto.end());
        event.setColor(dto.color());
        event.setEventType(toEntityReference(dto.eventType()));

        Set<UUID> newUserIds = dto.userId();
        if (newUserIds != null) {

            Set<UUID> existingIds = event.getUsers().stream()
                    .map(eu -> eu.getUser().getUserId())
                    .collect(Collectors.toSet());

            // rimuovi i non più presenti
            Set<UUID> toRemove = new HashSet<>(existingIds);
            toRemove.removeAll(newUserIds);
            if (!toRemove.isEmpty()) {
                List<User> usersToRemove = userService.getUsersByIds(toRemove);
                usersToRemove.forEach(event::removeUser);
            }

            // aggiungi i nuovi mancanti (riattivando un legame soft-deleted se esiste
            // già per questa coppia utente/evento, per non collidere sulla sua PK)
            Set<UUID> toAdd = new HashSet<>(newUserIds);
            toAdd.removeAll(existingIds);
            if (!toAdd.isEmpty()) {
                List<User> usersToAdd = resolveParticipantsSameCompany(toAdd, callerCompanyId);
                for (User user : usersToAdd) {
                    EventUsers link = eventUsersRepository
                            .findByIdIncludingDeleted(user.getUserId(), event.getEventId())
                            .map(existing -> { existing.restore(); return existing; })
                            .orElseGet(() -> new EventUsers(user, event));
                    event.attachUser(link);
                }
            }
        }

        Event saved = eventRepository.save(event);

        return saved.getEventId();
    }


    /**
     * Delete an event by its ID.
     * @param eventId The ID of the event to delete.
     * @param actorId The ID of the authenticated user requesting the deletion (also the audit actor).
     * @param callerCompanyId The company of the authenticated user.
     * @param roles The roles of the authenticated user.
     * @throws ResourceNotFoundException if the event does not exist.
     * @throws AuthorizationDeniedException if the caller may not delete this event.
     */
    @Transactional
    public void deleteEvent(UUID eventId, UUID actorId, UUID callerCompanyId, Set<String> roles) {

        log.info("Deleting event with ID: {}", eventId);

        Event event = eventRepository.findByIdWithParticipants(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento non trovato con ID: " + eventId));

        assertCanMutate(event, actorId, callerCompanyId, roles);

        User actor = userService.getUserById(actorId);
        SoftDeleteSupport.softDelete(eventRepository, event, actor);
    }
}
