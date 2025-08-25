package com.pat.crewhive.service;

import com.pat.crewhive.dto.shift.shift_template.CreateShiftTemplateDTO;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.shift.shifttemplate.entity.ShiftTemplate;
import com.pat.crewhive.repository.ShiftTemplateRepository;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ShiftTemplateService {

    private final ShiftTemplateRepository repo;
    private final CompanyService companyService;

    public ShiftTemplateService(ShiftTemplateRepository repo,
                                CompanyService companyService) {
        this.repo = repo;
        this.companyService = companyService;
    }


    /**
     * Retrieves a shift template by its name and company ID.
     * @param shiftName The name of the shift template.
     * @param companyId The ID of the company.
     * @return The ShiftTemplate object if found.
     * @throws ResourceNotFoundException if the shift template does not exist in the company.
     */
    @Transactional(readOnly = true)
    public ShiftTemplate getShiftTemplate(String shiftName, Long companyId) {

        log.info("Fetching Shift Template '{}' for company {}", shiftName, companyId);

        return repo.findByShiftNameAndCompanyCompanyId(shiftName, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift template with name '" + shiftName + "' does not exist in company with ID " + companyId));
    }


    /**
     * Creates a new shift template in the database.
     * @param dto Data transfer object containing shift template details.
     * @return The ID of the newly created shift template.
     * @throws ResourceAlreadyExistsException if a shift template with the same name already exists in the company.
     */
    @Transactional
    public ShiftTemplate createShiftTemplate(CreateShiftTemplateDTO dto) {

        if (repo.existsByShiftNameAndCompanyCompanyId(dto.getShiftName(), dto.getCompanyId())) {
            throw new ResourceAlreadyExistsException("Shift template with name '" + dto.getShiftName() + "' already exists in company with ID " + dto.getCompanyId());
        }

        log.info("Creating Shift Template for company {}", dto.getCompanyId());

        ShiftTemplate shift = new ShiftTemplate();

        shift.setShiftName(dto.getShiftName());
        shift.setDescription(dto.getDescription());
        shift.setColor(dto.getColor());
        shift.setStartShift(dto.getStart());
        shift.setEndShift(dto.getEnd());

        Company company = companyService.getCompanyById(dto.getCompanyId());
        shift.setCompany(company);

        return repo.save(shift);
    }


    /**
     * Updates an existing shift template in the database.
     * @param name The name of the shift template to update.
     * @param dto Data transfer object containing updated shift template details.
     * @return The ID of the updated shift template.
     * @throws ResourceNotFoundException if the shift template does not exist in the company.
     */
    @Transactional
    public Long patchShiftTemplate(String name, CreateShiftTemplateDTO dto) {

        log.info("Patching Shift Template for company {}", dto.getCompanyId());

        ShiftTemplate shiftTemplate = repo.findById(dto.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift template with name '" + name + "' does not exist in company with ID " + dto.getCompanyId()));

        shiftTemplate.setShiftName(dto.getShiftName());
        shiftTemplate.setDescription(dto.getDescription());
        shiftTemplate.setColor(dto.getColor());
        shiftTemplate.setStartShift(dto.getStart());
        shiftTemplate.setEndShift(dto.getEnd());

        repo.save(shiftTemplate);

        return shiftTemplate.getShiftId();
    }


    /**
     * Deletes a shift template from the database.
     * @param shiftName The name of the shift template to delete.
     * @param companyId The ID of the company.
     * @throws ResourceNotFoundException if the shift template does not exist in the company.
     */
    @Transactional
    public void deleteShiftTemplate(String shiftName, Long companyId) {

        log.info("Deleting Shift Template '{}' for company {}", shiftName, companyId);

        ShiftTemplate shiftTemplate = repo.findByShiftNameAndCompanyCompanyId(shiftName, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift template with name '" + shiftName + "' does not exist in company with ID " + companyId));

        repo.delete(shiftTemplate);
    }
}
