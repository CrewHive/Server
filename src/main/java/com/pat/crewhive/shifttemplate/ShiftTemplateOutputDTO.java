package com.pat.crewhive.shifttemplate;

import java.time.OffsetTime;
import java.util.UUID;

/**
 * Response DTO for a {@link ShiftTemplate}, exposed to the client instead of the JPA entity
 * (whose {@code company -> users} graph must never be serialized).
 */
public record ShiftTemplateOutputDTO(
        UUID shiftId,
        String shiftName,
        OffsetTime startShift,
        OffsetTime endShift,
        String description,
        String color
) {

    public static ShiftTemplateOutputDTO from(ShiftTemplate st) {

        return new ShiftTemplateOutputDTO(
                st.getShiftId(),
                st.getShiftName(),
                st.getStartShift(),
                st.getEndShift(),
                st.getDescription(),
                st.getColor()
        );
    }
}
