package com.pat.crewhive.shiftworked;

import com.pat.crewhive.company.Company;
import com.pat.crewhive.support.AbstractEndpointIntegrationTest;
import com.pat.crewhive.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint tests for {@code POST /shift-worked/create}: the shift is always registered for the caller.
 */
class ShiftWorkedEndpointsIntegrationTest extends AbstractEndpointIntegrationTest {

    private static final OffsetDateTime START = OffsetDateTime.now().minusHours(10).withNano(0);
    private static final OffsetDateTime END = START.plusHours(8);

    private Company alpha;
    private User employee;
    private User companyless;

    @BeforeEach
    void setUp() {
        alpha = newCompany("alpha");
        employee = newUser("employee@alpha.test", alpha);
        companyless = newUser("free@example.test", null);
    }

    private Map<String, Object> body() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("shiftName", "Night shift");
        body.put("start", START);
        body.put("end", END);
        body.put("breakTime", 30);
        body.put("extraHours", new BigDecimal("1.50"));
        return body;
    }

    private int workedShifts(User user) {
        return jdbc.queryForObject("SELECT count(*) FROM shift_worked WHERE user_id = ?", Integer.class, user.getUserId());
    }

    @Test
    void create_registersTheShiftForTheCallerAndAddsTheExtraHoursToTheOvertime() throws Exception {
        mockMvc.perform(post("/shift-worked/create").with(as(asUser(employee)))
                        .contentType(APPLICATION_JSON).content(json(body())))
                .andExpect(status().isOk());

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT shift_name, break_time, worked_hours, extra_hours FROM shift_worked WHERE user_id = ?", employee.getUserId());
        assertThat(row.get("shift_name")).isEqualTo("night shift");
        assertThat(row.get("break_time")).isEqualTo(30);
        assertThat((BigDecimal) row.get("worked_hours")).isEqualByComparingTo("7.50");
        assertThat((BigDecimal) row.get("extra_hours")).isEqualByComparingTo("1.50");
        BigDecimal overtime = jdbc.queryForObject("SELECT overtime_hours FROM users WHERE user_id = ?", BigDecimal.class, employee.getUserId());
        assertThat(overtime).isEqualByComparingTo("1.50");
    }

    @Test
    void create_twice_accumulatesTheOvertime() throws Exception {
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/shift-worked/create").with(as(asUser(employee)))
                            .contentType(APPLICATION_JSON).content(json(body())))
                    .andExpect(status().isOk());
        }

        BigDecimal overtime = jdbc.queryForObject("SELECT overtime_hours FROM users WHERE user_id = ?", BigDecimal.class, employee.getUserId());
        assertThat(overtime).isEqualByComparingTo("3.00");
        assertThat(workedShifts(employee)).isEqualTo(2);
    }

    @Test
    void create_withEndNotAfterStart_isBadRequestAndStoresNothing() throws Exception {
        Map<String, Object> body = body();
        body.put("start", END);
        body.put("end", START);

        mockMvc.perform(post("/shift-worked/create").with(as(asUser(employee)))
                        .contentType(APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest());

        assertThat(workedShifts(employee)).isZero();
    }

    @Test
    void create_withInvalidFields_isBadRequest() throws Exception {
        Map<String, Object> shortName = body();
        shortName.put("shiftName", "ab");
        Map<String, Object> htmlName = body();
        htmlName.put("shiftName", "<i>night</i>");
        Map<String, Object> negativeBreak = body();
        negativeBreak.put("breakTime", -1);
        Map<String, Object> negativeExtra = body();
        negativeExtra.put("extraHours", new BigDecimal("-0.5"));
        Map<String, Object> missingExtra = body();
        missingExtra.remove("extraHours");
        Map<String, Object> missingStart = body();
        missingStart.remove("start");

        for (Map<String, Object> invalid : java.util.List.of(shortName, htmlName, negativeBreak, negativeExtra, missingExtra, missingStart)) {
            mockMvc.perform(post("/shift-worked/create").with(as(asUser(employee)))
                            .contentType(APPLICATION_JSON).content(json(invalid)))
                    .andExpect(status().isBadRequest());
        }
        assertThat(workedShifts(employee)).isZero();
    }

    @Test
    void create_byAUserWithoutCompany_isNotFound() throws Exception {
        mockMvc.perform(post("/shift-worked/create").with(as(asUser(companyless)))
                        .contentType(APPLICATION_JSON).content(json(body())))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(post("/shift-worked/create").contentType(APPLICATION_JSON).content(json(body())))
                .andExpect(status().isUnauthorized());
    }
}
