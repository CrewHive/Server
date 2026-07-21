package com.pat.crewhive.shifttemplate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, Long> {

    Optional<ShiftTemplate> findByShiftNameAndCompanyCompanyId(String shiftName, Long companyId);

    boolean existsByShiftNameAndCompanyCompanyId(String shiftName, Long companyId);
}
