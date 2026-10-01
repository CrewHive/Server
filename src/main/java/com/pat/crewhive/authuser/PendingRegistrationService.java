package com.pat.crewhive.authuser;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * Tiene in Redis le registrazioni in attesa di conferma email. L'utente viene creato nel DB solo
 * alla conferma. In Redis la chiave e' lo SHA-256 del token: chi legge Redis non puo' confermare
 * registrazioni altrui.
 */
@Service
public class PendingRegistrationService {

    static final Duration TTL = Duration.ofHours(24);

    private static final String KEY_PREFIX = "pending-registration:";
    private static final int TOKEN_BYTES = 32;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public PendingRegistrationService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Salva la registrazione pendente.
     *
     * @return il token in chiaro da inviare via mail (non viene mai salvato)
     */
    public String create(String email, String firstName, String lastName, String encodedPassword) {

        byte[] raw = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(raw);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        String key = KEY_PREFIX + hash(token);

        redisTemplate.opsForHash().putAll(key, Map.of(
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "password", encodedPassword
        ));
        redisTemplate.expire(key, TTL);

        return token;
    }

    /**
     * Consuma il token: e' monouso. Se due richieste concorrenti lo presentano insieme, solo quella
     * per cui {@code delete} restituisce true ottiene la registrazione.
     */
    public Optional<PendingRegistration> consume(String token) {

        String key = KEY_PREFIX + hash(token);

        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
        if (data.isEmpty() || !Boolean.TRUE.equals(redisTemplate.delete(key))) {
            return Optional.empty();
        }

        return Optional.of(new PendingRegistration(
                (String) data.get("email"),
                (String) data.get("firstName"),
                (String) data.get("lastName"),
                (String) data.get("password")
        ));
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record PendingRegistration(String email, String firstName, String lastName, String encodedPassword) {
    }
}
