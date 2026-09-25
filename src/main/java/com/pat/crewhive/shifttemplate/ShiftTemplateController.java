package com.pat.crewhive.shifttemplate;

import com.pat.crewhive.security.CustomUserDetails;
import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping("/get/{shiftName}")
    public ResponseEntity<ShiftTemplateOutputDTO> getShiftTemplate(@AuthenticationPrincipal CustomUserDetails cud,
                                                          @PathVariable @NotBlank @NoHtml @Size(min = 1, max = 32) String shiftName) {

        log.info("Received request to get shift template '{}' for company ID {}", shiftName, cud.getCompanyId());

        ShiftTemplateOutputDTO st = shiftTemplateService.getShiftTemplate(shiftName, cud.getCompanyId());

        return ResponseEntity.ok(st);
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    @PostMapping("/create")
    public ResponseEntity<ShiftTemplateOutputDTO> createShiftTemplate(@AuthenticationPrincipal CustomUserDetails cud,
                                                             @RequestBody @Valid CreateShiftTemplateDTO request) {

        log.info("Received request to create shift template '{}' for company ID {}", request.shiftName(), cud.getCompanyId());

        ShiftTemplateOutputDTO st = shiftTemplateService.createShiftTemplate(request, cud.getCompanyId());

        return ResponseEntity.ok(st);
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/update")
    public ResponseEntity<ShiftTemplateOutputDTO> updateShiftTemplate(@AuthenticationPrincipal CustomUserDetails cud,
                                                             @RequestBody @Valid PatchShiftTemplateDTO request) {

        log.info("Received request to update shift template '{}' for company ID {}", request.shiftName(), cud.getCompanyId());

        ShiftTemplateOutputDTO st = shiftTemplateService.patchShiftTemplate(request, cud.getCompanyId());

        return ResponseEntity.ok(st);
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    @DeleteMapping("/delete/{shiftName}")
    public ResponseEntity<?> deleteShiftTemplate(@AuthenticationPrincipal CustomUserDetails cud,
                                                 @PathVariable @NotBlank @NoHtml @Size(min = 1, max = 32) String shiftName) {

        log.info("Received request to delete shift template '{}' for company ID {}", shiftName, cud.getCompanyId());

        shiftTemplateService.deleteShiftTemplate(shiftName, cud.getCompanyId(), cud.getUserId());

        return ResponseEntity.ok().build();
    }
}
