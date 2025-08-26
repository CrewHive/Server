package com.pat.crewhive.repository;

import com.pat.crewhive.model.event.Event;
import com.pat.crewhive.model.util.EventType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    @EntityGraph(attributePaths = {"users", "users.user"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
      select distinct e
      from Event e
      join e.users eu
      where eu.user.userId = :userId
        and e.date between :from and :to
      order by e.start asc
  """)
    List<Event> findWithParticipantsByUserAndDateBetween(
            @Param("userId") Long userId,
            @Param("from")   LocalDate from,
            @Param("to")     LocalDate to
    );

    @EntityGraph(attributePaths = {"users", "users.user"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
      select distinct e
      from Event e
      join e.users eu
      where e.eventType = :eventType
        and eu.user.company.companyId = :companyId
        and e.date between :from and :to
      order by e.start asc
  """)
    List<Event> findPublicWithParticipantsByCompanyAndDateBetween(
            @Param("eventType") EventType eventType,
            @Param("companyId") Long companyId,
            @Param("from")      LocalDate from,
            @Param("to")        LocalDate to
    );

    @EntityGraph(attributePaths = {"users", "users.user"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("select distinct e from Event e where e.eventId = :id")
    Optional<Event> findByIdWithParticipants(@Param("id") Long id);
}
