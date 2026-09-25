package com.pat.crewhive.event;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    @EntityGraph(attributePaths = {"users", "users.user", "eventType"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
      select distinct e
      from Event e
      join e.users eu
      where eu.user.userId = :userId
        and eu.status = com.pat.crewhive.event.EventParticipationStatus.ACCEPTED
        and e.date between :from and :to
      order by e.start asc
  """)
    List<Event> findWithParticipantsByUserAndDateBetween(
            @Param("userId") UUID userId,
            @Param("from")   LocalDate from,
            @Param("to")     LocalDate to
    );

    @EntityGraph(attributePaths = {"users", "users.user", "eventType"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
      select distinct e
      from Event e
      join e.users eu
      where eu.user.userId = :userId
        and eu.status = com.pat.crewhive.event.EventParticipationStatus.PENDING
        and e.end > :now
      order by e.start asc
  """)
    List<Event> findPendingWithParticipantsByUser(
            @Param("userId") UUID userId,
            @Param("now")    OffsetDateTime now
    );

    @EntityGraph(attributePaths = {"users", "users.user", "eventType"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
      select distinct e
      from Event e
      join e.users eu
      where e.eventType.id = :eventTypeId
        and eu.user.company.companyId = :companyId
        and e.date between :from and :to
      order by e.start asc
  """)
    List<Event> findPublicWithParticipantsByCompanyAndDateBetween(
            @Param("eventTypeId") Short eventTypeId,
            @Param("companyId")   UUID companyId,
            @Param("from")        LocalDate from,
            @Param("to")          LocalDate to
    );

    @EntityGraph(attributePaths = {"users", "users.user", "eventType", "creator", "creator.company"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("select distinct e from Event e where e.eventId = :id")
    Optional<Event> findByIdWithParticipants(@Param("id") UUID id);
}
