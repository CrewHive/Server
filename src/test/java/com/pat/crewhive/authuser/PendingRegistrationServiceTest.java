package com.pat.crewhive.authuser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PendingRegistrationService} (Redis mocked; the real flow is covered by
 * {@code RegistrationEmailIntegrationTest}).
 */
@ExtendWith(MockitoExtension.class)
class PendingRegistrationServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private HashOperations<String, Object, Object> hashOps;

    private PendingRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new PendingRegistrationService(redisTemplate);
    }

    private static String sha256(String token) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void create_storesTheRegistrationUnderTheHashOfTheTokenWithA24HoursTtl() throws Exception {
        when(redisTemplate.opsForHash()).thenReturn((HashOperations) hashOps);

        String token = service.create("mario@example.com", "Mario", "Rossi", "encoded");

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, String>> data = ArgumentCaptor.forClass(Map.class);
        verify(hashOps).putAll(key.capture(), data.capture());
        assertThat(key.getValue()).isEqualTo("pending-registration:" + sha256(token));
        assertThat(key.getValue()).doesNotContain(token);
        assertThat(data.getValue()).containsEntry("email", "mario@example.com")
                .containsEntry("firstName", "Mario")
                .containsEntry("lastName", "Rossi")
                .containsEntry("password", "encoded");
        verify(redisTemplate).expire(key.getValue(), PendingRegistrationService.TTL);
        assertThat(PendingRegistrationService.TTL.toHours()).isEqualTo(24);
    }

    @Test
    @SuppressWarnings("unchecked")
    void create_generatesDifferentUrlSafeTokensEveryTime() {
        when(redisTemplate.opsForHash()).thenReturn((HashOperations) hashOps);

        String first = service.create("a@example.com", "A", "B", "p");
        String second = service.create("a@example.com", "A", "B", "p");

        assertThat(first).isNotEqualTo(second);
        assertThat(first).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    @SuppressWarnings("unchecked")
    void consume_validToken_returnsTheRegistrationAndDeletesTheKey() throws Exception {
        String token = "some-token";
        String key = "pending-registration:" + sha256(token);
        Map<Object, Object> stored = new HashMap<>();
        stored.put("email", "mario@example.com");
        stored.put("firstName", "Mario");
        stored.put("lastName", "Rossi");
        stored.put("password", "encoded");
        when(redisTemplate.opsForHash()).thenReturn((HashOperations) hashOps);
        when(hashOps.entries(key)).thenReturn(stored);
        when(redisTemplate.delete(key)).thenReturn(true);

        Optional<PendingRegistrationService.PendingRegistration> result = service.consume(token);

        assertThat(result).contains(new PendingRegistrationService.PendingRegistration(
                "mario@example.com", "Mario", "Rossi", "encoded"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void consume_unknownOrExpiredToken_returnsEmptyWithoutDeleting() {
        when(redisTemplate.opsForHash()).thenReturn((HashOperations) hashOps);
        when(hashOps.entries(anyString())).thenReturn(Map.of());

        assertThat(service.consume("unknown")).isEmpty();

        org.mockito.Mockito.verify(redisTemplate, org.mockito.Mockito.never()).delete(anyString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void consume_whenAConcurrentRequestAlreadyDeletedTheKey_returnsEmpty() throws Exception {
        String token = "raced-token";
        String key = "pending-registration:" + sha256(token);
        when(redisTemplate.opsForHash()).thenReturn((HashOperations) hashOps);
        when(hashOps.entries(key)).thenReturn(Map.of("email", "a@example.com"));
        when(redisTemplate.delete(key)).thenReturn(false);

        assertThat(service.consume(token)).isEmpty();
    }
}
