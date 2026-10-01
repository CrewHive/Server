package com.pat.crewhive.shiftprogrammed;

import com.jayway.jsonpath.JsonPath;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.support.AbstractEndpointIntegrationTest;
import com.pat.crewhive.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint tests for {@code /shift-programmed/*}: happy path, validation, authentication, role and tenant isolation.
 */
class ShiftProgrammedEndpointsIntegrationTest extends AbstractEndpointIntegrationTest {

    // today 09:00-17:00 (server offset), so that the DAY/WEEK/MONTH periods all contain it
    private static final OffsetDateTime START = OffsetDateTime.now().withHour(9).withMinute(0).withSecond(0).withNano(0);
    private static final OffsetDateTime END = START.plusHours(8);

    private Company alpha;
    private Company beta;
    private User managerAlpha;
    private User employeeAlpha;
    private User managerBeta;
    private User employeeBeta;

    @BeforeEach
    void setUp() {
        alpha = newCompany("alpha");
        beta = newCompany("beta");
        managerAlpha = newUser("manager@alpha.test", alpha);
        employeeAlpha = newUser("employee@alpha.test", alpha);
        managerBeta = newUser("manager@beta.test", beta);
        employeeBeta = newUser("employee@beta.test", beta);
    }

    private Map<String, Object> createBody(Set<UUID> participants) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Morning shift");
        body.put("description", "desc");
        body.put("start", START);
        body.put("end", END);
        body.put("color", "FF0000");
        body.put("userId", participants);
        return body;
    }

    private UUID createShift(User manager, Set<UUID> participants) throws Exception {
        String response = mockMvc.perform(post("/shift-programmed/create").with(as(asManager(manager)))
                        .contentType(APPLICATION_JSON).content(json(createBody(participants))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(response, UUID.class);
    }

    private int activeLinks(UUID shiftId) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM shift_user WHERE shift_programmed_id = ? AND active = true", Integer.class, shiftId);
    }

    // ------------------------------------------------------------------ POST /create

    @Test
    void create_asManager_persistsTheShiftWithItsParticipants() throws Exception {
        UUID shiftId = createShift(managerAlpha, Set.of(employeeAlpha.getUserId()));

        String name = jdbc.queryForObject("SELECT shift_programmed_name FROM shift_programmed WHERE shift_programmed_id = ?", String.class, shiftId);
        UUID companyId = jdbc.queryForObject("SELECT company_id FROM shift_programmed WHERE shift_programmed_id = ?", UUID.class, shiftId);
        assertThat(name).isEqualTo("morning shift");
        assertThat(companyId).isEqualTo(alpha.getCompanyId());
        assertThat(activeLinks(shiftId)).isEqualTo(1);
    }

    @Test
    void create_withAParticipantOfAnotherCompany_isForbiddenAndPersistsNothing() throws Exception {
        mockMvc.perform(post("/shift-programmed/create").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON).content(json(createBody(Set.of(employeeBeta.getUserId())))))
                .andExpect(status().isForbidden());

        Integer shifts = jdbc.queryForObject("SELECT count(*) FROM shift_programmed", Integer.class);
        assertThat(shifts).isZero();
    }

    @Test
    void create_withStartAfterEnd_isBadRequest() throws Exception {
        Map<String, Object> body = createBody(Set.of());
        body.put("start", END);
        body.put("end", START);

        mockMvc.perform(post("/shift-programmed/create").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withInvalidFields_isBadRequest() throws Exception {
        Map<String, Object> shortName = createBody(Set.of());
        shortName.put("name", "ab");
        Map<String, Object> badColor = createBody(Set.of());
        badColor.put("color", "FFF");
        Map<String, Object> nullParticipants = createBody(Set.of());
        nullParticipants.put("userId", null);
        Map<String, Object> htmlName = createBody(Set.of());
        htmlName.put("name", "<b>shift</b>");

        for (Map<String, Object> body : java.util.List.of(shortName, badColor, nullParticipants, htmlName)) {
            mockMvc.perform(post("/shift-programmed/create").with(as(asManager(managerAlpha)))
                            .contentType(APPLICATION_JSON).content(json(body)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void create_asPlainUser_isForbidden() throws Exception {
        mockMvc.perform(post("/shift-programmed/create").with(as(asUser(employeeAlpha)))
                        .contentType(APPLICATION_JSON).content(json(createBody(Set.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(post("/shift-programmed/create").contentType(APPLICATION_JSON).content(json(createBody(Set.of()))))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ GET /period/{period}/user/{userId}

    @Test
    void getByUser_returnsTheShiftsOfAColleague() throws Exception {
        createShift(managerAlpha, Set.of(employeeAlpha.getUserId()));

        mockMvc.perform(get("/shift-programmed/period/{p}/user/{u}", "DAY", employeeAlpha.getUserId())
                        .with(as(asUser(employeeAlpha))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shifts", hasSize(1)));
    }

    @Test
    void getByUser_ofAUserOfAnotherCompany_isNotFound() throws Exception {
        mockMvc.perform(get("/shift-programmed/period/{p}/user/{u}", "DAY", employeeBeta.getUserId())
                        .with(as(asUser(employeeAlpha))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByUser_withAnUnknownPeriod_isBadRequest() throws Exception {
        mockMvc.perform(get("/shift-programmed/period/{p}/user/{u}", "DECADE", employeeAlpha.getUserId())
                        .with(as(asUser(employeeAlpha))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByUser_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/shift-programmed/period/{p}/user/{u}", "DAY", employeeAlpha.getUserId()))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ GET /period/{period}/{companyId}

    @Test
    void getByCompany_returnsTheShiftsOfTheCallersCompany() throws Exception {
        createShift(managerAlpha, Set.of(employeeAlpha.getUserId()));

        mockMvc.perform(get("/shift-programmed/period/{p}/{c}", "WEEK", alpha.getCompanyId())
                        .with(as(asUser(employeeAlpha))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shifts", hasSize(1)));
    }

    @Test
    void getByCompany_ofAnotherCompany_isForbidden() throws Exception {
        mockMvc.perform(get("/shift-programmed/period/{p}/{c}", "WEEK", beta.getCompanyId())
                        .with(as(asUser(employeeAlpha))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getByCompany_doesNotMixShiftsOfOtherCompanies() throws Exception {
        createShift(managerBeta, Set.of(employeeBeta.getUserId()));

        mockMvc.perform(get("/shift-programmed/period/{p}/{c}", "WEEK", alpha.getCompanyId())
                        .with(as(asUser(employeeAlpha))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shifts", hasSize(0)));
    }

    @Test
    void getByCompany_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/shift-programmed/period/{p}/{c}", "WEEK", alpha.getCompanyId()))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ GET /users/{shiftId}

    @Test
    void getUsers_ofAShiftOfTheCallersCompany_listsTheParticipants() throws Exception {
        UUID shiftId = createShift(managerAlpha, Set.of(employeeAlpha.getUserId()));

        mockMvc.perform(get("/shift-programmed/users/{id}", shiftId).with(as(asUser(employeeAlpha))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId").value(employeeAlpha.getUserId().toString()));
    }

    @Test
    void getUsers_ofAShiftOfAnotherCompany_isNotFound() throws Exception {
        UUID shiftId = createShift(managerBeta, Set.of(employeeBeta.getUserId()));

        mockMvc.perform(get("/shift-programmed/users/{id}", shiftId).with(as(asUser(employeeAlpha))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUsers_ofAnUnknownShift_isNotFound() throws Exception {
        mockMvc.perform(get("/shift-programmed/users/{id}", UUID.randomUUID()).with(as(asUser(employeeAlpha))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getUsers_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/shift-programmed/users/{id}", UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ PATCH /patch

    private Map<String, Object> patchBody(UUID shiftId, Set<UUID> participants) {
        Map<String, Object> body = createBody(participants);
        body.put("shiftProgrammedId", shiftId);
        body.put("name", "Renamed shift");
        return body;
    }

    @Test
    void patch_updatesTheShiftAndItsParticipants() throws Exception {
        UUID shiftId = createShift(managerAlpha, Set.of(employeeAlpha.getUserId()));
        User other = newUser("other@alpha.test", alpha);

        mockMvc.perform(patch("/shift-programmed/patch").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON).content(json(patchBody(shiftId, Set.of(other.getUserId())))))
                .andExpect(status().isOk());

        String name = jdbc.queryForObject("SELECT shift_programmed_name FROM shift_programmed WHERE shift_programmed_id = ?", String.class, shiftId);
        assertThat(name).isEqualTo("renamed shift");
        Integer otherLinked = jdbc.queryForObject(
                "SELECT count(*) FROM shift_user WHERE shift_programmed_id = ? AND user_id = ? AND active = true",
                Integer.class, shiftId, other.getUserId());
        Integer oldLinked = jdbc.queryForObject(
                "SELECT count(*) FROM shift_user WHERE shift_programmed_id = ? AND user_id = ? AND active = true",
                Integer.class, shiftId, employeeAlpha.getUserId());
        assertThat(otherLinked).isEqualTo(1);
        assertThat(oldLinked).isZero();
    }

    @Test
    void patch_ofAShiftOfAnotherCompany_isForbiddenAndChangesNothing() throws Exception {
        UUID shiftId = createShift(managerBeta, Set.of(employeeBeta.getUserId()));

        mockMvc.perform(patch("/shift-programmed/patch").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON).content(json(patchBody(shiftId, Set.of()))))
                .andExpect(status().isForbidden());

        String name = jdbc.queryForObject("SELECT shift_programmed_name FROM shift_programmed WHERE shift_programmed_id = ?", String.class, shiftId);
        assertThat(name).isEqualTo("morning shift");
    }

    @Test
    void patch_ofAnUnknownShift_isNotFound() throws Exception {
        mockMvc.perform(patch("/shift-programmed/patch").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON).content(json(patchBody(UUID.randomUUID(), Set.of()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void patch_withStartAfterEnd_isBadRequest() throws Exception {
        UUID shiftId = createShift(managerAlpha, Set.of());
        Map<String, Object> body = patchBody(shiftId, Set.of());
        body.put("start", END);
        body.put("end", START);

        mockMvc.perform(patch("/shift-programmed/patch").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patch_asPlainUser_isForbidden() throws Exception {
        mockMvc.perform(patch("/shift-programmed/patch").with(as(asUser(employeeAlpha)))
                        .contentType(APPLICATION_JSON).content(json(patchBody(UUID.randomUUID(), Set.of()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void patch_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(patch("/shift-programmed/patch").contentType(APPLICATION_JSON)
                        .content(json(patchBody(UUID.randomUUID(), Set.of()))))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ DELETE /delete/{shiftId}

    @Test
    void delete_softDeletesTheShift() throws Exception {
        UUID shiftId = createShift(managerAlpha, Set.of(employeeAlpha.getUserId()));

        mockMvc.perform(delete("/shift-programmed/delete/{id}", shiftId).with(as(asManager(managerAlpha))))
                .andExpect(status().isOk());

        Boolean active = jdbc.queryForObject("SELECT active FROM shift_programmed WHERE shift_programmed_id = ?", Boolean.class, shiftId);
        assertThat(active).isFalse();
    }

    @Test
    void delete_ofAShiftOfAnotherCompany_isForbiddenAndKeepsIt() throws Exception {
        UUID shiftId = createShift(managerBeta, Set.of());

        mockMvc.perform(delete("/shift-programmed/delete/{id}", shiftId).with(as(asManager(managerAlpha))))
                .andExpect(status().isForbidden());

        Boolean active = jdbc.queryForObject("SELECT active FROM shift_programmed WHERE shift_programmed_id = ?", Boolean.class, shiftId);
        assertThat(active).isTrue();
    }

    @Test
    void delete_ofAnUnknownShift_isNotFound() throws Exception {
        mockMvc.perform(delete("/shift-programmed/delete/{id}", UUID.randomUUID()).with(as(asManager(managerAlpha))))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_asPlainUser_isForbidden() throws Exception {
        mockMvc.perform(delete("/shift-programmed/delete/{id}", UUID.randomUUID()).with(as(asUser(employeeAlpha))))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(delete("/shift-programmed/delete/{id}", UUID.randomUUID())).andExpect(status().isUnauthorized());
    }
}
