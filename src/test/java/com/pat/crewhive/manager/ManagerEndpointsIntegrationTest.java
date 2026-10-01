package com.pat.crewhive.manager;

import com.pat.crewhive.company.Company;
import com.pat.crewhive.support.AbstractEndpointIntegrationTest;
import com.pat.crewhive.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint tests for {@code /manager/*}: role management and work info of the employees of the
 * caller's company. The company always comes from the token, never from the request.
 */
class ManagerEndpointsIntegrationTest extends AbstractEndpointIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    private Company alpha;
    private Company beta;
    private User managerAlpha;
    private User employeeAlpha;
    private User employeeBeta;

    @BeforeEach
    void setUp() {
        alpha = newCompany("alpha");
        beta = newCompany("beta");
        managerAlpha = newUser("manager@alpha.test", alpha);
        employeeAlpha = newUser("employee@alpha.test", alpha);
        employeeBeta = newUser("employee@beta.test", beta);
    }

    private int roleCount(String name, Company company) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM role WHERE role_name = ? AND company_id = ? AND active = true", Integer.class,
                name, company.getCompanyId());
    }

    // ------------------------------------------------------------------ POST /create-role

    @Test
    void createRole_asManager_createsTheRoleInTheCallersCompanyOnly() throws Exception {
        mockMvc.perform(post("/manager/create-role").with(as(asManager(managerAlpha)))
                        .contentType(MediaType.TEXT_PLAIN).content("cashier"))
                .andExpect(status().isOk());

        assertThat(roleCount("ROLE_CASHIER", alpha)).isEqualTo(1);
        assertThat(roleCount("ROLE_CASHIER", beta)).isZero();
    }

    @Test
    void createRole_duplicate_isConflict() throws Exception {
        roleRepository.saveAndFlush(new Role("ROLE_CASHIER", alpha));

        mockMvc.perform(post("/manager/create-role").with(as(asManager(managerAlpha)))
                        .contentType(MediaType.TEXT_PLAIN).content("cashier"))
                .andExpect(status().isConflict());
    }

    @Test
    void createRole_sameNameInAnotherCompany_isAllowed() throws Exception {
        roleRepository.saveAndFlush(new Role("ROLE_CASHIER", beta));

        mockMvc.perform(post("/manager/create-role").with(as(asManager(managerAlpha)))
                        .contentType(MediaType.TEXT_PLAIN).content("cashier"))
                .andExpect(status().isOk());
    }

    @Test
    void createRole_withAReservedName_isBadRequest() throws Exception {
        mockMvc.perform(post("/manager/create-role").with(as(asManager(managerAlpha)))
                        .contentType(MediaType.TEXT_PLAIN).content("manager"))
                .andExpect(status().isBadRequest());

        assertThat(roleCount("ROLE_MANAGER", alpha)).isZero();
    }

    @Test
    void createRole_asPlainUser_isForbidden() throws Exception {
        mockMvc.perform(post("/manager/create-role").with(as(asUser(employeeAlpha)))
                        .contentType(MediaType.TEXT_PLAIN).content("cashier"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createRole_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(post("/manager/create-role").contentType(MediaType.TEXT_PLAIN).content("cashier"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ PATCH /update-user-role

    @Test
    void updateUserRole_assignsTheCompanyRoleToTheEmployee() throws Exception {
        roleRepository.saveAndFlush(new Role("ROLE_CASHIER", alpha));

        mockMvc.perform(patch("/manager/update-user-role").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("newRole", "cashier", "userId", employeeAlpha.getUserId()))))
                .andExpect(status().isOk());

        Integer assigned = jdbc.queryForObject(
                "SELECT count(*) FROM user_role ur JOIN role r ON r.role_id = ur.role_id "
                        + "WHERE ur.user_id = ? AND r.role_name = 'ROLE_CASHIER' AND ur.active = true",
                Integer.class, employeeAlpha.getUserId());
        assertThat(assigned).isEqualTo(1);
    }

    @Test
    void updateUserRole_ofAnEmployeeOfAnotherCompany_isNotFoundAndAssignsNothing() throws Exception {
        roleRepository.saveAndFlush(new Role("ROLE_CASHIER", alpha));

        mockMvc.perform(patch("/manager/update-user-role").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("newRole", "cashier", "userId", employeeBeta.getUserId()))))
                .andExpect(status().isNotFound());

        Integer assigned = jdbc.queryForObject("SELECT count(*) FROM user_role WHERE user_id = ?", Integer.class, employeeBeta.getUserId());
        assertThat(assigned).isZero();
    }

    @Test
    void updateUserRole_withARoleOfAnotherCompany_isNotFound() throws Exception {
        roleRepository.saveAndFlush(new Role("ROLE_CASHIER", beta));

        mockMvc.perform(patch("/manager/update-user-role").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("newRole", "cashier", "userId", employeeAlpha.getUserId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUserRole_withAnUnknownRole_isNotFound() throws Exception {
        mockMvc.perform(patch("/manager/update-user-role").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("newRole", "ghost", "userId", employeeAlpha.getUserId()))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUserRole_withBlankRoleOrMissingUser_isBadRequest() throws Exception {
        mockMvc.perform(patch("/manager/update-user-role").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("newRole", " ", "userId", employeeAlpha.getUserId()))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(patch("/manager/update-user-role").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("newRole", "cashier"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserRole_asPlainUser_isForbidden() throws Exception {
        mockMvc.perform(patch("/manager/update-user-role").with(as(asUser(employeeAlpha)))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("newRole", "cashier", "userId", employeeAlpha.getUserId()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserRole_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(patch("/manager/update-user-role").contentType(APPLICATION_JSON)
                        .content(json(Map.of("newRole", "cashier", "userId", employeeAlpha.getUserId()))))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ PATCH /update-user-work-info

    private Map<String, Object> workInfo(UUID target) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("targetUserId", target);
        body.put("contractType", "PART_TIME_VERTICAL");
        body.put("workableHoursPerWeek", 24);
        body.put("overtimeHours", new BigDecimal("1.50"));
        body.put("vacationDaysAccumulated", new BigDecimal("10.00"));
        body.put("vacationDaysTaken", new BigDecimal("2.00"));
        body.put("leaveDaysAccumulated", new BigDecimal("5.00"));
        body.put("leaveDaysTaken", new BigDecimal("1.00"));
        return body;
    }

    @Test
    void updateWorkInfo_updatesTheEmployeeOfTheCallersCompany() throws Exception {
        mockMvc.perform(patch("/manager/update-user-work-info").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON).content(json(workInfo(employeeAlpha.getUserId()))))
                .andExpect(status().isOk());

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT contract_type, workable_hours_per_week, overtime_hours, vacation_days_accumulated "
                        + "FROM users WHERE user_id = ?", employeeAlpha.getUserId());
        assertThat(row.get("contract_type")).isEqualTo("PART_TIME_VERTICAL");
        assertThat(row.get("workable_hours_per_week")).isEqualTo(24);
        assertThat((BigDecimal) row.get("overtime_hours")).isEqualByComparingTo("1.50");
        assertThat((BigDecimal) row.get("vacation_days_accumulated")).isEqualByComparingTo("10.00");
    }

    @Test
    void updateWorkInfo_ofAnEmployeeOfAnotherCompany_isNotFoundAndChangesNothing() throws Exception {
        mockMvc.perform(patch("/manager/update-user-work-info").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON).content(json(workInfo(employeeBeta.getUserId()))))
                .andExpect(status().isNotFound());

        Integer hours = jdbc.queryForObject("SELECT workable_hours_per_week FROM users WHERE user_id = ?", Integer.class, employeeBeta.getUserId());
        assertThat(hours).isNotEqualTo(24);
    }

    @Test
    void updateWorkInfo_withNegativeValues_isBadRequest() throws Exception {
        Map<String, Object> body = workInfo(employeeAlpha.getUserId());
        body.put("overtimeHours", new BigDecimal("-1"));

        mockMvc.perform(patch("/manager/update-user-work-info").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWorkInfo_withUnknownContractType_isBadRequest() throws Exception {
        Map<String, Object> body = workInfo(employeeAlpha.getUserId());
        body.put("contractType", "SLAVE");

        mockMvc.perform(patch("/manager/update-user-work-info").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON).content(json(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateWorkInfo_asPlainUser_isForbidden() throws Exception {
        mockMvc.perform(patch("/manager/update-user-work-info").with(as(asUser(employeeAlpha)))
                        .contentType(APPLICATION_JSON).content(json(workInfo(employeeAlpha.getUserId()))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateWorkInfo_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(patch("/manager/update-user-work-info").contentType(APPLICATION_JSON)
                        .content(json(workInfo(employeeAlpha.getUserId()))))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ DELETE /delete-role/{roleName}

    @Test
    void deleteRole_unassignedRole_isSoftDeleted() throws Exception {
        roleRepository.saveAndFlush(new Role("ROLE_CASHIER", alpha));

        mockMvc.perform(delete("/manager/delete-role/{n}", "cashier").with(as(asManager(managerAlpha))))
                .andExpect(status().isOk());

        assertThat(roleCount("ROLE_CASHIER", alpha)).isZero();
    }

    @Test
    void deleteRole_assignedToAnEmployee_isConflictAndKeepsTheRole() throws Exception {
        Role role = roleRepository.saveAndFlush(new Role("ROLE_CASHIER", alpha));
        mockMvc.perform(patch("/manager/update-user-role").with(as(asManager(managerAlpha)))
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("newRole", "cashier", "userId", employeeAlpha.getUserId()))))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/manager/delete-role/{n}", "cashier").with(as(asManager(managerAlpha))))
                .andExpect(status().isConflict());

        assertThat(roleCount("ROLE_CASHIER", alpha)).isEqualTo(1);
    }

    @Test
    void deleteRole_ofAnotherCompany_isNotFoundAndKeepsTheRole() throws Exception {
        roleRepository.saveAndFlush(new Role("ROLE_CASHIER", beta));

        mockMvc.perform(delete("/manager/delete-role/{n}", "cashier").with(as(asManager(managerAlpha))))
                .andExpect(status().isNotFound());

        assertThat(roleCount("ROLE_CASHIER", beta)).isEqualTo(1);
    }

    @Test
    void deleteRole_unknownRole_isNotFound() throws Exception {
        mockMvc.perform(delete("/manager/delete-role/{n}", "ghost").with(as(asManager(managerAlpha))))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRole_asPlainUser_isForbidden() throws Exception {
        mockMvc.perform(delete("/manager/delete-role/{n}", "cashier").with(as(asUser(employeeAlpha))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteRole_withoutAuthentication_isUnauthorized() throws Exception {
        mockMvc.perform(delete("/manager/delete-role/{n}", "cashier")).andExpect(status().isUnauthorized());
    }
}
