package com.pat.crewhive.shiftprogrammed;

import com.pat.crewhive.common.DateUtils;
import com.pat.crewhive.common.Period;
import com.pat.crewhive.common.StringUtils;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.company.CompanyService;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.user.User;
import com.pat.crewhive.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ShiftProgrammedService}.
 */
@ExtendWith(MockitoExtension.class)
class ShiftProgrammedServiceTest {

    @Mock
    private ShiftProgrammedRepository shiftProgrammedRepository;
    @Mock
    private ShiftUserRepository shiftUserRepository;
    @Mock
    private StringUtils stringUtils;
    @Mock
    private UserService userService;
    @Mock
    private DateUtils dateUtils;
    @Mock
    private CompanyService companyService;

    private ShiftProgrammedService shiftProgrammedService;

    @BeforeEach
    void setUp() {
        shiftProgrammedService = new ShiftProgrammedService(
                shiftProgrammedRepository, shiftUserRepository, stringUtils, userService, dateUtils, companyService
        );
    }

    private User buildUser(UUID userId, String firstName, String lastName) {
        User user = new User("user@example.com", firstName, lastName, "encoded-pwd");
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private ShiftProgrammed buildShiftWithUser(UUID userId) {
        ShiftProgrammed shift = new ShiftProgrammed();
        ReflectionTestUtils.setField(shift, "shiftProgrammedId", UUID.randomUUID());
        shift.setShiftName("Turno mattina");
        shift.setStart(OffsetDateTime.parse("2026-08-21T09:00:00Z"));
        shift.setEnd(OffsetDateTime.parse("2026-08-21T17:00:00Z"));
        shift.setColor("#0000FF");
        shift.addUser(buildUser(userId, "Luigi", "Verdi"));
        return shift;
    }

    @Test
    void getShiftsByPeriodAndUser_returnsShiftsMappedToOutputDTOs() {
        UUID userId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 8, 17);
        LocalDate to = LocalDate.of(2026, 8, 23);
        ShiftProgrammed shift = buildShiftWithUser(UUID.randomUUID());

        when(dateUtils.getStartDateForPeriod(Period.WEEK)).thenReturn(from);
        when(dateUtils.getEndDateForPeriod(Period.WEEK)).thenReturn(to);
        when(shiftProgrammedRepository.findByUserAndDateBetween(userId, from, to))
                .thenReturn(List.of(shift));

        ShiftProgrammedOutputDTO result = shiftProgrammedService.getShiftsByPeriodAndUser(Period.WEEK, userId);

        assertThat(result.shifts()).containsExactly(ShiftProgrammedItemDTO.from(shift));
    }

    @Test
    void getShiftsByPeriodAndCompany_returnsShiftsMappedToOutputDTOs() {
        UUID requesterUserId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 8, 17);
        LocalDate to = LocalDate.of(2026, 8, 23);
        ShiftProgrammed shift = buildShiftWithUser(UUID.randomUUID());
        Company company = new Company(companyId, "Acme", null, null, null);

        when(companyService.getCompanyByUserId(requesterUserId)).thenReturn(company);
        when(dateUtils.getStartDateForPeriod(Period.WEEK)).thenReturn(from);
        when(dateUtils.getEndDateForPeriod(Period.WEEK)).thenReturn(to);
        when(shiftProgrammedRepository.findByCompanyAndDateBetween(companyId, from, to))
                .thenReturn(List.of(shift));

        ShiftProgrammedOutputDTO result = shiftProgrammedService.getShiftsByPeriodAndCompany(Period.WEEK, requesterUserId);

        assertThat(result.shifts()).containsExactly(ShiftProgrammedItemDTO.from(shift));
    }

    @Test
    void getUsersInShift_returnsUsersMappedToParticipantDTOs() {
        UUID shiftId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = buildUser(userId, "Anna", "Bianchi");

        when(shiftProgrammedRepository.existsById(shiftId)).thenReturn(true);
        when(shiftUserRepository.findUsersByShiftId(shiftId)).thenReturn(List.of(user));

        List<ShiftParticipantDTO> result = shiftProgrammedService.getUsersInShift(shiftId);

        assertThat(result).containsExactly(new ShiftParticipantDTO(userId, "Anna", "Bianchi"));
    }

    @Test
    void getUsersInShift_shiftDoesNotExist_throwsResourceNotFoundException() {
        UUID shiftId = UUID.randomUUID();

        when(shiftProgrammedRepository.existsById(shiftId)).thenReturn(false);

        assertThatThrownBy(() -> shiftProgrammedService.getUsersInShift(shiftId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
