package com.pat.crewhive.api.controller;

import com.pat.crewhive.model.shift.shifttemplate.entity.ShiftTemplate;
import com.pat.crewhive.security.sanitizer.annotation.NoHtml;
import com.pat.crewhive.service.ShiftTemplateService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/shift-template")
public class ShiftTemplateController {

    private final ShiftTemplateService shiftTemplateService;

    public ShiftTemplateController(ShiftTemplateService shiftTemplateService) {
        this.shiftTemplateService = shiftTemplateService;
    }


    @GetMapping("/get/{shiftName}/company/{companyId}")
    public ResponseEntity<ShiftTemplate> getShiftTemplate(@PathVariable @NotBlank @NoHtml @Size(min = 1, max = 32) String shiftName,
                                                          @PathVariable @NotNull Long companyId) {

        log.info("Received request to get shift template '{}' for company ID {}", shiftName, companyId);

        ShiftTemplate st = shiftTemplateService.getShiftTemplate(shiftName, companyId);

        return ResponseEntity.ok(st);
    }
}
