package com.pat.crewhive.repository;

import com.pat.crewhive.model.shift.shiftprogrammed.entity.ShiftProgrammed;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftProgrammedRepository extends JpaRepository<ShiftProgrammed, Long> {
}
