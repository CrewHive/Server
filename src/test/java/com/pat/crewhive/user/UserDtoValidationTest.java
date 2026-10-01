package com.pat.crewhive.user;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Validation of {@link UserDTO}: the email must accept real addresses.
 */
class UserDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private UserDTO dto(String email) {
        return new UserDTO(email, "Mario", "Rossi", Set.of("ROLE_USER"), UUID.randomUUID());
    }

    @Test
    void aNormalEmail_isValid() {
        assertThat(validator.validate(dto("mario.rossi@example.com"))).isEmpty();
    }

    @Test
    void aBlankEmail_isInvalid() {
        assertThat(validator.validate(dto(" "))).isNotEmpty();
    }

    @Test
    void anEmailLongerThan64Characters_isInvalid() {
        assertThat(validator.validate(dto("a".repeat(60) + "@x.it"))).isNotEmpty();
    }
}
