package com.pat.crewhive.event;

import com.pat.crewhive.company.Company;
import com.pat.crewhive.support.AbstractEndpointIntegrationTest;
import com.pat.crewhive.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint tests for the event read paths not covered by the invitation/authorization tests:
 * {@code GET /event/public/{period}} and {@code GET /event/{period}/user/{userId}}.
 */
class EventReadEndpointsIntegrationTest extends AbstractEndpointIntegrationTest {

    // today 10:00-11:00 (server offset), so that DAY/WEEK/MONTH all contain it
    private static final OffsetDateTime START = OffsetDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);
    private static final OffsetDateTime END = START.plusHours(1);

    private Company alpha;
    private Company beta;
    private User managerAlpha;
    private User employeeAlpha;
    private User colleagueAlpha;
    private User employeeBeta;

    @BeforeEach
    void setUp() {
        alpha = newCompany("alpha");
        beta = newCompany("beta");
        managerAlpha = newUser("manager@alpha.test", alpha);
        employeeAlpha = newUser("employee@alpha.test", alpha);
        colleagueAlpha = newUser("colleague@alpha.test", alpha);
        employeeBeta = newUser("employee@beta.test", beta);
    }

    private void createEvent(User creator, boolean manager, EventType type, Set<java.util.UUID> participants) throws Exception {
        mockMvc.perform(post("/event/create").with(as(manager ? asManager(creator) : asUser(creator)))
                        .contentType(APPLICATION_JSON)
                        .content(json(new CreateEventDTO("Team Meeting", "desc", START, END, "FF0000", type, participants))))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------ GET /public/{period}

    @Test
    void publicEvents_areVisibleToEveryoneInTheCompanyAndOnlyToThem() throws Exception {
        createEvent(managerAlpha, true, EventType.PUBLIC, Set.of());

        mockMvc.perform(get("/event/public/{p}", "MONTH").with(as(asUser(employeeAlpha))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/event/public/{p}", "MONTH").with(as(asUser(employeeBeta))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void publicEvents_doNotIncludePrivateOnes() throws Exception {
        createEvent(employeeAlpha, false, EventType.PRIVATE, Set.of(employeeAlpha.getUserId()));

        mockMvc.perform(get("/event/public/{p}", "MONTH").with(as(asUser(employeeAlpha))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void publicEvents_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/event/public/{p}", "MONTH")).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ GET /{period}/user/{userId}

    @Test
    void userEventsByPeriod_ownPrivateEvent_isListed() throws Exception {
        createEvent(employeeAlpha, false, EventType.PRIVATE, Set.of(employeeAlpha.getUserId()));

        mockMvc.perform(get("/event/{p}/user/{u}", "MONTH", employeeAlpha.getUserId()).with(as(asUser(employeeAlpha))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void userEventsByPeriod_privateEventOfAColleague_isNotShown() throws Exception {
        createEvent(employeeAlpha, false, EventType.PRIVATE, Set.of(employeeAlpha.getUserId()));

        mockMvc.perform(get("/event/{p}/user/{u}", "MONTH", employeeAlpha.getUserId()).with(as(asUser(colleagueAlpha))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void userEventsByPeriod_ofAUserOfAnotherCompany_isNotFound() throws Exception {
        mockMvc.perform(get("/event/{p}/user/{u}", "MONTH", employeeBeta.getUserId()).with(as(asUser(employeeAlpha))))
                .andExpect(status().isNotFound());
    }

    @Test
    void userEventsByPeriod_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(get("/event/{p}/user/{u}", "MONTH", employeeAlpha.getUserId())).andExpect(status().isUnauthorized());
    }
}
