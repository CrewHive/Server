package com.pat.crewhive.event;

import com.pat.crewhive.company.Company;
import com.pat.crewhive.company.CompanyRepository;
import com.pat.crewhive.company.CompanyType;
import com.pat.crewhive.security.CustomUserDetails;
import com.pat.crewhive.support.AbstractIntegrationTest;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for the H7 invitation flow: an invitee of a PRIVATE event is
 * PENDING, sees the event only through {@code GET /event/invitations} until they accept, and a
 * decline is not undone by inviting them again.
 */
class EventInvitationIntegrationTest extends AbstractIntegrationTest {

    // Far enough in the future for the "invitations not ended yet" filter.
    private static final OffsetDateTime START = OffsetDateTime.now().plusDays(30);
    private static final OffsetDateTime END = START.plusHours(1);

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper objectMapper;

    private Company companyA;
    private Company companyB;
    private User creator;
    private User invitee;
    private User bystander; // company A, never invited
    private User outsider;  // company B

    @BeforeEach
    void setUp() {
        // See EventAuthorizationIntegrationTest: mirrors the real registration flow (no UserPreferences).
        jdbc.execute("ALTER TABLE users ALTER COLUMN user_user_id DROP NOT NULL");

        jdbc.execute("DELETE FROM event_users");
        jdbc.execute("DELETE FROM event");
        jdbc.execute("DELETE FROM user_role");
        jdbc.execute("DELETE FROM users");
        jdbc.execute("DELETE FROM company");

        jdbc.execute("INSERT INTO event_type (id, name, active) VALUES "
                + "(1, 'PUBLIC', true), (2, 'PRIVATE', true) ON CONFLICT (id) DO NOTHING");

        companyA = newCompany("Alpha SpA");
        companyB = newCompany("Beta SpA");
        creator = newUser("creator@alpha.test", companyA);
        invitee = newUser("invitee@alpha.test", companyA);
        bystander = newUser("bystander@alpha.test", companyA);
        outsider = newUser("outsider@beta.test", companyB);
    }

    @Test
    void invitee_seesEventOnlyInInvitations_untilAccepting() throws Exception {
        UUID eventId = createPrivateEvent(Set.of(invitee.getUserId()));

        mockMvc.perform(get("/event/user/{id}", invitee.getUserId()).with(as(asInvitee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/event/invitations").with(as(asInvitee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventId").value(eventId.toString()));

        respond(eventId, asInvitee(), true).andExpect(status().isOk());

        assertThat(statusOf(eventId, invitee)).isEqualTo("ACCEPTED");
        mockMvc.perform(get("/event/user/{id}", invitee.getUserId()).with(as(asInvitee())))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/event/invitations").with(as(asInvitee())))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void creator_isAcceptedAndInvitee_isPending_atCreation() throws Exception {
        UUID eventId = createPrivateEvent(Set.of(invitee.getUserId()));

        assertThat(statusOf(eventId, creator)).isEqualTo("ACCEPTED");
        assertThat(statusOf(eventId, invitee)).isEqualTo("PENDING");
    }

    @Test
    void decline_removesEventFromAgenda_andReinvitingDoesNotResetIt() throws Exception {
        UUID eventId = createPrivateEvent(Set.of(invitee.getUserId()));

        respond(eventId, asInvitee(), false).andExpect(status().isOk());
        assertThat(statusOf(eventId, invitee)).isEqualTo("DECLINED");

        mockMvc.perform(get("/event/user/{id}", invitee.getUserId()).with(as(asInvitee())))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/event/invitations").with(as(asInvitee())))
                .andExpect(jsonPath("$", hasSize(0)));

        // the creator lists the invitee again in a patch: the decline must stay
        String body = objectMapper.writeValueAsString(new PatchEventDTO(
                eventId, "Team Meeting", "desc", START, END, "00FF00", EventType.PRIVATE,
                Set.of(creator.getUserId(), invitee.getUserId())));
        mockMvc.perform(patch("/event/patch").contentType(APPLICATION_JSON).content(body).with(as(asCreator())))
                .andExpect(status().isOk());

        assertThat(statusOf(eventId, invitee)).isEqualTo("DECLINED");
    }

    @Test
    void respond_byUserOfAnotherCompany_isForbidden() throws Exception {
        UUID eventId = createPrivateEvent(Set.of(invitee.getUserId()));

        respond(eventId, principal(outsider.getUserId(), companyB.getCompanyId(), "ROLE_USER"), true)
                .andExpect(status().isForbidden());
    }

    @Test
    void respond_byColleagueWhoWasNotInvited_isNotFound() throws Exception {
        UUID eventId = createPrivateEvent(Set.of(invitee.getUserId()));

        respond(eventId, principal(bystander.getUserId(), companyA.getCompanyId(), "ROLE_USER"), true)
                .andExpect(status().isNotFound());
    }

    @Test
    void respond_creatorDecliningOwnEvent_isBadRequest() throws Exception {
        UUID eventId = createPrivateEvent(Set.of(invitee.getUserId()));

        respond(eventId, asCreator(), false).andExpect(status().isBadRequest());
        assertThat(statusOf(eventId, creator)).isEqualTo("ACCEPTED");
    }

    @Test
    void create_withMoreThan50Participants_isBadRequest() throws Exception {
        Set<UUID> tooMany = new HashSet<>();
        for (int i = 0; i < 51; i++) tooMany.add(UUID.randomUUID());
        String body = objectMapper.writeValueAsString(new CreateEventDTO(
                "Team Meeting", "desc", START, END, "FF0000", EventType.PRIVATE, tooMany));

        mockMvc.perform(post("/event/create").contentType(APPLICATION_JSON).content(body).with(as(asCreator())))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private CustomUserDetails asCreator() {
        return principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER");
    }

    private CustomUserDetails asInvitee() {
        return principal(invitee.getUserId(), companyA.getCompanyId(), "ROLE_USER");
    }

    private UUID createPrivateEvent(Set<UUID> participantIds) throws Exception {
        String body = objectMapper.writeValueAsString(new CreateEventDTO(
                "Team Meeting", "desc", START, END, "FF0000", EventType.PRIVATE, participantIds));

        String response = mockMvc.perform(post("/event/create")
                        .contentType(APPLICATION_JSON).content(body).with(as(asCreator())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(response, UUID.class);
    }

    private org.springframework.test.web.servlet.ResultActions respond(UUID eventId, CustomUserDetails caller, boolean accepted) throws Exception {
        return mockMvc.perform(post("/event/{id}/respond", eventId)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("accepted", accepted)))
                .with(as(caller)));
    }

    private String statusOf(UUID eventId, User user) {
        return jdbc.queryForObject(
                "SELECT status FROM event_users WHERE event_id = ? AND user_id = ?", String.class, eventId, user.getUserId());
    }

    private Company newCompany(String name) {
        Company company = new Company();
        company.setName(name);
        company.setCompanyType(CompanyType.OTHER);
        return companyRepository.saveAndFlush(company);
    }

    private User newUser(String email, Company company) {
        User user = new User(email, "First", "Last", "hashed-pwd");
        user.setCompany(company);
        user.setWorking(true);
        return userRepository.saveAndFlush(user);
    }
}
