package com.pat.crewhive.repository;

import com.pat.crewhive.dto.shift.shift_template.CreateShiftTemplateDTO;
import com.pat.crewhive.model.shift.shifttemplate.entity.ShiftTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Long> {

    Optional<ShiftTemplate> findByShiftNameAndCompanyCompanyId(String shiftName, Long companyId);

    boolean existsByShiftNameAndCompanyCompanyId(String shiftName, Long companyId);
}
