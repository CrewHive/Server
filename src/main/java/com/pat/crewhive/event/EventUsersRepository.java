package com.pat.crewhive.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EventUsersRepository extends JpaRepository<EventUsers, EventUsersId> {

    @Query("""
       select e
       from EventUsers eu
       join eu.event e
       where eu.user.userId = :userId
       order by e.start asc
       """)
    List<Event> findEventsByUserId(@Param("userId") UUID userId);


    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from EventUsers eu where eu.event.eventId = :eventId")
    int deleteByEventId(@Param("eventId") UUID eventId);
}
