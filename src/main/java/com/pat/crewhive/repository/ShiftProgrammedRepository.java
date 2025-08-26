package com.pat.crewhive.repository;

import com.pat.crewhive.model.shift.shiftprogrammed.entity.ShiftProgrammed;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShiftProgrammedRepository extends JpaRepository<ShiftProgrammed, Long> {

    @EntityGraph(attributePaths = {"users.user"})
    @Query("select sp from ShiftProgrammed sp where sp.shiftProgrammedId = :id")
    Optional<ShiftProgrammed> findByIdWithWorkers(@Param("id") Long id);

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
            @Param("userId") Long userId,
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
            @Param("companyId") Long companyId,
            @Param("from")      java.time.LocalDate from,
            @Param("to")        java.time.LocalDate to
    );
}
