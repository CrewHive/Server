package com.pat.crewhive.repository;

import com.pat.crewhive.model.shift.shifttemplate.entity.ShiftTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Long> {
}
