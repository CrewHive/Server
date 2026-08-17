package com.pat.crewhive.shiftworked;


import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/shift-worked")
public class ShiftWorkedController implements ShiftWorkedControllerInterface {

    private static final Logger log = LoggerFactory.getLogger(ShiftWorkedController.class);

    private final ShiftWorkedService shiftWorkedService;

    public ShiftWorkedController(ShiftWorkedService shiftWorkedService) {
        this.shiftWorkedService = shiftWorkedService;
    }

    @Override
    @PostMapping("/create")
    public ResponseEntity<?> createShiftWorked(@RequestBody @Valid CreateShiftWorkedDTO request) {

        log.info("Creating ShiftWorked for user {}", request.userId());

        shiftWorkedService.createShiftWorked(request);

        return ResponseEntity.ok("ShiftWorked created successfully");
    }
}
