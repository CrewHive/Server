package com.pat.crewhive.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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


    /**
     * Cerca una riga EventUsers per la sua chiave composita bypassando il filtro
     * {@code active = true} di {@code @SQLRestriction}, così un legame soft-deleted
     * in passato può essere trovato e riattivato invece di collidere sulla sua chiave
     * primaria quando lo stesso utente viene aggiunto di nuovo allo stesso evento.
     */
    @Query(value = "SELECT * FROM event_users WHERE user_id = :userId AND event_id = :eventId", nativeQuery = true)
    Optional<EventUsers> findByIdIncludingDeleted(@Param("userId") UUID userId, @Param("eventId") UUID eventId);
}
