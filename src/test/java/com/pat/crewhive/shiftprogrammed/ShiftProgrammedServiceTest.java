package com.pat.crewhive.shiftprogrammed;

import com.pat.crewhive.common.DateUtils;
import com.pat.crewhive.common.Period;
import com.pat.crewhive.common.StringUtils;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.company.CompanyAccessService;
import com.pat.crewhive.company.CompanyService;
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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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
    @Mock
    private CompanyAccessService companyAccessService;

    private ShiftProgrammedService shiftProgrammedService;

    @BeforeEach
    void setUp() {
        shiftProgrammedService = new ShiftProgrammedService(
                shiftProgrammedRepository, shiftUserRepository, stringUtils, userService, dateUtils, companyService,
                companyAccessService
        );
    }

    private User buildUser(UUID userId, String firstName, String lastName) {
        User user = new User("user@example.com", firstName, lastName, "encoded-pwd");
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private Company buildCompany() {
        Company company = new Company();
        ReflectionTestUtils.setField(company, "companyId", UUID.randomUUID());
        return company;
    }

    private ShiftProgrammed buildShiftWithUser(UUID userId) {
        ShiftProgrammed shift = new ShiftProgrammed();
        ReflectionTestUtils.setField(shift, "id", UUID.randomUUID());
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

        when(dateUtils.getStartDateForPeriod(Period.WEEK)).thenReturn(from);
        when(dateUtils.getEndDateForPeriod(Period.WEEK)).thenReturn(to);
        when(shiftProgrammedRepository.findByCompanyAndDateBetween(companyId, from, to))
                .thenReturn(List.of(shift));

        ShiftProgrammedOutputDTO result = shiftProgrammedService.getShiftsByPeriodAndCompany(Period.WEEK, requesterUserId, companyId);

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

    @Test
    void patchShift_reactivatesPreviouslySoftDeletedLink_insteadOfCreatingDuplicate() {
        UUID shiftId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Company company = buildCompany();
        ShiftProgrammed shift = new ShiftProgrammed();
        ReflectionTestUtils.setField(shift, "id", shiftId);
        shift.setShiftName("Turno mattina");
        shift.setStart(OffsetDateTime.parse("2026-08-21T09:00:00Z"));
        shift.setEnd(OffsetDateTime.parse("2026-08-21T17:00:00Z"));
        shift.setColor("#00FF00");
        shift.setCompany(company);

        User user = buildUser(userId, "Mario", "Rossi");
        user.setCompany(company);

        ShiftUser softDeletedLink = new ShiftUser(shift, user);
        softDeletedLink.markDeleted(user);

        PatchShiftProgrammedDTO dto = new PatchShiftProgrammedDTO(
                shiftId, "Turno mattina", null,
                OffsetDateTime.parse("2026-08-21T09:00:00Z"),
                OffsetDateTime.parse("2026-08-21T17:00:00Z"),
                "00FF00", java.util.Set.of(userId)
        );

        when(shiftProgrammedRepository.findByIdWithWorkers(shiftId)).thenReturn(java.util.Optional.of(shift));
        when(stringUtils.normalizeString("Turno mattina")).thenReturn("Turno mattina");
        when(userService.getUsersByIds(java.util.Set.of(userId))).thenReturn(List.of(user));
        when(shiftUserRepository.findByIdIncludingDeleted(shiftId, userId)).thenReturn(java.util.Optional.of(softDeletedLink));

        shiftProgrammedService.patchShift(UUID.randomUUID(), dto);

        assertThat(softDeletedLink.isActive()).isTrue();
        assertThat(softDeletedLink.getDeletedAt()).isNull();
        assertThat(softDeletedLink.getDeletedBy()).isNull();
        assertThat(shift.getUsers()).hasSize(1);
        assertThat(shift.getUsers().iterator().next()).isSameAs(softDeletedLink);
    }

    @Test
    void patchShift_requesterNotPartOfShiftCompany_throwsIllegalArgumentException() {
        UUID shiftId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();

        ShiftProgrammed shift = new ShiftProgrammed();
        ReflectionTestUtils.setField(shift, "id", shiftId);
        shift.setShiftName("Turno mattina");
        shift.setStart(OffsetDateTime.parse("2026-08-21T09:00:00Z"));
        shift.setEnd(OffsetDateTime.parse("2026-08-21T17:00:00Z"));
        shift.setColor("#00FF00");
        shift.setCompany(buildCompany());

        PatchShiftProgrammedDTO dto = new PatchShiftProgrammedDTO(
                shiftId, "Turno mattina", null,
                OffsetDateTime.parse("2026-08-21T09:00:00Z"),
                OffsetDateTime.parse("2026-08-21T17:00:00Z"),
                "00FF00", null
        );

        when(shiftProgrammedRepository.findByIdWithWorkers(shiftId)).thenReturn(java.util.Optional.of(shift));
        when(companyAccessService.isNotPartOfCompany(requesterUserId, shift.getCompany().getCompanyId())).thenReturn(true);

        assertThatThrownBy(() -> shiftProgrammedService.patchShift(requesterUserId, dto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteShift_requesterNotPartOfShiftCompany_throwsIllegalArgumentException() {
        UUID shiftId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();

        ShiftProgrammed shift = new ShiftProgrammed();
        ReflectionTestUtils.setField(shift, "id", shiftId);
        shift.setCompany(buildCompany());

        when(shiftProgrammedRepository.findById(shiftId)).thenReturn(java.util.Optional.of(shift));
        when(companyAccessService.isNotPartOfCompany(requesterUserId, shift.getCompany().getCompanyId())).thenReturn(true);

        assertThatThrownBy(() -> shiftProgrammedService.deleteShift(requesterUserId, shiftId))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userService, never()).getUserById(requesterUserId);
    }

    @Test
    void createShift_persistsAllParticipants_notJustTheFirst() {
        Company company = buildCompany();
        User creator = buildUser(UUID.randomUUID(), "Anna", "Bianchi");
        creator.setCompany(company);
        User user1 = buildUser(UUID.randomUUID(), "Mario", "Rossi");
        user1.setCompany(company);
        User user2 = buildUser(UUID.randomUUID(), "Luigi", "Verdi");
        user2.setCompany(company);

        CreateShiftProgrammedDTO dto = new CreateShiftProgrammedDTO(
                "Turno mattina", null,
                OffsetDateTime.parse("2026-08-21T09:00:00Z"),
                OffsetDateTime.parse("2026-08-21T17:00:00Z"),
                "00FF00", Set.of(user1.getUserId(), user2.getUserId())
        );

        UUID creatorUserId = creator.getUserId();

        when(userService.getUserById(creatorUserId)).thenReturn(creator);
        when(stringUtils.normalizeString("Turno mattina")).thenReturn("Turno mattina");
        when(userService.getUsersByIds(Set.of(user1.getUserId(), user2.getUserId())))
                .thenReturn(List.of(user1, user2));
        when(shiftProgrammedRepository.save(any(ShiftProgrammed.class))).thenAnswer(inv -> inv.getArgument(0));

        shiftProgrammedService.createShift(creatorUserId, dto);

        ArgumentCaptor<ShiftProgrammed> captor = ArgumentCaptor.forClass(ShiftProgrammed.class);
        verify(shiftProgrammedRepository).save(captor.capture());
        assertThat(captor.getValue().getUsers()).hasSize(2);
        assertThat(captor.getValue().getCompany()).isSameAs(company);
    }
}
