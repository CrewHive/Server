package com.pat.crewhive.shifttemplate;

import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/shift-template")
public class ShiftTemplateController implements ShiftTemplateControllerInterface {

    private static final Logger log = LoggerFactory.getLogger(ShiftTemplateController.class);

    private final ShiftTemplateService shiftTemplateService;

    public ShiftTemplateController(ShiftTemplateService shiftTemplateService) {
        this.shiftTemplateService = shiftTemplateService;
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/get/{shiftName}/company/{companyId}")
    public ResponseEntity<ShiftTemplate> getShiftTemplate(@PathVariable @NotBlank @NoHtml @Size(min = 1, max = 32) String shiftName,
                                                          @PathVariable @NotNull UUID companyId) {

        log.info("Received request to get shift template '{}' for company ID {}", shiftName, companyId);

        ShiftTemplate st = shiftTemplateService.getShiftTemplate(shiftName, companyId);

        return ResponseEntity.ok(st);
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/create")
    public ResponseEntity<ShiftTemplate> createShiftTemplate(@RequestBody @Valid CreateShiftTemplateDTO request) {

        log.info("Received request to create shift template '{}' for company ID {}", request.shiftName(), request.companyId());

        ShiftTemplate st = shiftTemplateService.createShiftTemplate(request);

        return ResponseEntity.ok(st);
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/update")
    public ResponseEntity<ShiftTemplate> updateShiftTemplate(@RequestBody @Valid PatchShiftTemplateDTO request) {

        log.info("Received request to update shift template '{}' for company ID {}", request.shiftName(), request.companyId());

        ShiftTemplate st = shiftTemplateService.patchShiftTemplate(request);

        return ResponseEntity.ok(st);
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/delete/{shiftName}/company/{companyId}")
    public ResponseEntity<?> deleteShiftTemplate(@PathVariable @NotBlank @NoHtml @Size(min = 1, max = 32) String shiftName,
                                                 @PathVariable @NotNull UUID companyId) {

        log.info("Received request to delete shift template '{}' for company ID {}", shiftName, companyId);

        shiftTemplateService.deleteShiftTemplate(shiftName, companyId);

        return ResponseEntity.ok().build();
    }
}
