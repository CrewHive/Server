package com.pat.crewhive.event;

import tools.jackson.databind.ObjectMapper;
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

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for the C2 fix: object-level authorization on
 * {@code POST /event/create}, {@code PATCH /event/patch} and {@code DELETE /event/delete/{id}}.
 *
 * <p>Rule under test: the caller must belong to the same company as the event's creator,
 * and must be either that creator or a {@code ROLE_MANAGER}. Participants may never come
 * from another company.
 */
class EventAuthorizationIntegrationTest extends AbstractIntegrationTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-09-01T09:00:00Z");
    private static final OffsetDateTime END = OffsetDateTime.parse("2026-09-01T10:00:00Z");

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper objectMapper;

    private Company companyA;
    private Company companyB;
    private User creator;      // company A, the one who creates the events under test
    private User colleague;    // company A, plain ROLE_USER, not the creator
    private User manager;      // company A, ROLE_MANAGER
    private User outsider;     // company B
    private User participantB; // company B, used to attempt cross-tenant participant injection

    @BeforeEach
    void setUp() {
        // On a pristine schema ddl-auto makes users.user_user_id NOT NULL (User.userPreferences is
        // optional=false), but nothing in the app ever creates a UserPreferences - AuthService.register
        // saves a User with no preferences. Relax it so seeding mirrors the real registration flow.
        jdbc.execute("ALTER TABLE users ALTER COLUMN user_user_id DROP NOT NULL");

        // Targeted deletes in FK order - NOT "TRUNCATE ... CASCADE", which would also wipe the
        // event_type seed (event_type has a deleted_by FK back to users).
        jdbc.execute("DELETE FROM event_users");
        jdbc.execute("DELETE FROM event");
        jdbc.execute("DELETE FROM user_role");
        jdbc.execute("DELETE FROM users");
        jdbc.execute("DELETE FROM company");

        // Ensure the event_type lookup rows exist (EventTypeInitializer seeds them at startup,
        // but re-assert here so the test is self-contained).
        jdbc.execute("INSERT INTO event_type (id, name, active) VALUES "
                + "(1, 'PUBLIC', true), (2, 'PRIVATE', true) ON CONFLICT (id) DO NOTHING");

        companyA = newCompany("Alpha SpA");
        companyB = newCompany("Beta SpA");
        creator = newUser("creator@alpha.test", companyA);
        colleague = newUser("colleague@alpha.test", companyA);
        manager = newUser("manager@alpha.test", companyA);
        outsider = newUser("outsider@beta.test", companyB);
        participantB = newUser("participant@beta.test", companyB);
    }

    // ------------------------------------------------------------------
    // create
    // ------------------------------------------------------------------

    @Test
    void create_setsAuthenticatedUserAsCreatorAndParticipant() throws Exception {
        UUID eventId = createEventAs(
                principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"),
                EventType.PRIVATE,
                Set.of(colleague.getUserId()));

        UUID storedCreator = jdbc.queryForObject(
                "SELECT creator_id FROM event WHERE event_id = ?", UUID.class, eventId);
        assertThat(storedCreator).isEqualTo(creator.getUserId());

        Integer participantCount = jdbc.queryForObject(
                "SELECT count(*) FROM event_users WHERE event_id = ? AND active", Integer.class, eventId);
        assertThat(participantCount).isEqualTo(2); // colleague + the creator, auto-added
    }

    @Test
    void create_rejectsParticipantFromAnotherCompany() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateEventDTO(
                "Cross tenant", "x", START, END, "FF0000", EventType.PRIVATE,
                Set.of(participantB.getUserId())));

        mockMvc.perform(post("/event/create").contentType(APPLICATION_JSON).content(body)
                        .with(as(principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"))))
                .andExpect(status().isForbidden());

        assertThat(eventRepository.count()).isZero();
    }

    @Test
    void create_rejectsPublicEventForNonManager() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateEventDTO(
                "Public", "x", START, END, "FF0000", EventType.PUBLIC, Set.of(creator.getUserId())));

        mockMvc.perform(post("/event/create").contentType(APPLICATION_JSON).content(body)
                        .with(as(principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_allowsPublicEventForManager() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateEventDTO(
                "Public", "x", START, END, "FF0000", EventType.PUBLIC, Set.of(creator.getUserId())));

        mockMvc.perform(post("/event/create").contentType(APPLICATION_JSON).content(body)
                        .with(as(principal(manager.getUserId(), companyA.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // patch
    // ------------------------------------------------------------------

    @Test
    void patch_rejectsCallerFromAnotherCompany_evenAsManager() throws Exception {
        UUID eventId = createEventAs(
                principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"),
                EventType.PRIVATE, Set.of());

        mockMvc.perform(patch("/event/patch").contentType(APPLICATION_JSON)
                        .content(patchBody(eventId, "Hacked", null))
                        .with(as(principal(outsider.getUserId(), companyB.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isForbidden());

        assertThat(currentName(eventId)).isEqualTo("team meeting");
    }

    @Test
    void patch_rejectsSameCompanyUserWhoIsNeitherCreatorNorManager() throws Exception {
        UUID eventId = createEventAs(
                principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"),
                EventType.PRIVATE, Set.of());

        mockMvc.perform(patch("/event/patch").contentType(APPLICATION_JSON)
                        .content(patchBody(eventId, "Changed", null))
                        .with(as(principal(colleague.getUserId(), companyA.getCompanyId(), "ROLE_USER"))))
                .andExpect(status().isForbidden());

        assertThat(currentName(eventId)).isEqualTo("team meeting");
    }

    @Test
    void patch_allowsTheCreator() throws Exception {
        UUID eventId = createEventAs(
                principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"),
                EventType.PRIVATE, Set.of());

        mockMvc.perform(patch("/event/patch").contentType(APPLICATION_JSON)
                        .content(patchBody(eventId, "Updated by creator", null))
                        .with(as(principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"))))
                .andExpect(status().isOk());

        assertThat(currentName(eventId)).isEqualTo("updated by creator");
    }

    @Test
    void patch_allowsAManagerOfTheSameCompany() throws Exception {
        UUID eventId = createEventAs(
                principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"),
                EventType.PRIVATE, Set.of());

        mockMvc.perform(patch("/event/patch").contentType(APPLICATION_JSON)
                        .content(patchBody(eventId, "Updated by manager", null))
                        .with(as(principal(manager.getUserId(), companyA.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isOk());

        assertThat(currentName(eventId)).isEqualTo("updated by manager");
    }

    @Test
    void patch_rejectsAddingAParticipantFromAnotherCompany() throws Exception {
        UUID eventId = createEventAs(
                principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"),
                EventType.PRIVATE, Set.of());

        mockMvc.perform(patch("/event/patch").contentType(APPLICATION_JSON)
                        .content(patchBody(eventId, "Team Meeting", Set.of(participantB.getUserId())))
                        .with(as(principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"))))
                .andExpect(status().isForbidden());

        Integer foreignLinks = jdbc.queryForObject(
                "SELECT count(*) FROM event_users WHERE event_id = ? AND user_id = ?",
                Integer.class, eventId, participantB.getUserId());
        assertThat(foreignLinks).isZero();
    }

    @Test
    void patch_requiresAuthentication() throws Exception {
        mockMvc.perform(patch("/event/patch").contentType(APPLICATION_JSON)
                        .content(patchBody(UUID.randomUUID(), "Nope", null)))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // delete
    // ------------------------------------------------------------------

    @Test
    void delete_rejectsCallerFromAnotherCompany() throws Exception {
        UUID eventId = createEventAs(
                principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"),
                EventType.PRIVATE, Set.of());

        mockMvc.perform(delete("/event/delete/{id}", eventId)
                        .with(as(principal(outsider.getUserId(), companyB.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isForbidden());

        assertThat(eventRepository.findById(eventId)).isPresent();
    }

    @Test
    void delete_rejectsSameCompanyUserWhoIsNeitherCreatorNorManager() throws Exception {
        UUID eventId = createEventAs(
                principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"),
                EventType.PRIVATE, Set.of());

        mockMvc.perform(delete("/event/delete/{id}", eventId)
                        .with(as(principal(colleague.getUserId(), companyA.getCompanyId(), "ROLE_USER"))))
                .andExpect(status().isForbidden());

        assertThat(eventRepository.findById(eventId)).isPresent();
    }

    @Test
    void delete_allowsTheCreator_andSoftDeletes() throws Exception {
        UUID eventId = createEventAs(
                principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"),
                EventType.PRIVATE, Set.of());

        mockMvc.perform(delete("/event/delete/{id}", eventId)
                        .with(as(principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"))))
                .andExpect(status().isOk());

        assertThat(eventRepository.findById(eventId)).isEmpty(); // hidden by @SQLRestriction
        Boolean active = jdbc.queryForObject(
                "SELECT active FROM event WHERE event_id = ?", Boolean.class, eventId);
        assertThat(active).isFalse();
    }

    @Test
    void delete_allowsAManagerOfTheSameCompany() throws Exception {
        UUID eventId = createEventAs(
                principal(creator.getUserId(), companyA.getCompanyId(), "ROLE_USER"),
                EventType.PRIVATE, Set.of());

        mockMvc.perform(delete("/event/delete/{id}", eventId)
                        .with(as(principal(manager.getUserId(), companyA.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isOk());

        assertThat(eventRepository.findById(eventId)).isEmpty();
    }

    @Test
    void delete_returnsNotFoundForUnknownEvent() throws Exception {
        mockMvc.perform(delete("/event/delete/{id}", UUID.randomUUID())
                        .with(as(principal(manager.getUserId(), companyA.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private UUID createEventAs(CustomUserDetails principal, EventType type, Set<UUID> participantIds) throws Exception {
        String body = objectMapper.writeValueAsString(new CreateEventDTO(
                "Team Meeting", "desc", START, END, "FF0000", type, participantIds));

        String response = mockMvc.perform(post("/event/create")
                        .contentType(APPLICATION_JSON).content(body).with(as(principal)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(response, UUID.class);
    }

    private String patchBody(UUID eventId, String name, Set<UUID> participantIds) throws Exception {
        return objectMapper.writeValueAsString(new PatchEventDTO(
                eventId, name, "desc", START, END, "00FF00", EventType.PRIVATE, participantIds));
    }

    private String currentName(UUID eventId) {
        return jdbc.queryForObject("SELECT name FROM event WHERE event_id = ?", String.class, eventId);
    }

    private Company newCompany(String name) {
        Company company = new Company();
        company.setName(name);
        company.setCompanyType(CompanyType.OTHER);
        return companyRepository.saveAndFlush(company);
    }

    private User newUser(String email, Company company) {
        // Mirrors AuthService.register: a User with no UserPreferences.
        User user = new User(email, "First", "Last", "hashed-pwd");
        user.setCompany(company);
        user.setWorking(true);
        return userRepository.saveAndFlush(user);
    }
}
