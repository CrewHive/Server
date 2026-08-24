package com.pat.crewhive.security.key;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class JwtKeyPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
            .withUserConfiguration(JwtKeyProperties.class);

    @Test
    void failsToStartWhenPrivateKeyIsMissing() {
        contextRunner
                .withPropertyValues("jwt.publicKey=some-value")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void failsToStartWhenPublicKeyIsMissing() {
        contextRunner
                .withPropertyValues("jwt.privateKey=some-value")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void startsWhenBothKeysArePresent() {
        contextRunner
                .withPropertyValues("jwt.privateKey=some-value", "jwt.publicKey=some-value")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
