package com.pat.crewhive.repository;

import com.pat.crewhive.model.event.Event;
import com.pat.crewhive.model.util.EventType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = {"users", "users.user"})
    List<Event> findAllByEventTypeAndCompanyId(EventType eventType, Long companyId);

    @EntityGraph(attributePaths = {"users", "users.user"})
    @Query("select distinct e from Event e where e.eventId = :id")
    Optional<Event> findByIdWithParticipants(@Param("id") Long id);

}
