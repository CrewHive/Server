package com.pat.crewhive.shifttemplate;

import com.pat.crewhive.company.Company;
import org.junit.jupiter.api.Test;

import java.time.OffsetTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link ShiftTemplate} accessors.
 */
class ShiftTemplateEntityTest {

    private static final OffsetTime START = OffsetTime.parse("09:00:00+02:00");
    private static final OffsetTime END = OffsetTime.parse("17:00:00+02:00");

    @Test
    void fullConstructor_setsEveryField() {
        UUID id = UUID.randomUUID();
        Company company = new Company();

        ShiftTemplate template = new ShiftTemplate(id, "morning", START, END, "desc", "FF0000", company);

        assertThat(template.getShiftId()).isEqualTo(id);
        assertThat(template.getShiftName()).isEqualTo("morning");
        assertThat(template.getStartShift()).isEqualTo(START);
        assertThat(template.getEndShift()).isEqualTo(END);
        assertThat(template.getDescription()).isEqualTo("desc");
        assertThat(template.getColor()).isEqualTo("FF0000");
        assertThat(template.getCompany()).isSameAs(company);
    }

    @Test
    void settersReplaceTheValues() {
        ShiftTemplate template = new ShiftTemplate();
        UUID id = UUID.randomUUID();
        Company company = new Company();

        template.setShiftId(id);
        template.setShiftName("night");
        template.setStartShift(END);
        template.setEndShift(START);
        template.setDescription("d2");
        template.setColor("00FF00");
        template.setCompany(company);

        assertThat(template.getShiftId()).isEqualTo(id);
        assertThat(template.getShiftName()).isEqualTo("night");
        assertThat(template.getStartShift()).isEqualTo(END);
        assertThat(template.getEndShift()).isEqualTo(START);
        assertThat(template.getDescription()).isEqualTo("d2");
        assertThat(template.getColor()).isEqualTo("00FF00");
        assertThat(template.getCompany()).isSameAs(company);
    }
}
