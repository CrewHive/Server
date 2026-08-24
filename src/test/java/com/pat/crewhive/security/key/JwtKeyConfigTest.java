package com.pat.crewhive.security.key;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtKeyConfigTest {

    @Test
    void throwsWhenNoRecognizedProfileIsActive() {
        MockEnvironment environment = new MockEnvironment();
        JwtKeyConfig config = new JwtKeyConfig(null, environment);

        assertThatThrownBy(config::validateActiveProfile)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SPRING_PROFILES_ACTIVE")
                .hasMessageContaining("cloud")
                .hasMessageContaining("onpremise");
    }

    @Test
    void doesNotThrowWhenCloudProfileIsActive() {
        MockEnvironment environment = new MockEnvironment();
        environment.addActiveProfile("cloud");
        JwtKeyConfig config = new JwtKeyConfig(null, environment);

        assertThatCode(config::validateActiveProfile).doesNotThrowAnyException();
    }

    @Test
    void doesNotThrowWhenOnpremiseProfileIsActive() {
        MockEnvironment environment = new MockEnvironment();
        environment.addActiveProfile("onpremise");
        JwtKeyConfig config = new JwtKeyConfig(null, environment);

        assertThatCode(config::validateActiveProfile).doesNotThrowAnyException();
    }
}
