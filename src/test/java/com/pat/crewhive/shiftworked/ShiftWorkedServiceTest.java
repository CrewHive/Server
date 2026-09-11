package com.pat.crewhive.shiftworked;

import com.pat.crewhive.common.StringUtils;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ShiftWorkedService}.
 */
@ExtendWith(MockitoExtension.class)
class ShiftWorkedServiceTest {

    @Mock
    private ShiftWorkedRepository repo;
    @Mock
    private StringUtils stringUtils;
    @Mock
    private UserService userService;

    private ShiftWorkedService shiftWorkedService;

    @BeforeEach
    void setUp() {
        shiftWorkedService = new ShiftWorkedService(repo, stringUtils, userService);
    }

    private User buildUser(UUID userId, BigDecimal overtime) {
        User user = new User("user@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(user, "userId", userId);
        user.setOvertimeHours(overtime);
        user.setCompany(buildCompany());
        return user;
    }

    private Company buildCompany() {
        Company company = new Company();
        ReflectionTestUtils.setField(company, "companyId", UUID.randomUUID());
        return company;
    }

    private CreateShiftWorkedDTO dto(BigDecimal extraHours) {
        return new CreateShiftWorkedDTO(
                "Turno mattina",
                OffsetDateTime.parse("2026-08-21T09:00:00Z"),
                OffsetDateTime.parse("2026-08-21T17:00:00Z"),
                30,
                extraHours
        );
    }

    @Test
    void createShiftWorked_addsExtraHoursToTheGivenUserAndPersists() {
        UUID callerId = UUID.randomUUID();
        User user = buildUser(callerId, new BigDecimal("2.00"));
        when(userService.getUserById(callerId)).thenReturn(user);
        when(stringUtils.normalizeString(anyString())).thenAnswer(inv -> inv.getArgument(0));

        shiftWorkedService.createShiftWorked(dto(new BigDecimal("1.50")), callerId);

        assertThat(user.getOvertimeHours()).isEqualByComparingTo("3.50");
        verify(userService).updateUser(user);

        ArgumentCaptor<ShiftWorked> saved = ArgumentCaptor.forClass(ShiftWorked.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getExtraHours()).isEqualByComparingTo("1.50");
    }

    @Test
    void createShiftWorked_resolvesTheUserFromThePassedIdOnly() {
        UUID callerId = UUID.randomUUID();
        User user = buildUser(callerId, BigDecimal.ZERO);
        when(userService.getUserById(callerId)).thenReturn(user);
        when(stringUtils.normalizeString(anyString())).thenAnswer(inv -> inv.getArgument(0));

        shiftWorkedService.createShiftWorked(dto(BigDecimal.ZERO), callerId);

        verify(userService).getUserById(callerId);
    }

    @Test
    void createShiftWorked_userHasNoCompany_throwsResourceNotFoundException() {
        UUID callerId = UUID.randomUUID();
        User user = buildUser(callerId, BigDecimal.ZERO);
        user.setCompany(null);
        when(userService.getUserById(callerId)).thenReturn(user);

        assertThatThrownBy(() -> shiftWorkedService.createShiftWorked(dto(BigDecimal.ZERO), callerId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
