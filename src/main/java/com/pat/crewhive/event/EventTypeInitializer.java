package com.pat.crewhive.event;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds the event_type lookup table from the {@link EventType} enum on startup,
 * since there is no Flyway/Liquibase migration in this project to do it declaratively.
 */
@Component
public class EventTypeInitializer implements ApplicationRunner {

    private final EventTypeRepository eventTypeRepository;

    public EventTypeInitializer(EventTypeRepository eventTypeRepository) {
        this.eventTypeRepository = eventTypeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        for (EventType type : EventType.values()) {
            eventTypeRepository.findById(type.getId())
                    .orElseGet(() -> eventTypeRepository.save(new EventTypeEntity(type.getId(), type.name())));
        }
    }
}
