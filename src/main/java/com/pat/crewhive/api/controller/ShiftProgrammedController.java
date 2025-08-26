package com.pat.crewhive.api.controller;


import com.pat.crewhive.api.swagger.interfaces.ShiftProgrammedControllerInterface;
import com.pat.crewhive.dto.shift.shift_programmed.CreateShiftProgrammedDTO;
import com.pat.crewhive.dto.shift.shift_programmed.PatchShiftProgrammedDTO;
import com.pat.crewhive.model.shift.shiftprogrammed.entity.ShiftProgrammed;
import com.pat.crewhive.model.user.entity.User;
import com.pat.crewhive.model.util.Period;
import com.pat.crewhive.service.ShiftProgrammedService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/shift-programmed")
public class ShiftProgrammedController implements ShiftProgrammedControllerInterface {

    private final ShiftProgrammedService shiftProgrammedService;

    public ShiftProgrammedController(ShiftProgrammedService shiftProgrammedService) {
        this.shiftProgrammedService = shiftProgrammedService;
    }


    @PostMapping("/create")
    public ResponseEntity<Long> createShift(@RequestBody @Valid CreateShiftProgrammedDTO dto) {

        log.info("Received request to create shift: {}", dto.getName());

        return ResponseEntity.ok(shiftProgrammedService.createShift(dto));
    }


    @GetMapping("/period/{period}/user/{userId}")
    public ResponseEntity<List<ShiftProgrammed>> getShiftsByPeriodAndUser(@PathVariable @NotNull Period period,
                                                                          @PathVariable @NotNull Long userId) {

        log.info("Received request to get shifts for user {} in period {}", userId, period);

        return ResponseEntity.ok(shiftProgrammedService.getShiftsByPeriodAndUser(period, userId));
    }

    @GetMapping("/users/{shiftId}")
    public ResponseEntity<List<User>> getUsersByShift(@PathVariable @NotNull Long shiftId) {

        log.info("Received request to get users for shift {}", shiftId);

        return ResponseEntity.ok(shiftProgrammedService.getUsersInShift(shiftId));
    }

    @PatchMapping("/patch")
    public ResponseEntity<Long> patchShift(@RequestBody @Valid PatchShiftProgrammedDTO dto) {

        log.info("Received request to patch shift: {}", dto.getName());

        return ResponseEntity.ok(shiftProgrammedService.patchShift(dto));
    }

    @DeleteMapping("/delete/{shiftId}")
    public ResponseEntity<?> deleteShift(@PathVariable @NotNull Long shiftId) {

        log.info("Received request to delete shift: {}", shiftId);

        shiftProgrammedService.deleteShift(shiftId);
        return ResponseEntity.ok().build();
    }
}
