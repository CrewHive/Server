package com.pat.crewhive.shifttemplate;

import com.pat.crewhive.company.Company;
import org.junit.jupiter.api.Test;

import java.time.OffsetTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShiftTemplateOutputDTOTest {

    @Test
    void from_mapsTemplateFieldsWithoutTouchingTheCompany() {
        UUID id = UUID.randomUUID();
        OffsetTime start = OffsetTime.parse("08:00:00Z");
        OffsetTime end = OffsetTime.parse("16:00:00Z");
        ShiftTemplate st = new ShiftTemplate(id, "morning", start, end, "desc", "FF0000", new Company());

        ShiftTemplateOutputDTO dto = ShiftTemplateOutputDTO.from(st);

        assertThat(dto).isEqualTo(new ShiftTemplateOutputDTO(id, "morning", start, end, "desc", "FF0000"));
    }
}
