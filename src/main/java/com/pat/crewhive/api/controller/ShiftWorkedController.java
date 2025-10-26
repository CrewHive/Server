package com.pat.crewhive.api.controller;


import com.pat.crewhive.api.swagger.interfaces.ShiftWorkedControllerInterface;
import com.pat.crewhive.dto.request.shift.worked.CreateShiftWorkedDTO;
import com.pat.crewhive.service.ShiftWorkedService;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/shift-worked")
public class ShiftWorkedController implements ShiftWorkedControllerInterface {

    private final ShiftWorkedService shiftWorkedService;

    public ShiftWorkedController(ShiftWorkedService shiftWorkedService) {
        this.shiftWorkedService = shiftWorkedService;
    }

    @Override
    @PostMapping("/create")
    public ResponseEntity<?> createShiftWorked(@RequestBody @Valid CreateShiftWorkedDTO request) {

        log.info("Creating ShiftWorked for user {}", request.getUserId());

        shiftWorkedService.createShiftWorked(request);

        return ResponseEntity.ok("ShiftWorked created successfully");
    }
}
