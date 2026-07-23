package com.pat.crewhive.shiftprogrammed;


import com.pat.crewhive.user.User;
import com.pat.crewhive.security.CustomUserDetails;
import com.pat.crewhive.common.Period;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/shift-programmed")
public class ShiftProgrammedController implements ShiftProgrammedControllerInterface {

    private final ShiftProgrammedService shiftProgrammedService;

    public ShiftProgrammedController(ShiftProgrammedService shiftProgrammedService) {
        this.shiftProgrammedService = shiftProgrammedService;
    }

    @Override
    @PostMapping("/create")
    public ResponseEntity<UUID> createShift(@AuthenticationPrincipal CustomUserDetails cud,
                                            @RequestBody @Valid CreateShiftProgrammedDTO dto) {

        log.info("Received request to create shift: {}", dto.getName());

        return ResponseEntity.ok(shiftProgrammedService.createShift(cud.getUserId(), dto));
    }

    @Override
    @GetMapping("/period/{period}/user/{userId}")
    public ResponseEntity<ShiftProgrammedOutputDTO> getShiftsByPeriodAndUser(@PathVariable @NotNull Period period,
                                                                          @PathVariable @NotNull UUID userId) {
        log.info("Received request to get shifts for user {} in period {}", userId, period);

        return ResponseEntity.ok(shiftProgrammedService.getShiftsByPeriodAndUser(period, userId));
    }


    @Override
    @GetMapping("/period/{period}/company}")
    public ResponseEntity<ShiftProgrammedOutputDTO> getShiftsByPeriodAndCompany(@AuthenticationPrincipal CustomUserDetails cud,
                                                                                @PathVariable @NotNull Period period) {

        log.info("Received request to get shifts for company {} in period {}",  cud.getCompanyId(), period);

        return ResponseEntity.ok(shiftProgrammedService.getShiftsByPeriodAndCompany(period, cud.getUserId()));
    }


    @Override
    @GetMapping("/users/{shiftId}")
    public ResponseEntity<List<User>> getUsersByShift(@PathVariable @NotNull UUID shiftId) {

        log.info("Received request to get users for shift {}", shiftId);

        return ResponseEntity.ok(shiftProgrammedService.getUsersInShift(shiftId));
    }


    @Override
    @PatchMapping("/patch")
    public ResponseEntity<UUID> patchShift(@AuthenticationPrincipal CustomUserDetails cud,
                                           @RequestBody @Valid PatchShiftProgrammedDTO dto) {

        log.info("Received request to patch shift: {}", dto.getName());

        return ResponseEntity.ok(shiftProgrammedService.patchShift(cud.getUserId(), dto));
    }

    @Override
    @DeleteMapping("/delete/{shiftId}")
    public ResponseEntity<?> deleteShift(@AuthenticationPrincipal CustomUserDetails cud,
                                         @PathVariable @NotNull UUID shiftId) {

        log.info("Received request to delete shift: {}", shiftId);

        shiftProgrammedService.deleteShift(cud.getUserId(), shiftId);
        return ResponseEntity.ok().build();
    }
}
