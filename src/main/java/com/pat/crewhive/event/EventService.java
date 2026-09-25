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
import java.time.OffsetDateTime;
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
        List<User> users = userService.getUsersInCompany(createEventDTO.userId(), callerCompanyId);

        Event event = new Event();
        event.setEventName(normalizedEventName);
        event.setDescription(createEventDTO.description());
        event.setStart(createEventDTO.start());
        event.setEnd(createEventDTO.end());
        event.setColor(createEventDTO.color());
        event.setEventType(toEntityReference(createEventDTO.eventType()));
        event.setCreator(creator);

        // the creator always takes part in the event they created, and is added first:
        // addUser dedupes, so listing themselves among the participants must not leave them PENDING
        event.addUser(creator, EventParticipationStatus.ACCEPTED);

        EventParticipationStatus initialStatus = initialStatusFor(createEventDTO.eventType());
        for (User user : users) {
            event.addUser(user, initialStatus);
        }

        Event saved = eventRepository.save(event);

        log.info("Event {} created by user {} with {} participants (status {})",
                saved.getEventId(), creatorId, users.size(), initialStatus);

        return saved.getEventId();
    }


    /**
     * Initial status of a participant added by someone else: PUBLIC events are a broadcast by a
     * manager and need no consent; invitations to PRIVATE events must be answered by the invitee.
     */
    private EventParticipationStatus initialStatusFor(EventType eventType) {
        return eventType == PUBLIC ? EventParticipationStatus.ACCEPTED : EventParticipationStatus.PENDING;
    }


    /**
     * Assert that the event belongs to the caller's company (the company of its creator).
     * @param event the event ({@code creator} and {@code creator.company} must be loaded)
     * @throws AuthorizationDeniedException if the event is from another company
     */
    private void assertSameCompany(Event event, UUID callerId, UUID callerCompanyId) {

        User creator = event.getCreator();
        UUID eventCompanyId = creator.getCompany() != null ? creator.getCompany().getCompanyId() : null;

        if (eventCompanyId == null || !eventCompanyId.equals(callerCompanyId)) {
            log.warn("User {} (company {}) denied access to event {} (company {})",
                    callerId, callerCompanyId, event.getEventId(), eventCompanyId);
            throw new AuthorizationDeniedException("Non hai accesso a questo evento");
        }
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

        assertSameCompany(event, callerId, callerCompanyId);

        User creator = event.getCreator();

        if (!roles.contains("ROLE_MANAGER") && !creator.getUserId().equals(callerId)) {
            throw new AuthorizationDeniedException("Solo il creatore o un manager può modificare l'evento");
        }
    }


    /**
     * Assert that the target user belongs to the caller's company. A missing user and a user of
     * another company get the same answer, so the endpoint can't be used to probe which IDs exist.
     * @throws ResourceNotFoundException if the target is unknown or outside the caller's company
     */
    private void assertTargetInCompany(UUID targetUserId, UUID callerCompanyId) {

        User target = userService.getUserById(targetUserId);

        if (target.getCompany() == null || !target.getCompany().getCompanyId().equals(callerCompanyId)) {
            log.warn("Denied agenda read of user {} to a caller of company {}", targetUserId, callerCompanyId);
            throw new ResourceNotFoundException("User not found");
        }
    }


    /**
     * Restrict a colleague's events to what the caller may see: public events, and private
     * events the caller has accepted (a pending or declined invitation does not open the agenda
     * of the inviter). The owner's own agenda is returned untouched.
     */
    private List<Event> visibleTo(List<Event> events, UUID targetUserId, UUID callerId) {

        if (targetUserId.equals(callerId)) {
            return events;
        }

        return events.stream()
                .filter(e -> e.getEventType().getId() == PUBLIC.getId()
                        || e.getUsers().stream().anyMatch(eu -> eu.getUser().getUserId().equals(callerId)
                                && eu.getStatus() == EventParticipationStatus.ACCEPTED))
                .toList();
    }


    /**
     * Fetch events for a user within a specified time period.
     * @param period The time period (DAY, WEEK, MONTH, TRIMESTER, SEMESTER, YEAR).
     * @param targetUserId The ID of the user whose agenda is requested.
     * @param callerId The ID of the authenticated user.
     * @param callerCompanyId The company of the authenticated user.
     * @return Events of the target within the period that the caller may see.
     * @throws ResourceNotFoundException if the target is unknown or belongs to another company.
     */
    @Transactional(readOnly = true)
    public List<EventOutputDTO> getEventsByPeriodAndUser(Period period, UUID targetUserId, UUID callerId, UUID callerCompanyId) {

        log.info("Fetching events for userId: {} with eventTemp: {}", targetUserId, period);

        assertTargetInCompany(targetUserId, callerCompanyId);

        LocalDate from = dateUtils.getStartDateForPeriod(period);
        LocalDate to = dateUtils.getEndDateForPeriod(period);

        return visibleTo(eventRepository.findWithParticipantsByUserAndDateBetween(targetUserId, from, to), targetUserId, callerId).stream()
                .map(EventOutputDTO::from)
                .toList();
    }


    /**
     * Fetch all events associated with a specific user.
     * @param targetUserId The ID of the user whose agenda is requested.
     * @param callerId The ID of the authenticated user.
     * @param callerCompanyId The company of the authenticated user.
     * @return Events of the target that the caller may see.
     * @throws ResourceNotFoundException if the target is unknown or belongs to another company.
     */
    @Transactional(readOnly = true)
    public List<EventOutputDTO> getUserEvents(UUID targetUserId, UUID callerId, UUID callerCompanyId) {

        log.info("Fetching all events for userId: {}", targetUserId);

        assertTargetInCompany(targetUserId, callerCompanyId);

        return visibleTo(eventUsersRepository.findEventsByUserId(targetUserId), targetUserId, callerId).stream()
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
     * Fetch the pending invitations of the caller (events not yet ended).
     * @param callerId The ID of the authenticated user.
     * @return Events the caller has been invited to and has not answered yet.
     */
    @Transactional(readOnly = true)
    public List<EventOutputDTO> getPendingInvitations(UUID callerId) {

        log.info("Fetching pending invitations for userId: {}", callerId);

        return eventRepository.findPendingWithParticipantsByUser(callerId, OffsetDateTime.now()).stream()
                .map(EventOutputDTO::from)
                .toList();
    }


    /**
     * Answer an invitation to an event. The answer can be changed later between accepted and declined,
     * but a participant never goes back to pending.
     * @param eventId The event the caller was invited to.
     * @param callerId The ID of the authenticated user.
     * @param callerCompanyId The company of the authenticated user.
     * @param accepted true to accept, false to decline.
     * @throws ResourceNotFoundException if the event does not exist or the caller does not take part in it.
     * @throws AuthorizationDeniedException if the event belongs to another company.
     * @throws IllegalArgumentException if the creator tries to decline their own event.
     */
    @Transactional
    public void respondToInvitation(UUID eventId, UUID callerId, UUID callerCompanyId, boolean accepted) {

        Event event = eventRepository.findByIdWithParticipants(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento non trovato con ID: " + eventId));

        assertSameCompany(event, callerId, callerCompanyId);

        EventUsers link = event.getUsers().stream()
                .filter(eu -> eu.getUser().getUserId().equals(callerId))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("User {} answered event {} without being invited", callerId, eventId);
                    return new ResourceNotFoundException("Evento non trovato con ID: " + eventId);
                });

        if (!accepted && event.getCreator().getUserId().equals(callerId)) {
            throw new IllegalArgumentException("Il creatore non può rifiutare il proprio evento");
        }

        EventParticipationStatus status = accepted ? EventParticipationStatus.ACCEPTED : EventParticipationStatus.DECLINED;
        link.setStatus(status);

        log.info("User {} {} event {}", callerId, status, eventId);
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

        // stessa regola di createEvent: senza questo check un evento PRIVATE si trasformerebbe in PUBLIC via patch
        if (!roles.contains("ROLE_MANAGER") && dto.eventType() == PUBLIC) {
            throw new AuthorizationDeniedException("Non sei autorizzato a rendere pubblico un evento");
        }

        String normalizedEventName = stringUtils.normalizeString(dto.name());

        event.setEventName(normalizedEventName);
        event.setDescription(dto.description());
        event.setStart(dto.start());
        event.setEnd(dto.end());
        event.setColor(dto.color());
        event.setEventType(toEntityReference(dto.eventType()));

        // un evento PUBLIC è un broadcast del manager: gli inviti ancora in sospeso non servono più
        // (un rifiuto già dato resta tale)
        if (dto.eventType() == PUBLIC) {
            event.getUsers().stream()
                    .filter(eu -> eu.getStatus() == EventParticipationStatus.PENDING)
                    .forEach(eu -> eu.setStatus(EventParticipationStatus.ACCEPTED));
        }

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
                EventParticipationStatus initialStatus = initialStatusFor(dto.eventType());
                List<User> usersToAdd = userService.getUsersInCompany(toAdd, callerCompanyId);
                for (User user : usersToAdd) {
                    EventUsers link = eventUsersRepository
                            .findByIdIncludingDeleted(user.getUserId(), event.getEventId())
                            .map(existing -> {
                                existing.restore();
                                // un rifiuto non si annulla rimuovendo e ri-invitando
                                if (existing.getStatus() != EventParticipationStatus.DECLINED) {
                                    existing.setStatus(initialStatus);
                                }
                                return existing;
                            })
                            .orElseGet(() -> new EventUsers(user, event, initialStatus));
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
