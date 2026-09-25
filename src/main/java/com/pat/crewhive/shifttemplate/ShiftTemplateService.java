package com.pat.crewhive.shifttemplate;

import com.pat.crewhive.common.audit.SoftDeleteSupport;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.company.CompanyService;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.common.StringUtils;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ShiftTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ShiftTemplateService.class);

    private final ShiftTemplateRepository repo;
    private final CompanyService companyService;
    private final StringUtils stringUtils;
    private final UserService userService;

    public ShiftTemplateService(ShiftTemplateRepository repo,
                                CompanyService companyService,
                                StringUtils stringUtils,
                                UserService userService) {
        this.repo = repo;
        this.companyService = companyService;
        this.stringUtils = stringUtils;
        this.userService = userService;
    }


    /**
     * Every operation is scoped to the caller's company (taken from the token, never from the request):
     * a template of another company is indistinguishable from a missing one.
     * @throws AuthorizationDeniedException if the caller does not belong to any company.
     */
    private void requireCompany(UUID companyId) {

        if (companyId == null) {

            log.warn("Shift template operation denied: caller does not belong to any company");
            throw new AuthorizationDeniedException("User does not belong to any company.");
        }
    }


    /**
     * Retrieves a shift template by its name within the caller's company.
     * @param shiftName The name of the shift template.
     * @param companyId The caller's company ID (from the token).
     * @return The shift template if found.
     * @throws AuthorizationDeniedException if the caller does not belong to any company.
     * @throws ResourceNotFoundException if the shift template does not exist in the company.
     */
    @Transactional(readOnly = true)
    public ShiftTemplateOutputDTO getShiftTemplate(String shiftName, UUID companyId) {

        requireCompany(companyId);

        log.info("Fetching Shift Template '{}' for company {}", shiftName, companyId);

        String normalizedShiftName = stringUtils.normalizeString(shiftName);

        ShiftTemplate shiftTemplate = repo.findByShiftNameAndCompanyCompanyId(normalizedShiftName, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift template with name '" + normalizedShiftName + "' does not exist in company with ID " + companyId));

        return ShiftTemplateOutputDTO.from(shiftTemplate);
    }


    /**
     * Creates a new shift template in the caller's company.
     * @param dto Data transfer object containing shift template details.
     * @param companyId The caller's company ID (from the token).
     * @return The newly created shift template.
     * @throws AuthorizationDeniedException if the caller does not belong to any company.
     * @throws ResourceAlreadyExistsException if a shift template with the same name already exists in the company.
     */
    @Transactional
    public ShiftTemplateOutputDTO createShiftTemplate(CreateShiftTemplateDTO dto, UUID companyId) {

        requireCompany(companyId);

        if (repo.existsByShiftNameAndCompanyCompanyId(dto.shiftName(), companyId)) {
            //TODO: L'ID dev'essere lasciato solo nel log
            throw new ResourceAlreadyExistsException("Shift template with name '" + dto.shiftName() + "' already exists in company with ID " + companyId);
        }

        log.info("Creating Shift Template for company {}", companyId);

        ShiftTemplate shift = new ShiftTemplate();

        String normalizedShiftName = stringUtils.normalizeString(dto.shiftName());

        shift.setShiftName(normalizedShiftName);
        shift.setDescription(dto.description());
        shift.setColor(dto.color());
        shift.setStartShift(dto.start());
        shift.setEndShift(dto.end());

        Company company = companyService.getCompanyById(companyId);
        shift.setCompany(company);

        return ShiftTemplateOutputDTO.from(repo.save(shift));
    }


    /**
     * Updates an existing shift template of the caller's company.
     * @param dto Data transfer object containing updated shift template details.
     * @param companyId The caller's company ID (from the token).
     * @return The updated shift template.
     * @throws AuthorizationDeniedException if the caller does not belong to any company.
     * @throws ResourceNotFoundException if the shift template does not exist in the company.
     */
    @Transactional
    public ShiftTemplateOutputDTO patchShiftTemplate(PatchShiftTemplateDTO dto, UUID companyId) {

        requireCompany(companyId);

        log.info("Patching Shift Template for company {}", companyId);

        String normalizedShiftName = stringUtils.normalizeString(dto.shiftName());
        String normalizedOldShiftName = stringUtils.normalizeString(dto.oldShiftName());

        if (repo.existsByShiftNameAndCompanyCompanyId(normalizedShiftName, companyId)) {

            if (!normalizedOldShiftName.equals(normalizedShiftName)) {
                //TODO: L'ID dev'essere lasciato solo nel log
                throw new ResourceAlreadyExistsException("Shift template with name '" + normalizedShiftName + "' already exists in company with ID " + companyId);
            }
        }

        ShiftTemplate shiftTemplate = repo.findByShiftNameAndCompanyCompanyId(normalizedOldShiftName, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift template with name '" + normalizedOldShiftName + "' does not exist in company with ID " + companyId));

        shiftTemplate.setShiftName(normalizedShiftName);
        shiftTemplate.setDescription(dto.description());
        shiftTemplate.setColor(dto.color());
        shiftTemplate.setStartShift(dto.start());
        shiftTemplate.setEndShift(dto.end());

        repo.save(shiftTemplate);

        return ShiftTemplateOutputDTO.from(shiftTemplate);
    }


    /**
     * Soft-deletes a shift template of the caller's company.
     * @param shiftName The name of the shift template to delete.
     * @param companyId The caller's company ID (from the token).
     * @param actorId The caller's user ID (recorded as {@code deletedBy}).
     * @throws AuthorizationDeniedException if the caller does not belong to any company.
     * @throws ResourceNotFoundException if the shift template does not exist in the company.
     */
    @Transactional
    public void deleteShiftTemplate(String shiftName, UUID companyId, UUID actorId) {

        requireCompany(companyId);

        log.info("Deleting Shift Template '{}' for company {}", shiftName, companyId);

        String normalizedShiftName = stringUtils.normalizeString(shiftName);

        ShiftTemplate shiftTemplate = repo.findByShiftNameAndCompanyCompanyId(normalizedShiftName, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift template with name '" + shiftName + "' does not exist in company with ID " + companyId));

        User actor = userService.getUserById(actorId);
        SoftDeleteSupport.softDelete(repo, shiftTemplate, actor);
    }
}
