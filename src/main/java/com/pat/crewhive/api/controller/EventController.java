package com.pat.crewhive.api.controller;


import com.pat.crewhive.api.swagger.interfaces.EventControllerInterface;
import com.pat.crewhive.dto.request.event.CreateEventDTO;
import com.pat.crewhive.dto.request.event.PatchEventDTO;
import com.pat.crewhive.model.event.Event;
import com.pat.crewhive.model.user.wrapper.CustomUserDetails;
import com.pat.crewhive.model.util.Period;
import com.pat.crewhive.service.EventService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/event")
public class EventController implements EventControllerInterface {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @Override
    @PostMapping(path = "/create", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Long> createEvent(@AuthenticationPrincipal CustomUserDetails cud,
                                            @RequestBody @Valid CreateEventDTO dto) {

        log.info("Received request to create event");

        String role = cud.getRole();

        return ResponseEntity.ok(eventService.createEvent(dto, role));
    }

    @Override
    @GetMapping(path = "/{temp}/user/{userId}", produces = "application/json")
    public ResponseEntity<List<Event>> getEventsByPeriodAndUser(@PathVariable @NotNull Period temp,
                                                                @PathVariable @NotNull Long userId) {

        log.info("Received request to get events for user {} in period {}", userId, temp);

        return ResponseEntity.ok(eventService.getEventsByPeriodAndUser(temp, userId));
    }

    @Override
    @GetMapping(path = "/user/{userId}", produces = "application/json")
    public ResponseEntity<List<Event>> getAllEventsByUser(@PathVariable @NotNull Long userId) {

        log.info("Received request to get all events for user {}", userId);

        return ResponseEntity.ok(eventService.getUserEvents(userId));
    }

    @Override
    @GetMapping(path = "/public/{temp}", produces = "application/json")
    public ResponseEntity<List<Event>> getAllPublicEventsByCompanyAndPeriod(@AuthenticationPrincipal CustomUserDetails cud,
                                                                            @PathVariable @NotNull Period temp) {

        Long companyId = cud.getCompanyId();

        log.info("Received request to get all public events");
        return ResponseEntity.ok(eventService.getPublicEventsByCompanyAndPeriod(companyId, temp));
    }

    @Override
    @PatchMapping(path = "/patch", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Long> patchEvent(@RequestBody @Valid PatchEventDTO dto) {

        log.info("Received request to patch event with id: {}", dto.getEventId());

        return ResponseEntity.ok(eventService.patchEvent(dto));
    }

    @Override
    @DeleteMapping(path = "/delete/{eventId}", produces = "application/json")
    public ResponseEntity<String> deleteEvent(@PathVariable @NotNull Long eventId) {

        log.info("Received request to delete event with id: {}", eventId);

        eventService.deleteEvent(eventId);
        return ResponseEntity.ok("Evento eliminato con successo");
    }
}
