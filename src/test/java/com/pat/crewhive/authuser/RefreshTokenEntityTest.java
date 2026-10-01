package com.pat.crewhive.authuser;

import com.pat.crewhive.user.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link RefreshToken} accessors.
 */
class RefreshTokenEntityTest {

    @Test
    void fullConstructor_setsEveryField() {
        UUID id = UUID.randomUUID();
        UUID family = UUID.randomUUID();
        User user = new User("m@example.com", "Mario", "Rossi", "pwd");
        Instant expires = Instant.now().plusSeconds(60);
        Instant used = Instant.now();

        RefreshToken token = new RefreshToken(id, "hash", family, user, expires, used);

        assertThat(token.getRefreshTokenId()).isEqualTo(id);
        assertThat(token.getTokenHash()).isEqualTo("hash");
        assertThat(token.getFamilyId()).isEqualTo(family);
        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getExpiresAt()).isEqualTo(expires);
        assertThat(token.getUsedAt()).isEqualTo(used);
    }

    @Test
    void settersReplaceTheValues() {
        RefreshToken token = new RefreshToken();
        UUID id = UUID.randomUUID();
        UUID family = UUID.randomUUID();
        User user = new User("m@example.com", "Mario", "Rossi", "pwd");
        Instant expires = Instant.now().plusSeconds(60);
        Instant used = Instant.now();

        token.setRefreshTokenId(id);
        token.setTokenHash("h2");
        token.setFamilyId(family);
        token.setUser(user);
        token.setExpiresAt(expires);
        token.setUsedAt(used);

        assertThat(token.getRefreshTokenId()).isEqualTo(id);
        assertThat(token.getTokenHash()).isEqualTo("h2");
        assertThat(token.getFamilyId()).isEqualTo(family);
        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getExpiresAt()).isEqualTo(expires);
        assertThat(token.getUsedAt()).isEqualTo(used);
    }

    @Test
    void aNewToken_isNotUsed() {
        assertThat(new RefreshToken().getUsedAt()).isNull();
    }
}
