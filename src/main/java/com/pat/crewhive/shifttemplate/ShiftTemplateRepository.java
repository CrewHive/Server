package com.pat.crewhive.shifttemplate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, UUID> {

    Optional<ShiftTemplate> findByShiftNameAndCompanyCompanyId(String shiftName, UUID companyId);

    boolean existsByShiftNameAndCompanyCompanyId(String shiftName, UUID companyId);
}
