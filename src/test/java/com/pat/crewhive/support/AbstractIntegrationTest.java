package com.pat.crewhive.support;

import com.pat.crewhive.security.CustomUserDetails;
import com.redis.testcontainers.RedisContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * Base class for {@code @SpringBootTest} integration tests.
 *
 * <p>Spins a real Postgres and a real Redis in Docker (Testcontainers), started once per JVM and
 * shared by every subclass, and points the Spring context at them via {@link DynamicPropertySource}.
 * The project has no embedded DB/Redis wired in, so this is the only way to load the full context
 * under test.
 *
 * <p>Authentication is injected directly into the {@code SecurityContext} with {@link #as(CustomUserDetails)}
 * (a {@code spring-security-test} post-processor); the {@code JwtAuthenticationFilter} sees an already
 * populated context and steps aside, so no real JWT is minted. This keeps the tests focused on the
 * object-level authorization logic (C2) rather than the token pipeline.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                // The AWS Parameter Store starter eagerly builds an SsmClient (needs an AWS region);
                // under the "onpremise" test profile there is no AWS, so exclude it.
                "spring.autoconfigure.exclude="
                        + "io.awspring.cloud.autoconfigure.config.parameterstore.ParameterStoreAutoConfiguration,"
                        + "io.awspring.cloud.autoconfigure.config.parameterstore.ParameterStoreReloadAutoConfiguration"
        })
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    protected static final RedisContainer REDIS = new RedisContainer("redis:7-alpine");

    private static final String JWT_PRIVATE_KEY_B64;
    private static final String JWT_PUBLIC_KEY_B64;

    static {
        POSTGRES.start();
        REDIS.start();
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            var keyPair = keyPairGenerator.generateKeyPair();
            // PemUtils strips headers/whitespace and base64-decodes, so a bare base64 body is enough.
            JWT_PRIVATE_KEY_B64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());
            JWT_PUBLIC_KEY_B64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getRedisHost);
        registry.add("spring.data.redis.port", REDIS::getRedisPort);
        registry.add("jwt.privateKey", () -> JWT_PRIVATE_KEY_B64);
        registry.add("jwt.publicKey", () -> JWT_PUBLIC_KEY_B64);
    }

    @Autowired
    protected MockMvc mockMvc;

    /**
     * A request post-processor that authenticates the request as {@code principal},
     * bypassing the JWT filter.
     */
    protected static RequestPostProcessor as(CustomUserDetails principal) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    /**
     * Build a {@link CustomUserDetails} as the JWT filter would from a token's claims.
     */
    protected static CustomUserDetails principal(UUID userId, UUID companyId, String... roles) {
        return new CustomUserDetails(
                userId,
                "principal-" + userId + "@example.com",
                "Test", "Principal",
                Set.of(roles),
                companyId,
                true,
                "test-jti-" + UUID.randomUUID(),
                new Date(System.currentTimeMillis() + 3_600_000));
    }
}
