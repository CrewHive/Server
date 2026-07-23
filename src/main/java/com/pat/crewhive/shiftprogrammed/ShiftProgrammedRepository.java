package com.pat.crewhive.shiftprogrammed;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftProgrammedRepository extends JpaRepository<ShiftProgrammed, UUID> {

    @EntityGraph(attributePaths = {"users.user"})
    @Query("select sp from ShiftProgrammed sp where sp.shiftProgrammedId = :id")
    Optional<ShiftProgrammed> findByIdWithWorkers(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"users.user"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
        select distinct sp
        from ShiftProgrammed sp
        join sp.users su
        where su.user.userId = :userId
            and sp.date between :from and :to
        order by sp.start asc
        """)
    List<ShiftProgrammed> findByUserAndDateBetween(
            @Param("userId") UUID userId,
            @Param("from")   java.time.LocalDate from,
            @Param("to")     java.time.LocalDate to
    );

    @EntityGraph(attributePaths = {"users.user"}, type = EntityGraph.EntityGraphType.LOAD)
    @Query("""
        select distinct sp
        from ShiftProgrammed sp
        join sp.users su
        where su.user.company.companyId = :companyId
            and sp.date between :from and :to
        order by sp.start asc
        """)
    List<ShiftProgrammed> findByCompanyAndDateBetween(
            @Param("companyId") UUID companyId,
            @Param("from")      java.time.LocalDate from,
            @Param("to")        java.time.LocalDate to
    );
}
