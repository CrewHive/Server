package com.pat.crewhive.service;

import com.pat.crewhive.dto.request.shift.template.CreateShiftTemplateDTO;
import com.pat.crewhive.dto.request.shift.template.PatchShiftTemplateDTO;
import com.pat.crewhive.model.company.entity.Company;
import com.pat.crewhive.model.shift.shifttemplate.entity.ShiftTemplate;
import com.pat.crewhive.repository.ShiftTemplateRepository;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.service.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ShiftTemplateService {

    private final ShiftTemplateRepository repo;
    private final CompanyService companyService;
    private final StringUtils stringUtils;

    public ShiftTemplateService(ShiftTemplateRepository repo,
                                CompanyService companyService,
                                StringUtils stringUtils) {
        this.repo = repo;
        this.companyService = companyService;
        this.stringUtils = stringUtils;
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

        //todo ritorna un dto

        log.info("Fetching Shift Template '{}' for company {}", shiftName, companyId);

        String normalizedShiftName = stringUtils.normalizeString(shiftName);

        return repo.findByShiftNameAndCompanyCompanyId(normalizedShiftName, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift template with name '" + normalizedShiftName + "' does not exist in company with ID " + companyId));
    }


    /**
     * Creates a new shift template in the database.
     * @param dto Data transfer object containing shift template details.
     * @return The ID of the newly created shift template.
     * @throws ResourceAlreadyExistsException if a shift template with the same name already exists in the company.
     */
    @Transactional
    public ShiftTemplate createShiftTemplate(CreateShiftTemplateDTO dto) {

        //todo ritorna un dto

        if (repo.existsByShiftNameAndCompanyCompanyId(dto.getShiftName(), dto.getCompanyId())) {
            throw new ResourceAlreadyExistsException("Shift template with name '" + dto.getShiftName() + "' already exists in company with ID " + dto.getCompanyId());
        }

        log.info("Creating Shift Template for company {}", dto.getCompanyId());

        ShiftTemplate shift = new ShiftTemplate();

        String normalizedShiftName = stringUtils.normalizeString(dto.getShiftName());

        shift.setShiftName(normalizedShiftName);
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
     * @param dto Data transfer object containing updated shift template details.
     * @return The ID of the updated shift template.
     * @throws ResourceNotFoundException if the shift template does not exist in the company.
     */
    @Transactional
    public ShiftTemplate patchShiftTemplate(PatchShiftTemplateDTO dto) {

        //todo ritorna un dto

        log.info("Patching Shift Template for company {}", dto.getCompanyId());

        String normalizedShiftName = stringUtils.normalizeString(dto.getShiftName());
        String normalizedOldShiftName = stringUtils.normalizeString(dto.getOldShiftName());

        dto.setShiftName(normalizedShiftName);
        dto.setOldShiftName(normalizedOldShiftName);

        if (repo.existsByShiftNameAndCompanyCompanyId(dto.getShiftName(), dto.getCompanyId())) {

            if (!dto.getOldShiftName().equals(dto.getShiftName())) {
                throw new ResourceAlreadyExistsException("Shift template with name '" + dto.getShiftName() + "' already exists in company with ID " + dto.getCompanyId());
            }
        }

        ShiftTemplate shiftTemplate = repo.findByShiftNameAndCompanyCompanyId(dto.getOldShiftName(), dto.getCompanyId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift template with name '" + dto.getOldShiftName() + "' does not exist in company with ID " + dto.getCompanyId()));

        shiftTemplate.setShiftName(dto.getShiftName());
        shiftTemplate.setDescription(dto.getDescription());
        shiftTemplate.setColor(dto.getColor());
        shiftTemplate.setStartShift(dto.getStart());
        shiftTemplate.setEndShift(dto.getEnd());

        repo.save(shiftTemplate);

        return shiftTemplate;
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

        String normalizedShiftName = stringUtils.normalizeString(shiftName);

        ShiftTemplate shiftTemplate = repo.findByShiftNameAndCompanyCompanyId(normalizedShiftName, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift template with name '" + shiftName + "' does not exist in company with ID " + companyId));

        repo.delete(shiftTemplate);
    }
}
