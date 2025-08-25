package com.pat.crewhive.service;

import com.pat.crewhive.dto.shift.shift_template.CreateShiftTemplateDTO;
import com.pat.crewhive.repository.ShiftTemplateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ShiftTemplateService {

    private final ShiftTemplateRepository repo;

    public ShiftTemplateService(ShiftTemplateRepository repo) {
        this.repo = repo;
    }

    public Long createShiftTemplate(CreateShiftTemplateDTO dto) {
        return null;
    }
}
