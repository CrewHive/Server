package com.pat.crewhive.shiftprogrammed;

import com.pat.crewhive.common.DateUtils;
import com.pat.crewhive.common.Period;
import com.pat.crewhive.common.StringUtils;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.company.CompanyAccessService;
import com.pat.crewhive.company.CompanyService;
import com.pat.crewhive.security.exception.custom.InvalidRequestException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import org.springframework.security.authorization.AuthorizationDeniedException;
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
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
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
    private CompanyAccessService companyAccessService;

    private ShiftProgrammedService shiftProgrammedService;

    @BeforeEach
    void setUp() {
        shiftProgrammedService = new ShiftProgrammedService(
                shiftProgrammedRepository, shiftUserRepository, stringUtils, userService, dateUtils,
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
    void assertCanReadUserShifts_sameCompany_passes() {
        UUID userId = UUID.randomUUID();
        Company company = buildCompany();
        User target = buildUser(userId, "Luigi", "Verdi");
        target.setCompany(company);

        when(userService.getUserById(userId)).thenReturn(target);

        assertThatCode(() -> shiftProgrammedService.assertCanReadUserShifts(userId, company.getCompanyId()))
                .doesNotThrowAnyException();
    }

    @Test
    void assertCanReadUserShifts_otherCompanyOrNone_throwsResourceNotFound() {
        UUID userId = UUID.randomUUID();
        User target = buildUser(userId, "Luigi", "Verdi");
        target.setCompany(buildCompany());

        when(userService.getUserById(userId)).thenReturn(target);

        assertThatThrownBy(() -> shiftProgrammedService.assertCanReadUserShifts(userId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);

        target.setCompany(null);
        assertThatThrownBy(() -> shiftProgrammedService.assertCanReadUserShifts(userId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assertCanReadShift_sameCompany_passes() {
        Company company = buildCompany();
        ShiftProgrammed shift = buildShiftWithUser(UUID.randomUUID());
        shift.setCompany(company);
        UUID shiftId = shift.getShiftProgrammedId();

        when(shiftProgrammedRepository.findById(shiftId)).thenReturn(Optional.of(shift));

        assertThatCode(() -> shiftProgrammedService.assertCanReadShift(shiftId, company.getCompanyId()))
                .doesNotThrowAnyException();
    }

    @Test
    void assertCanReadShift_otherCompany_throwsResourceNotFound() {
        ShiftProgrammed shift = buildShiftWithUser(UUID.randomUUID());
        shift.setCompany(buildCompany());
        UUID shiftId = shift.getShiftProgrammedId();

        when(shiftProgrammedRepository.findById(shiftId)).thenReturn(Optional.of(shift));

        assertThatThrownBy(() -> shiftProgrammedService.assertCanReadShift(shiftId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assertCanReadShift_shiftDoesNotExist_throwsResourceNotFound() {
        UUID shiftId = UUID.randomUUID();

        when(shiftProgrammedRepository.findById(shiftId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftProgrammedService.assertCanReadShift(shiftId, UUID.randomUUID()))
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
        when(userService.getUsersInCompany(java.util.Set.of(userId), company.getCompanyId())).thenReturn(List.of(user));
        when(shiftUserRepository.findByIdIncludingDeleted(shiftId, userId)).thenReturn(java.util.Optional.of(softDeletedLink));

        shiftProgrammedService.patchShift(UUID.randomUUID(), dto);

        assertThat(softDeletedLink.isActive()).isTrue();
        assertThat(softDeletedLink.getDeletedAt()).isNull();
        assertThat(softDeletedLink.getDeletedBy()).isNull();
        assertThat(shift.getUsers()).hasSize(1);
        assertThat(shift.getUsers().iterator().next()).isSameAs(softDeletedLink);
    }

    @Test
    void patchShift_requesterNotPartOfShiftCompany_throwsAuthorizationDenied() {
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
                .isInstanceOf(AuthorizationDeniedException.class);
    }

    @Test
    void deleteShift_requesterNotPartOfShiftCompany_throwsAuthorizationDenied() {
        UUID shiftId = UUID.randomUUID();
        UUID requesterUserId = UUID.randomUUID();

        ShiftProgrammed shift = new ShiftProgrammed();
        ReflectionTestUtils.setField(shift, "id", shiftId);
        shift.setCompany(buildCompany());

        when(shiftProgrammedRepository.findById(shiftId)).thenReturn(java.util.Optional.of(shift));
        when(companyAccessService.isNotPartOfCompany(requesterUserId, shift.getCompany().getCompanyId())).thenReturn(true);

        assertThatThrownBy(() -> shiftProgrammedService.deleteShift(requesterUserId, shiftId))
                .isInstanceOf(AuthorizationDeniedException.class);

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
        when(userService.getUsersInCompany(Set.of(user1.getUserId(), user2.getUserId()), company.getCompanyId()))
                .thenReturn(List.of(user1, user2));
        when(shiftProgrammedRepository.save(any(ShiftProgrammed.class))).thenAnswer(inv -> inv.getArgument(0));

        shiftProgrammedService.createShift(creatorUserId, dto);

        ArgumentCaptor<ShiftProgrammed> captor = ArgumentCaptor.forClass(ShiftProgrammed.class);
        verify(shiftProgrammedRepository).save(captor.capture());
        assertThat(captor.getValue().getUsers()).hasSize(2);
        assertThat(captor.getValue().getCompany()).isSameAs(company);
    }

    // ---------------------------------------------------------------------
    // createShift(): rami di errore
    // ---------------------------------------------------------------------

    private static final OffsetDateTime T_START = OffsetDateTime.parse("2026-08-21T09:00:00Z");
    private static final OffsetDateTime T_END = OffsetDateTime.parse("2026-08-21T17:00:00Z");

    @Test
    void createShift_startAfterEnd_throwsInvalidRequestAndSavesNothing() {
        CreateShiftProgrammedDTO dto = new CreateShiftProgrammedDTO("Turno", null, T_END, T_START, "00FF00", Set.of());
        when(stringUtils.normalizeString("Turno")).thenReturn("turno");

        assertThatThrownBy(() -> shiftProgrammedService.createShift(UUID.randomUUID(), dto))
                .isInstanceOf(InvalidRequestException.class);

        verify(shiftProgrammedRepository, never()).save(any());
        verifyNoInteractions(userService);
    }

    @Test
    void createShift_creatorWithoutCompany_throwsResourceNotFoundAndSavesNothing() {
        User creator = buildUser(UUID.randomUUID(), "Anna", "Bianchi");
        CreateShiftProgrammedDTO dto = new CreateShiftProgrammedDTO("Turno", null, T_START, T_END, "00FF00", Set.of());
        when(stringUtils.normalizeString("Turno")).thenReturn("turno");
        when(userService.getUserById(creator.getUserId())).thenReturn(creator);

        assertThatThrownBy(() -> shiftProgrammedService.createShift(creator.getUserId(), dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(shiftProgrammedRepository, never()).save(any());
    }

    @Test
    void createShift_normalizesNameAndStoresTheDtoFields() {
        Company company = buildCompany();
        User creator = buildUser(UUID.randomUUID(), "Anna", "Bianchi");
        creator.setCompany(company);
        CreateShiftProgrammedDTO dto = new CreateShiftProgrammedDTO("  Turno  ", "desc", T_START, T_END, "00FF00", Set.of());
        when(stringUtils.normalizeString("  Turno  ")).thenReturn("turno");
        when(userService.getUserById(creator.getUserId())).thenReturn(creator);
        when(userService.getUsersInCompany(Set.of(), company.getCompanyId())).thenReturn(List.of());
        when(shiftProgrammedRepository.save(any(ShiftProgrammed.class))).thenAnswer(inv -> inv.getArgument(0));

        shiftProgrammedService.createShift(creator.getUserId(), dto);

        ArgumentCaptor<ShiftProgrammed> captor = ArgumentCaptor.forClass(ShiftProgrammed.class);
        verify(shiftProgrammedRepository).save(captor.capture());
        ShiftProgrammed saved = captor.getValue();
        assertThat(saved.getShiftName()).isEqualTo("turno");
        assertThat(saved.getDescription()).isEqualTo("desc");
        assertThat(saved.getStart()).isEqualTo(T_START);
        assertThat(saved.getEnd()).isEqualTo(T_END);
        assertThat(saved.getColor()).isEqualTo("00FF00");
    }

    // ---------------------------------------------------------------------
    // getShiftsByPeriodAndCompany(): accesso negato
    // ---------------------------------------------------------------------

    @Test
    void getShiftsByPeriodAndCompany_requesterNotPartOfCompany_throwsAuthorizationDenied() {
        UUID requesterId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(companyAccessService.isNotPartOfCompany(requesterId, companyId)).thenReturn(true);

        assertThatThrownBy(() -> shiftProgrammedService.getShiftsByPeriodAndCompany(Period.WEEK, requesterId, companyId))
                .isInstanceOf(AuthorizationDeniedException.class);

        verifyNoInteractions(shiftProgrammedRepository);
    }

    // ---------------------------------------------------------------------
    // patchShift(): rami
    // ---------------------------------------------------------------------

    private ShiftProgrammed shiftOf(Company company, User... members) {
        ShiftProgrammed shift = new ShiftProgrammed();
        ReflectionTestUtils.setField(shift, "id", UUID.randomUUID());
        shift.setShiftName("Turno mattina");
        shift.setStart(T_START);
        shift.setEnd(T_END);
        shift.setColor("0000FF");
        shift.setCompany(company);
        for (User m : members) {
            m.setCompany(company);
            shift.addUser(m);
        }
        return shift;
    }

    private PatchShiftProgrammedDTO patchDto(ShiftProgrammed shift, Set<UUID> userIds) {
        return new PatchShiftProgrammedDTO(shift.getShiftProgrammedId(), "Nuovo Nome", "nuova desc", T_START, T_END, "FF0000", userIds);
    }

    private void stubPatchPrerequisites(ShiftProgrammed shift, UUID requesterId) {
        when(shiftProgrammedRepository.findByIdWithWorkers(shift.getShiftProgrammedId())).thenReturn(Optional.of(shift));
        when(companyAccessService.isNotPartOfCompany(requesterId, shift.getCompany().getCompanyId())).thenReturn(false);
        when(stringUtils.normalizeString("Nuovo Nome")).thenReturn("nuovo nome");
    }

    @Test
    void patchShift_unknownShift_throwsResourceNotFound() {
        UUID shiftId = UUID.randomUUID();
        when(shiftProgrammedRepository.findByIdWithWorkers(shiftId)).thenReturn(Optional.empty());
        PatchShiftProgrammedDTO dto = new PatchShiftProgrammedDTO(shiftId, "x", null, T_START, T_END, "00FF00", null);

        assertThatThrownBy(() -> shiftProgrammedService.patchShift(UUID.randomUUID(), dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void patchShift_startAfterEnd_throwsInvalidRequest() {
        UUID requesterId = UUID.randomUUID();
        ShiftProgrammed shift = shiftOf(buildCompany());
        stubPatchPrerequisites(shift, requesterId);
        PatchShiftProgrammedDTO dto = new PatchShiftProgrammedDTO(
                shift.getShiftProgrammedId(), "Nuovo Nome", null, T_END, T_START, "FF0000", null);

        assertThatThrownBy(() -> shiftProgrammedService.patchShift(requesterId, dto))
                .isInstanceOf(InvalidRequestException.class);

        assertThat(shift.getStart()).isEqualTo(T_START);
    }

    @Test
    void patchShift_updatesScalarFields_andLeavesParticipantsUntouched_whenUserIdIsNull() {
        UUID requesterId = UUID.randomUUID();
        User member = buildUser(UUID.randomUUID(), "Mario", "Rossi");
        ShiftProgrammed shift = shiftOf(buildCompany(), member);
        stubPatchPrerequisites(shift, requesterId);

        UUID result = shiftProgrammedService.patchShift(requesterId, patchDto(shift, null));

        assertThat(result).isEqualTo(shift.getShiftProgrammedId());
        assertThat(shift.getShiftName()).isEqualTo("nuovo nome");
        assertThat(shift.getDescription()).isEqualTo("nuova desc");
        assertThat(shift.getColor()).isEqualTo("FF0000");
        assertThat(shift.getUsers()).hasSize(1);
        verify(userService, never()).getUsersByIds(any());
    }

    @Test
    void patchShift_removesParticipantsNoLongerListed() {
        UUID requesterId = UUID.randomUUID();
        User kept = buildUser(UUID.randomUUID(), "Mario", "Rossi");
        User dropped = buildUser(UUID.randomUUID(), "Luigi", "Verdi");
        ShiftProgrammed shift = shiftOf(buildCompany(), kept, dropped);
        stubPatchPrerequisites(shift, requesterId);
        when(userService.getUsersByIds(Set.of(dropped.getUserId()))).thenReturn(List.of(dropped));

        shiftProgrammedService.patchShift(requesterId, patchDto(shift, Set.of(kept.getUserId())));

        assertThat(shift.getUsers()).extracting(su -> su.getUser().getUserId()).containsExactly(kept.getUserId());
    }

    @Test
    void patchShift_addsNewParticipantsWithNewLink_whenNoSoftDeletedOneExists() {
        UUID requesterId = UUID.randomUUID();
        User existing = buildUser(UUID.randomUUID(), "Mario", "Rossi");
        User added = buildUser(UUID.randomUUID(), "Luigi", "Verdi");
        Company company = buildCompany();
        ShiftProgrammed shift = shiftOf(company, existing);
        added.setCompany(company);
        stubPatchPrerequisites(shift, requesterId);
        when(userService.getUsersInCompany(Set.of(added.getUserId()), company.getCompanyId())).thenReturn(List.of(added));
        when(shiftUserRepository.findByIdIncludingDeleted(shift.getShiftProgrammedId(), added.getUserId()))
                .thenReturn(Optional.empty());

        shiftProgrammedService.patchShift(requesterId, patchDto(shift, Set.of(existing.getUserId(), added.getUserId())));

        assertThat(shift.getUsers()).extracting(su -> su.getUser().getUserId())
                .containsExactlyInAnyOrder(existing.getUserId(), added.getUserId());
    }

    @Test
    void patchShift_emptyUserSet_removesEveryParticipant() {
        UUID requesterId = UUID.randomUUID();
        User m1 = buildUser(UUID.randomUUID(), "Mario", "Rossi");
        User m2 = buildUser(UUID.randomUUID(), "Luigi", "Verdi");
        ShiftProgrammed shift = shiftOf(buildCompany(), m1, m2);
        stubPatchPrerequisites(shift, requesterId);
        when(userService.getUsersByIds(Set.of(m1.getUserId(), m2.getUserId()))).thenReturn(List.of(m1, m2));

        shiftProgrammedService.patchShift(requesterId, patchDto(shift, Set.of()));

        assertThat(shift.getUsers()).isEmpty();
    }

    // ---------------------------------------------------------------------
    // deleteShift()
    // ---------------------------------------------------------------------

    @Test
    void deleteShift_unknownShift_throwsResourceNotFound() {
        UUID shiftId = UUID.randomUUID();
        when(shiftProgrammedRepository.findById(shiftId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftProgrammedService.deleteShift(UUID.randomUUID(), shiftId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteShift_softDeletesTheShiftWithTheRequesterAsActor() {
        UUID requesterId = UUID.randomUUID();
        User requester = buildUser(requesterId, "Anna", "Bianchi");
        ShiftProgrammed shift = shiftOf(buildCompany());
        when(shiftProgrammedRepository.findById(shift.getShiftProgrammedId())).thenReturn(Optional.of(shift));
        when(companyAccessService.isNotPartOfCompany(requesterId, shift.getCompany().getCompanyId())).thenReturn(false);
        when(userService.getUserById(requesterId)).thenReturn(requester);
        when(shiftProgrammedRepository.save(shift)).thenReturn(shift);

        shiftProgrammedService.deleteShift(requesterId, shift.getShiftProgrammedId());

        assertThat(shift.isActive()).isFalse();
        assertThat(shift.getDeletedBy()).isSameAs(requester);
        verify(shiftProgrammedRepository).delete(shift);
    }
}
