package com.pat.crewhive.authuser;

import com.pat.crewhive.security.exception.custom.InvalidTokenException;
import com.pat.crewhive.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RefreshTokenService}.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-10T12:00:00Z");

    @Mock
    private RefreshTokenRepository repo;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(repo, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private User buildUser(UUID userId) {
        User user = new User("mario.rossi@example.com", "Mario", "Rossi", "encoded-pwd");
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private RefreshToken storedToken(User user, String raw, UUID familyId, Instant expiresAt, Instant usedAt) {
        return new RefreshToken(UUID.randomUUID(), RefreshTokenService.hash(raw), familyId, user, expiresAt, usedAt);
    }

    // ---------------------------------------------------------------------
    // issueNewFamily()
    // ---------------------------------------------------------------------

    @Test
    void issueNewFamily_storesOnlyTheHash_andDeletesPreviousTokens() {
        User user = buildUser(UUID.randomUUID());

        String raw = refreshTokenService.issueNewFamily(user);

        verify(repo).deleteByUser(user);
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(UUID.fromString(raw)).isNotNull();
        assertThat(saved.getTokenHash()).isNotEqualTo(raw).isEqualTo(RefreshTokenService.hash(raw)).hasSize(64);
        assertThat(saved.getFamilyId()).isNotNull();
        assertThat(saved.getUsedAt()).isNull();
        assertThat(saved.getUser()).isSameAs(user);
    }

    @Test
    void issueNewFamily_expiresExactlyTtlFromNow_withSecondGranularity() {
        refreshTokenService.issueNewFamily(buildUser(UUID.randomUUID()));

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).save(captor.capture());

        assertThat(captor.getValue().getExpiresAt()).isEqualTo(NOW.plus(RefreshTokenService.REFRESH_TOKEN_TTL));
    }

    @Test
    void issueNewFamily_issuesDifferentTokensAndFamiliesEachTime() {
        User user = buildUser(UUID.randomUUID());

        String first = refreshTokenService.issueNewFamily(user);
        String second = refreshTokenService.issueNewFamily(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo, times(2)).save(captor.capture());

        assertThat(first).isNotEqualTo(second);
        assertThat(captor.getAllValues().get(0).getFamilyId()).isNotEqualTo(captor.getAllValues().get(1).getFamilyId());
    }

    // ---------------------------------------------------------------------
    // rotate()
    // ---------------------------------------------------------------------

    @Test
    void rotate_marksOldTokenUsed_andIssuesNewTokenInTheSameFamily() {
        User user = buildUser(UUID.randomUUID());
        UUID family = UUID.randomUUID();
        RefreshToken current = storedToken(user, "raw-1", family, NOW.plusSeconds(60), null);

        when(repo.findByTokenHashWithUserAndRole(RefreshTokenService.hash("raw-1"))).thenReturn(Optional.of(current));
        when(repo.markUsed(current.getRefreshTokenId(), NOW)).thenReturn(1);

        RefreshTokenService.Rotation result = refreshTokenService.rotate("raw-1");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repo).save(captor.capture());
        RefreshToken next = captor.getValue();

        assertThat(result.user()).isSameAs(user);
        assertThat(result.refreshToken()).isNotEqualTo("raw-1");
        assertThat(next.getTokenHash()).isEqualTo(RefreshTokenService.hash(result.refreshToken()));
        assertThat(next.getFamilyId()).isEqualTo(family);
        assertThat(next.getExpiresAt()).isEqualTo(NOW.plus(RefreshTokenService.REFRESH_TOKEN_TTL));
        verify(repo).deleteExpiredByUser(user, NOW);
        verify(repo, never()).deleteByFamilyId(any());
    }

    @Test
    void rotate_revokesWholeFamily_whenTokenWasAlreadyUsed() {
        User user = buildUser(UUID.randomUUID());
        UUID family = UUID.randomUUID();
        RefreshToken used = storedToken(user, "raw-1", family, NOW.plusSeconds(60), NOW.minusSeconds(30));

        when(repo.findByTokenHashWithUserAndRole(RefreshTokenService.hash("raw-1"))).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> refreshTokenService.rotate("raw-1"))
                .isInstanceOf(InvalidTokenException.class);

        verify(repo).deleteByFamilyId(family);
        verify(repo, never()).save(any());
        verify(repo, never()).markUsed(any(), any());
    }

    @Test
    void rotate_revokesWholeFamily_whenConcurrentRotationWonTheRace() {
        User user = buildUser(UUID.randomUUID());
        UUID family = UUID.randomUUID();
        RefreshToken current = storedToken(user, "raw-1", family, NOW.plusSeconds(60), null);

        when(repo.findByTokenHashWithUserAndRole(RefreshTokenService.hash("raw-1"))).thenReturn(Optional.of(current));
        when(repo.markUsed(current.getRefreshTokenId(), NOW)).thenReturn(0);

        assertThatThrownBy(() -> refreshTokenService.rotate("raw-1"))
                .isInstanceOf(InvalidTokenException.class);

        verify(repo).deleteByFamilyId(family);
        verify(repo, never()).save(any());
    }

    @Test
    void rotate_rejectsUnknownToken_withoutRevokingAnything() {
        when(repo.findByTokenHashWithUserAndRole(anyHash())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.rotate("unknown"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid refresh token");

        verify(repo, never()).deleteByFamilyId(any());
        verify(repo, never()).save(any());
    }

    @Test
    void rotate_rejectsExpiredToken_evenByASingleSecond() {
        User user = buildUser(UUID.randomUUID());
        RefreshToken expired = storedToken(user, "raw-1", UUID.randomUUID(), NOW.minusSeconds(1), null);

        when(repo.findByTokenHashWithUserAndRole(RefreshTokenService.hash("raw-1"))).thenReturn(Optional.of(expired));
        when(repo.markUsed(expired.getRefreshTokenId(), NOW)).thenReturn(1);

        assertThatThrownBy(() -> refreshTokenService.rotate("raw-1"))
                .isInstanceOf(InvalidTokenException.class);

        verify(repo, never()).save(any());
    }

    @Test
    void rotate_rejectsTokenExpiringExactlyNow() {
        User user = buildUser(UUID.randomUUID());
        RefreshToken expiring = storedToken(user, "raw-1", UUID.randomUUID(), NOW, null);

        when(repo.findByTokenHashWithUserAndRole(RefreshTokenService.hash("raw-1"))).thenReturn(Optional.of(expiring));
        when(repo.markUsed(expiring.getRefreshTokenId(), NOW)).thenReturn(1);

        assertThatThrownBy(() -> refreshTokenService.rotate("raw-1"))
                .isInstanceOf(InvalidTokenException.class);
    }

    // ---------------------------------------------------------------------
    // getValidToken() / revokeFamily() / deleteTokenByUser()
    // ---------------------------------------------------------------------

    @Test
    void getValidToken_returnsLiveToken() {
        RefreshToken live = storedToken(buildUser(UUID.randomUUID()), "raw-1", UUID.randomUUID(), NOW.plusSeconds(1), null);

        when(repo.findByTokenHashWithUserAndRole(RefreshTokenService.hash("raw-1"))).thenReturn(Optional.of(live));

        assertThat(refreshTokenService.getValidToken("raw-1")).isSameAs(live);
    }

    @Test
    void getValidToken_throwsInvalidToken_whenUnknownOrExpired() {
        RefreshToken expired = storedToken(buildUser(UUID.randomUUID()), "raw-2", UUID.randomUUID(), NOW, null);

        when(repo.findByTokenHashWithUserAndRole(RefreshTokenService.hash("raw-1"))).thenReturn(Optional.empty());
        when(repo.findByTokenHashWithUserAndRole(RefreshTokenService.hash("raw-2"))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.getValidToken("raw-1")).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> refreshTokenService.getValidToken("raw-2")).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void revokeFamily_deletesEveryTokenOfTheFamily() {
        UUID family = UUID.randomUUID();
        RefreshToken rt = storedToken(buildUser(UUID.randomUUID()), "raw-1", family, NOW.plusSeconds(60), null);

        refreshTokenService.revokeFamily(rt);

        verify(repo).deleteByFamilyId(eq(family));
    }

    @Test
    void deleteTokenByUser_deletesEveryTokenOfTheUser() {
        User user = buildUser(UUID.randomUUID());

        refreshTokenService.deleteTokenByUser(user);

        verify(repo).deleteByUser(user);
    }

    @Test
    void hash_isDeterministicSha256Hex() {
        assertThat(RefreshTokenService.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    private static String anyHash() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}
