package com.pat.crewhive.common;

import com.pat.crewhive.event.CreateEventDTO;
import com.pat.crewhive.event.EventType;
import com.pat.crewhive.event.PatchEventDTO;
import com.pat.crewhive.shiftprogrammed.CreateShiftProgrammedDTO;
import com.pat.crewhive.shiftprogrammed.PatchShiftProgrammedDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validation tests for the participant limits (H7): 50 per event, 200 per shift.
 */
class ParticipantLimitValidationTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-08-21T09:00:00Z");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-08-21T17:00:00Z");

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static Set<UUID> ids(int n) {
        Set<UUID> ids = new HashSet<>();
        for (int i = 0; i < n; i++) ids.add(UUID.randomUUID());
        return ids;
    }

    @Test
    void createEventDto_limitIs50() {
        assertThat(validator.validate(new CreateEventDTO("Riunione", null, START, END, "FF0000", EventType.PRIVATE, ids(50)))).isEmpty();
        assertThat(validator.validate(new CreateEventDTO("Riunione", null, START, END, "FF0000", EventType.PRIVATE, ids(51))))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("userId");
    }

    @Test
    void patchEventDto_limitIs50() {
        assertThat(validator.validate(new PatchEventDTO(UUID.randomUUID(), "Riunione", null, START, END, "FF0000", EventType.PRIVATE, ids(50)))).isEmpty();
        assertThat(validator.validate(new PatchEventDTO(UUID.randomUUID(), "Riunione", null, START, END, "FF0000", EventType.PRIVATE, ids(51))))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("userId");
    }

    @Test
    void createShiftDto_limitIs200() {
        assertThat(validator.validate(new CreateShiftProgrammedDTO("Turno", null, START, END, "00FF00", ids(200)))).isEmpty();
        assertThat(validator.validate(new CreateShiftProgrammedDTO("Turno", null, START, END, "00FF00", ids(201))))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("userId");
    }

    @Test
    void createShiftDto_nullParticipants_isRejected_butEmptySetIsAllowed() {
        assertThat(validator.validate(new CreateShiftProgrammedDTO("Turno", null, START, END, "00FF00", null)))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("userId");
        assertThat(validator.validate(new CreateShiftProgrammedDTO("Turno", null, START, END, "00FF00", Set.of()))).isEmpty();
    }

    @Test
    void patchShiftDto_limitIs200() {
        assertThat(validator.validate(new PatchShiftProgrammedDTO(UUID.randomUUID(), "Turno", null, START, END, "00FF00", ids(200)))).isEmpty();
        assertThat(validator.validate(new PatchShiftProgrammedDTO(UUID.randomUUID(), "Turno", null, START, END, "00FF00", ids(201))))
                .extracting(v -> v.getPropertyPath().toString()).containsExactly("userId");
    }
}
