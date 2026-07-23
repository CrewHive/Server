package com.pat.crewhive.shiftworked;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ShiftWorkedRepository extends JpaRepository<ShiftWorked, UUID> {
}
