package com.pat.crewhive.repository;

import com.pat.crewhive.model.event.Event;
import com.pat.crewhive.model.event.EventUsers;
import com.pat.crewhive.model.event.EventUsersId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface EventUsersRepository extends JpaRepository<EventUsers, EventUsersId> {

    @Query("""
       select e
       from EventUsers eu
       join eu.event e
       where eu.user.userId = :userId
       order by e.start asc
       """)
    List<Event> findEventsByUserId(@Param("userId") Long userId);

    @Query("""
       select e
       from EventUsers eu
       join eu.event e
       where eu.user.userId = :userId
         and e.date between :from and :to
       order by e.start asc
       """)
    List<Event> findEventsByUserIdAndDateBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to")     LocalDate to);


    @Modifying
    @Query("delete from EventUsers eu where eu.event.eventId = :eventId")
    int deleteByEventId(@Param("eventId") Long eventId);
}
