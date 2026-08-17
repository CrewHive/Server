package com.pat.crewhive.shifttemplate;

import com.pat.crewhive.company.Company;
import com.pat.crewhive.company.CompanyService;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.common.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ShiftTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ShiftTemplateService.class);

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
    public ShiftTemplate getShiftTemplate(String shiftName, UUID companyId) {

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

        if (repo.existsByShiftNameAndCompanyCompanyId(dto.shiftName(), dto.companyId())) {
            throw new ResourceAlreadyExistsException("Shift template with name '" + dto.shiftName() + "' already exists in company with ID " + dto.companyId());
        }

        log.info("Creating Shift Template for company {}", dto.companyId());

        ShiftTemplate shift = new ShiftTemplate();

        String normalizedShiftName = stringUtils.normalizeString(dto.shiftName());

        shift.setShiftName(normalizedShiftName);
        shift.setDescription(dto.description());
        shift.setColor(dto.color());
        shift.setStartShift(dto.start());
        shift.setEndShift(dto.end());

        Company company = companyService.getCompanyById(dto.companyId());
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

        log.info("Patching Shift Template for company {}", dto.companyId());

        String normalizedShiftName = stringUtils.normalizeString(dto.shiftName());
        String normalizedOldShiftName = stringUtils.normalizeString(dto.oldShiftName());

        if (repo.existsByShiftNameAndCompanyCompanyId(normalizedShiftName, dto.companyId())) {

            if (!normalizedOldShiftName.equals(normalizedShiftName)) {
                throw new ResourceAlreadyExistsException("Shift template with name '" + normalizedShiftName + "' already exists in company with ID " + dto.companyId());
            }
        }

        ShiftTemplate shiftTemplate = repo.findByShiftNameAndCompanyCompanyId(normalizedOldShiftName, dto.companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Shift template with name '" + normalizedOldShiftName + "' does not exist in company with ID " + dto.companyId()));

        shiftTemplate.setShiftName(normalizedShiftName);
        shiftTemplate.setDescription(dto.description());
        shiftTemplate.setColor(dto.color());
        shiftTemplate.setStartShift(dto.start());
        shiftTemplate.setEndShift(dto.end());

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
    public void deleteShiftTemplate(String shiftName, UUID companyId) {

        log.info("Deleting Shift Template '{}' for company {}", shiftName, companyId);

        String normalizedShiftName = stringUtils.normalizeString(shiftName);

        ShiftTemplate shiftTemplate = repo.findByShiftNameAndCompanyCompanyId(normalizedShiftName, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift template with name '" + shiftName + "' does not exist in company with ID " + companyId));

        repo.delete(shiftTemplate);
    }
}
