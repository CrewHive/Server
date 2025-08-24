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
    List<Event> findAllByEventType(EventType eventType);

    @EntityGraph(attributePaths = {"users", "users.user"})
    @Query("select distinct e from Event e where e.eventId = :id")
    Optional<Event> findByIdWithParticipants(@Param("id") Long id);

    @EntityGraph(attributePaths = {"users", "users.user"})
    @Query("""
           select distinct e
           from Event e
           join e.users eu
           where eu.user.userId = :userId
           order by e.start asc
           """)
    List<Event> findAllByUserIdWithParticipants(@Param("userId") Long userId);

}
