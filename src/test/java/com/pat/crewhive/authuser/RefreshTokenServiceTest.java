package com.pat.crewhive.authuser;

import com.pat.crewhive.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RefreshTokenService}.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository repo;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(repo);
    }

    private User buildUser(UUID userId) {
        User user = new User("mario.rossi@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    // ---------------------------------------------------------------------
    // getOrIssueRefreshToken()
    // ---------------------------------------------------------------------

    @Test
    void getOrIssueRefreshToken_returnsExistingTokenUnchanged_whenAValidTokenExists() {
        User user = buildUser(UUID.randomUUID());
        RefreshToken existing = new RefreshToken(UUID.randomUUID(), "existing-token", user, LocalDate.now().plusDays(1));

        when(repo.findByUser(user)).thenReturn(java.util.Optional.of(existing));

        String result = refreshTokenService.getOrIssueRefreshToken(user);

        assertThat(result).isEqualTo("existing-token");
        // reusing the existing session must not delete or mutate it
        verify(repo, never()).deleteByUser(any());
        verify(repo, never()).save(any());
    }

    @Test
    void getOrIssueRefreshToken_issuesNewToken_whenNoValidTokenExists() {
        User user = buildUser(UUID.randomUUID());

        when(repo.findByUser(user)).thenReturn(java.util.Optional.empty());

        String result = refreshTokenService.getOrIssueRefreshToken(user);

        assertThat(result).isNotBlank();
        verify(repo).save(any(RefreshToken.class));
    }
}
