package com.pat.crewhive.repository;

import com.pat.crewhive.model.shift.shiftprogrammed.entity.ShiftProgrammed;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShiftProgrammedRepository extends JpaRepository<ShiftProgrammed, Long> {

    @EntityGraph(attributePaths = {"users.user"})
    @Query("select sp from ShiftProgrammed sp where sp.shiftProgrammedId = :id")
    Optional<ShiftProgrammed> findByIdWithWorkers(@Param("id") Long id);
}
