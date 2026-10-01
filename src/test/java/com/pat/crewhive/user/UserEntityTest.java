package com.pat.crewhive.user;

import com.pat.crewhive.company.Company;
import com.pat.crewhive.event.EventUsers;
import com.pat.crewhive.manager.Role;
import com.pat.crewhive.manager.UserRole;
import com.pat.crewhive.shiftprogrammed.ShiftUser;
import com.pat.crewhive.shiftworked.ShiftWorked;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link User} (constructor defaults, accessors, role management) and
 * {@link UserPreferences}/{@link ContractType}. The effective role names have their own test.
 */
class UserEntityTest {

    private User user() {
        return new User("mario.rossi@example.com", "Mario", "Rossi", "encoded-pwd");
    }

    @Test
    void constructor_setsTheIdentityAndZeroedBalances() {
        User user = user();

        assertThat(user.getEmail()).isEqualTo("mario.rossi@example.com");
        assertThat(user.getFirstName()).isEqualTo("Mario");
        assertThat(user.getLastName()).isEqualTo("Rossi");
        assertThat(user.getPassword()).isEqualTo("encoded-pwd");
        assertThat(user.isWorking()).isFalse();
        assertThat(user.getWorkableHoursPerWeek()).isZero();
        assertThat(user.getOvertimeHours()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(user.getVacationDaysAccumulated()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(user.getVacationDaysTaken()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(user.getLeaveDaysAccumulated()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(user.getLeaveDaysTaken()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(user.getUserId()).isNull();
        assertThat(user.getCompany()).isNull();
        assertThat(user.getRoles()).isEmpty();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    void settersReplaceTheValues() {
        User user = new User();
        Company company = new Company();
        Set<EventUsers> events = new HashSet<>();
        Set<ShiftUser> shifts = new HashSet<>();
        Set<ShiftWorked> worked = new HashSet<>();
        Set<UserRole> roles = new HashSet<>();

        user.setEmail("new@example.com");
        user.setFirstName("Luigi");
        user.setLastName("Verdi");
        user.setPassword("pwd2");
        user.setCompany(company);
        user.setWorking(true);
        user.setContractType(ContractType.AT_CALL);
        user.setWorkableHoursPerWeek(24);
        user.setOvertimeHours(new BigDecimal("1.5"));
        user.setVacationDaysAccumulated(new BigDecimal("10"));
        user.setVacationDaysTaken(new BigDecimal("2"));
        user.setLeaveDaysAccumulated(new BigDecimal("5"));
        user.setLeaveDaysTaken(new BigDecimal("1"));
        user.setPersonalEvents(events);
        user.setShiftUsers(shifts);
        user.setShiftWorked(worked);
        user.setRoles(roles);

        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getFirstName()).isEqualTo("Luigi");
        assertThat(user.getLastName()).isEqualTo("Verdi");
        assertThat(user.getPassword()).isEqualTo("pwd2");
        assertThat(user.getCompany()).isSameAs(company);
        assertThat(user.isWorking()).isTrue();
        assertThat(user.getContractType()).isEqualTo(ContractType.AT_CALL);
        assertThat(user.getWorkableHoursPerWeek()).isEqualTo(24);
        assertThat(user.getOvertimeHours()).isEqualByComparingTo("1.5");
        assertThat(user.getVacationDaysAccumulated()).isEqualByComparingTo("10");
        assertThat(user.getVacationDaysTaken()).isEqualByComparingTo("2");
        assertThat(user.getLeaveDaysAccumulated()).isEqualByComparingTo("5");
        assertThat(user.getLeaveDaysTaken()).isEqualByComparingTo("1");
        assertThat(user.getPersonalEvents()).isSameAs(events);
        assertThat(user.getShiftUsers()).isSameAs(shifts);
        assertThat(user.getShiftWorked()).isSameAs(worked);
        assertThat(user.getRoles()).isSameAs(roles);
    }

    // ---- roles ----

    @Test
    void addRole_linksBothSides_andIgnoresADuplicate() {
        User user = user();
        Role role = new Role("ROLE_USER");

        user.addRole(role);
        user.addRole(role);

        assertThat(user.getRoles()).hasSize(1);
        assertThat(user.getRoles().iterator().next().getRole()).isSameAs(role);
        assertThat(role.getUsers()).hasSize(1);
    }

    @Test
    void removeRole_unlinksBothSides_andKeepsTheOthers() {
        User user = user();
        Role keep = new Role("ROLE_USER");
        Role drop = new Role("ROLE_CASHIER");
        user.addRole(keep);
        user.addRole(drop);

        user.removeRole(drop);

        assertThat(user.getRoles()).extracting(UserRole::getRole).containsExactly(keep);
        assertThat(drop.getUsers()).isEmpty();
    }

    @Test
    void removeRole_ofARoleTheUserDoesNotHave_changesNothing() {
        User user = user();
        Role role = new Role("ROLE_USER");
        user.addRole(role);

        user.removeRole(new Role("ROLE_OTHER"));

        assertThat(user.getRoles()).hasSize(1);
    }

    // ---- preferences ----

    @Test
    void setUserPreferences_linksTheBackReference() {
        User user = user();
        UserPreferences preferences = new UserPreferences();

        user.setUserPreferences(preferences);

        assertThat(user.getUserPreferences()).isSameAs(preferences);
        assertThat(preferences.getUser()).isSameAs(user);
    }

    @Test
    void setUserPreferences_withNull_clearsThem() {
        User user = user();
        user.setUserPreferences(new UserPreferences());

        user.setUserPreferences(null);

        assertThat(user.getUserPreferences()).isNull();
    }

    @Test
    void userPreferences_haveSensibleDefaults_andSettersReplaceThem() {
        UserPreferences preferences = new UserPreferences();

        assertThat(preferences.getTimeZone()).isEqualTo("UTC");
        assertThat(preferences.getLocale()).isEqualTo("it-IT");
        assertThat(preferences.getTheme()).isEqualTo("system");

        preferences.setTimeZone("Europe/Rome");
        preferences.setLocale("en-GB");
        preferences.setTheme("dark");

        assertThat(preferences.getTimeZone()).isEqualTo("Europe/Rome");
        assertThat(preferences.getLocale()).isEqualTo("en-GB");
        assertThat(preferences.getTheme()).isEqualTo("dark");
    }

    @Test
    void contractTypeLabels() {
        assertThat(ContractType.FULL_TIME.getLabel()).isEqualTo("Full time");
        assertThat(ContractType.PART_TIME_HORIZONTAL.getLabel()).isEqualTo("Part time horizontal");
        assertThat(ContractType.PART_TIME_VERTICAL.getLabel()).isEqualTo("Part time vertical");
        assertThat(ContractType.AT_CALL.getLabel()).isEqualTo("At-Call");
    }
}
