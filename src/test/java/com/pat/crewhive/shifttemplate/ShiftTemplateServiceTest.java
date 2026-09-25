package com.pat.crewhive.shifttemplate;

import com.pat.crewhive.common.StringUtils;
import com.pat.crewhive.company.Company;
import com.pat.crewhive.company.CompanyService;
import com.pat.crewhive.security.exception.custom.ResourceAlreadyExistsException;
import com.pat.crewhive.security.exception.custom.ResourceNotFoundException;
import com.pat.crewhive.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ShiftTemplateService} (H4): every operation is scoped to the company
 * taken from the caller's token, and a caller without a company is rejected.
 */
@ExtendWith(MockitoExtension.class)
class ShiftTemplateServiceTest {

    private static final OffsetTime START = OffsetTime.parse("08:00:00Z");
    private static final OffsetTime END = OffsetTime.parse("16:00:00Z");

    @Mock
    private ShiftTemplateRepository repo;
    @Mock
    private CompanyService companyService;
    @Mock
    private UserService userService;

    private ShiftTemplateService service;

    private final UUID companyId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ShiftTemplateService(repo, companyService, new StringUtils(), userService);
    }

    private Company company(UUID id) {
        Company c = new Company();
        ReflectionTestUtils.setField(c, "companyId", id);
        return c;
    }

    private ShiftTemplate template(String name, Company company) {
        return new ShiftTemplate(UUID.randomUUID(), name, START, END, "desc", "FF0000", company);
    }

    private CreateShiftTemplateDTO createDto(String name) {
        return new CreateShiftTemplateDTO(name, "desc", "FF0000", START, END);
    }

    private PatchShiftTemplateDTO patchDto(String name, String oldName) {
        return new PatchShiftTemplateDTO(name, "desc", "FF0000", START, END, oldName);
    }

    // ------------------------------------------------------------------
    // caller without company
    // ------------------------------------------------------------------

    @Test
    void get_callerWithoutCompany_isDenied() {
        assertThatThrownBy(() -> service.getShiftTemplate("morning", null))
                .isInstanceOf(AuthorizationDeniedException.class);

        verifyNoInteractions(repo);
    }

    @Test
    void create_callerWithoutCompany_isDenied() {
        assertThatThrownBy(() -> service.createShiftTemplate(createDto("morning"), null))
                .isInstanceOf(AuthorizationDeniedException.class);

        verifyNoInteractions(repo, companyService);
    }

    @Test
    void patch_callerWithoutCompany_isDenied() {
        assertThatThrownBy(() -> service.patchShiftTemplate(patchDto("evening", "morning"), null))
                .isInstanceOf(AuthorizationDeniedException.class);

        verifyNoInteractions(repo);
    }

    @Test
    void delete_callerWithoutCompany_isDenied() {
        assertThatThrownBy(() -> service.deleteShiftTemplate("morning", null, UUID.randomUUID()))
                .isInstanceOf(AuthorizationDeniedException.class);

        verifyNoInteractions(repo, userService);
    }

    // ------------------------------------------------------------------
    // get
    // ------------------------------------------------------------------

    @Test
    void get_looksUpOnlyInCallerCompany() {
        ShiftTemplate st = template("morning", company(companyId));
        when(repo.findByShiftNameAndCompanyCompanyId("morning", companyId)).thenReturn(Optional.of(st));

        assertThat(service.getShiftTemplate("Morning", companyId)).isEqualTo(ShiftTemplateOutputDTO.from(st));
    }

    @Test
    void get_templateOfAnotherCompany_isNotFound() {
        when(repo.findByShiftNameAndCompanyCompanyId("morning", companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getShiftTemplate("morning", companyId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ------------------------------------------------------------------
    // create
    // ------------------------------------------------------------------

    @Test
    void create_assignsCallerCompany() {
        Company company = company(companyId);
        when(repo.existsByShiftNameAndCompanyCompanyId("morning", companyId)).thenReturn(false);
        when(companyService.getCompanyById(companyId)).thenReturn(company);
        when(repo.save(any(ShiftTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        ShiftTemplateOutputDTO result = service.createShiftTemplate(createDto("morning"), companyId);

        assertThat(result.shiftName()).isEqualTo("morning");

        ArgumentCaptor<ShiftTemplate> saved = ArgumentCaptor.forClass(ShiftTemplate.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getCompany()).isSameAs(company);
        assertThat(saved.getValue().getShiftName()).isEqualTo("morning");
    }

    @Test
    void create_duplicateInCallerCompany_isRejected() {
        when(repo.existsByShiftNameAndCompanyCompanyId("morning", companyId)).thenReturn(true);

        assertThatThrownBy(() -> service.createShiftTemplate(createDto("morning"), companyId))
                .isInstanceOf(ResourceAlreadyExistsException.class);

        verify(repo, never()).save(any());
    }

    // ------------------------------------------------------------------
    // patch
    // ------------------------------------------------------------------

    @Test
    void patch_updatesTemplateOfCallerCompany() {
        ShiftTemplate st = template("morning", company(companyId));
        when(repo.existsByShiftNameAndCompanyCompanyId("evening", companyId)).thenReturn(false);
        when(repo.findByShiftNameAndCompanyCompanyId("morning", companyId)).thenReturn(Optional.of(st));

        ShiftTemplateOutputDTO result = service.patchShiftTemplate(patchDto("Evening", "Morning"), companyId);

        assertThat(result.shiftName()).isEqualTo("evening");
        verify(repo).save(st);
    }

    @Test
    void patch_templateOfAnotherCompany_isNotFound() {
        when(repo.existsByShiftNameAndCompanyCompanyId("evening", companyId)).thenReturn(false);
        when(repo.findByShiftNameAndCompanyCompanyId("morning", companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.patchShiftTemplate(patchDto("evening", "morning"), companyId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repo, never()).save(any());
    }

    @Test
    void patch_newNameAlreadyUsedInCallerCompany_isRejected() {
        when(repo.existsByShiftNameAndCompanyCompanyId("evening", companyId)).thenReturn(true);

        assertThatThrownBy(() -> service.patchShiftTemplate(patchDto("evening", "morning"), companyId))
                .isInstanceOf(ResourceAlreadyExistsException.class);

        verify(repo, never()).save(any());
    }

    // ------------------------------------------------------------------
    // delete
    // ------------------------------------------------------------------

    @Test
    void delete_templateOfAnotherCompany_isNotFound() {
        when(repo.findByShiftNameAndCompanyCompanyId("morning", companyId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteShiftTemplate("morning", companyId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repo, never()).save(any());
        verifyNoInteractions(userService);
    }
}
