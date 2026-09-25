package com.pat.crewhive.shifttemplate;

import com.pat.crewhive.company.Company;
import com.pat.crewhive.company.CompanyRepository;
import com.pat.crewhive.company.CompanyType;
import com.pat.crewhive.support.AbstractIntegrationTest;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for the H4 fix: {@code /shift-template/*} is scoped to the company
 * of the authenticated caller. The company is never taken from the request, so a manager of one
 * company cannot read, create, update or delete templates of another.
 */
class ShiftTemplateAuthorizationIntegrationTest extends AbstractIntegrationTest {

    private static final String NAME = "morning";

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ShiftTemplateRepository shiftTemplateRepository;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper objectMapper;

    private Company companyA;
    private Company companyB;
    private User managerA;
    private User managerB;
    private User userA;
    private User companyless;
    private ShiftTemplate templateA;

    @BeforeEach
    void setUp() {
        // See EventAuthorizationIntegrationTest: mirrors the real registration flow (no UserPreferences).
        jdbc.execute("ALTER TABLE users ALTER COLUMN user_user_id DROP NOT NULL");

        // Targeted deletes in FK order (leftovers of other integration test classes included).
        jdbc.execute("DELETE FROM shift_template");
        jdbc.execute("DELETE FROM event_users");
        jdbc.execute("DELETE FROM event");
        jdbc.execute("DELETE FROM user_role");
        jdbc.execute("DELETE FROM users");
        jdbc.execute("DELETE FROM company");

        companyA = newCompany("Alpha SpA");
        companyB = newCompany("Beta SpA");
        managerA = newUser("manager@alpha.test", companyA);
        userA = newUser("user@alpha.test", companyA);
        managerB = newUser("manager@beta.test", companyB);
        companyless = newUser("nocompany@example.test", null);

        templateA = shiftTemplateRepository.saveAndFlush(new ShiftTemplate(
                null, NAME, OffsetTime.parse("08:00:00Z"), OffsetTime.parse("16:00:00Z"),
                "desc", "FF0000", companyA));
    }

    // ------------------------------------------------------------------
    // get
    // ------------------------------------------------------------------

    @Test
    void get_managerOfOwningCompany_isAllowed() throws Exception {
        mockMvc.perform(get("/shift-template/get/{name}", NAME)
                        .with(as(principal(managerA.getUserId(), companyA.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftId", is(templateA.getShiftId().toString())))
                .andExpect(jsonPath("$.shiftName", is(NAME)))
                .andExpect(jsonPath("$.company").doesNotExist());
    }

    @Test
    void get_managerOfAnotherCompany_isNotFound() throws Exception {
        mockMvc.perform(get("/shift-template/get/{name}", NAME)
                        .with(as(principal(managerB.getUserId(), companyB.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_managerWithoutCompany_isForbidden() throws Exception {
        mockMvc.perform(get("/shift-template/get/{name}", NAME)
                        .with(as(principal(companyless.getUserId(), null, "ROLE_MANAGER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void get_plainUser_isForbidden() throws Exception {
        mockMvc.perform(get("/shift-template/get/{name}", NAME)
                        .with(as(principal(userA.getUserId(), companyA.getCompanyId(), "ROLE_USER"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void get_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/shift-template/get/{name}", NAME))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // create
    // ------------------------------------------------------------------

    @Test
    void create_usesCallerCompany_andIgnoresACompanyIdInTheBody() throws Exception {
        // Same name as templateA, but in another company: no conflict, and the smuggled companyId is ignored.
        String body = objectMapper.writeValueAsString(Map.of(
                "shiftName", NAME, "description", "desc", "color", "00FF00",
                "start", "09:00:00Z", "end", "17:00:00Z",
                "companyId", companyA.getCompanyId()));

        mockMvc.perform(post("/shift-template/create").contentType(APPLICATION_JSON).content(body)
                        .with(as(principal(managerB.getUserId(), companyB.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftName", is(NAME)));

        assertThat(templatesOf(companyA)).isEqualTo(1);
        assertThat(templatesOf(companyB)).isEqualTo(1);
    }

    @Test
    void create_duplicateInOwnCompany_isConflict() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "shiftName", NAME, "description", "desc", "color", "00FF00",
                "start", "09:00:00Z", "end", "17:00:00Z"));

        mockMvc.perform(post("/shift-template/create").contentType(APPLICATION_JSON).content(body)
                        .with(as(principal(managerA.getUserId(), companyA.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isConflict());
    }

    @Test
    void create_managerWithoutCompany_isForbidden() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "shiftName", "evening", "description", "desc", "color", "00FF00",
                "start", "09:00:00Z", "end", "17:00:00Z"));

        mockMvc.perform(post("/shift-template/create").contentType(APPLICATION_JSON).content(body)
                        .with(as(principal(companyless.getUserId(), null, "ROLE_MANAGER"))))
                .andExpect(status().isForbidden());

        assertThat(shiftTemplateRepository.count()).isEqualTo(1);
    }

    // ------------------------------------------------------------------
    // update
    // ------------------------------------------------------------------

    @Test
    void update_managerOfOwningCompany_isAllowed() throws Exception {
        mockMvc.perform(patch("/shift-template/update").contentType(APPLICATION_JSON)
                        .content(patchBody("afternoon", NAME))
                        .with(as(principal(managerA.getUserId(), companyA.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shiftName", is("afternoon")))
                .andExpect(jsonPath("$.company").doesNotExist());

        assertThat(currentName()).isEqualTo("afternoon");
    }

    @Test
    void update_managerOfAnotherCompany_isNotFound_andLeavesTemplateUntouched() throws Exception {
        mockMvc.perform(patch("/shift-template/update").contentType(APPLICATION_JSON)
                        .content(patchBody("hacked", NAME))
                        .with(as(principal(managerB.getUserId(), companyB.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isNotFound());

        assertThat(currentName()).isEqualTo(NAME);
    }

    @Test
    void update_managerWithoutCompany_isForbidden() throws Exception {
        mockMvc.perform(patch("/shift-template/update").contentType(APPLICATION_JSON)
                        .content(patchBody("hacked", NAME))
                        .with(as(principal(companyless.getUserId(), null, "ROLE_MANAGER"))))
                .andExpect(status().isForbidden());

        assertThat(currentName()).isEqualTo(NAME);
    }

    // ------------------------------------------------------------------
    // delete
    // ------------------------------------------------------------------

    @Test
    void delete_managerOfOwningCompany_softDeletes() throws Exception {
        mockMvc.perform(delete("/shift-template/delete/{name}", NAME)
                        .with(as(principal(managerA.getUserId(), companyA.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isOk());

        assertThat(shiftTemplateRepository.findById(templateA.getShiftId())).isEmpty(); // hidden by @SQLRestriction
        Boolean active = jdbc.queryForObject(
                "SELECT active FROM shift_template WHERE shift_id = ?", Boolean.class, templateA.getShiftId());
        assertThat(active).isFalse();
    }

    @Test
    void delete_managerOfAnotherCompany_isNotFound_andLeavesTemplateActive() throws Exception {
        mockMvc.perform(delete("/shift-template/delete/{name}", NAME)
                        .with(as(principal(managerB.getUserId(), companyB.getCompanyId(), "ROLE_MANAGER"))))
                .andExpect(status().isNotFound());

        assertThat(shiftTemplateRepository.findById(templateA.getShiftId())).isPresent();
    }

    @Test
    void delete_managerWithoutCompany_isForbidden() throws Exception {
        mockMvc.perform(delete("/shift-template/delete/{name}", NAME)
                        .with(as(principal(companyless.getUserId(), null, "ROLE_MANAGER"))))
                .andExpect(status().isForbidden());

        assertThat(shiftTemplateRepository.findById(templateA.getShiftId())).isPresent();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private String patchBody(String name, String oldName) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "shiftName", name, "description", "desc", "color", "00FF00",
                "start", "09:00:00Z", "end", "17:00:00Z", "oldShiftName", oldName));
    }

    private String currentName() {
        return jdbc.queryForObject("SELECT shift_name FROM shift_template WHERE shift_id = ?",
                String.class, templateA.getShiftId());
    }

    private int templatesOf(Company company) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM shift_template WHERE company_id = ? AND active", Integer.class, company.getCompanyId());
        return count == null ? 0 : count;
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
