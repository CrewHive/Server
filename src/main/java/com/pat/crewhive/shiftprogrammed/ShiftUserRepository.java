package com.pat.crewhive.shiftprogrammed;


import com.pat.crewhive.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftUserRepository extends JpaRepository<ShiftUser, ShiftUserId> {

    @Query("""
        select distinct u
        from ShiftUser su
        join su.user u
        where su.shift.shiftProgrammedId = :shiftId
        order by u.email asc
        """)
    List<User> findUsersByShiftId(@Param("shiftId") UUID shiftId);


    /**
     * Soft-delete di tutti i legami ShiftUser di un utente (es. quando lascia
     * l'azienda), senza caricare ogni riga nel persistence context.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ShiftUser su set su.active = false, su.deletedAt = :now where su.user.userId = :userId and su.active = true")
    int deleteByUserId(@Param("userId") UUID userId, @Param("now") OffsetDateTime now);

    /**
     * Cerca una riga ShiftUser per la sua chiave composita bypassando il filtro
     * {@code active = true} di {@code @SQLRestriction}, così un legame soft-deleted
     * in passato può essere trovato e riattivato invece di collidere sulla sua chiave
     * primaria quando lo stesso utente viene aggiunto di nuovo allo stesso turno.
     */
    @Query(value = "SELECT * FROM shift_user WHERE shift_programmed_id = :shiftId AND user_id = :userId", nativeQuery = true)
    Optional<ShiftUser> findByIdIncludingDeleted(@Param("shiftId") UUID shiftId, @Param("userId") UUID userId);
}
