package com.pat.crewhive.api.controller;

import com.pat.crewhive.api.swagger.interfaces.ShiftTemplateControllerInterface;
import com.pat.crewhive.dto.request.shift.template.CreateShiftTemplateDTO;
import com.pat.crewhive.dto.request.shift.template.PatchShiftTemplateDTO;
import com.pat.crewhive.model.shift.shifttemplate.entity.ShiftTemplate;
import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import com.pat.crewhive.service.ShiftTemplateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/shift-template")
public class ShiftTemplateController implements ShiftTemplateControllerInterface {

    private final ShiftTemplateService shiftTemplateService;

    public ShiftTemplateController(ShiftTemplateService shiftTemplateService) {
        this.shiftTemplateService = shiftTemplateService;
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @GetMapping("/get/{shiftName}/company/{companyId}")
    public ResponseEntity<ShiftTemplate> getShiftTemplate(@PathVariable @NotBlank @NoHtml @Size(min = 1, max = 32) String shiftName,
                                                          @PathVariable @NotNull Long companyId) {

        log.info("Received request to get shift template '{}' for company ID {}", shiftName, companyId);

        ShiftTemplate st = shiftTemplateService.getShiftTemplate(shiftName, companyId);

        return ResponseEntity.ok(st);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @PostMapping("/create")
    public ResponseEntity<ShiftTemplate> createShiftTemplate(@RequestBody @Valid CreateShiftTemplateDTO request) {

        log.info("Received request to create shift template '{}' for company ID {}", request.getShiftName(), request.getCompanyId());

        ShiftTemplate st = shiftTemplateService.createShiftTemplate(request);

        return ResponseEntity.ok(st);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @PatchMapping("/update")
    public ResponseEntity<ShiftTemplate> updateShiftTemplate(@RequestBody @Valid PatchShiftTemplateDTO request) {

        log.info("Received request to update shift template '{}' for company ID {}", request.getShiftName(), request.getCompanyId());

        ShiftTemplate st = shiftTemplateService.patchShiftTemplate(request);

        return ResponseEntity.ok(st);
    }

    @Override
    @PreAuthorize("hasRole('ROLE_MANAGER')")
    @DeleteMapping("/delete/{shiftName}/company/{companyId}")
    public ResponseEntity<?> deleteShiftTemplate(@PathVariable @NotBlank @NoHtml @Size(min = 1, max = 32) String shiftName,
                                                 @PathVariable @NotNull Long companyId) {

        log.info("Received request to delete shift template '{}' for company ID {}", shiftName, companyId);

        shiftTemplateService.deleteShiftTemplate(shiftName, companyId);

        return ResponseEntity.ok().build();
    }
}
