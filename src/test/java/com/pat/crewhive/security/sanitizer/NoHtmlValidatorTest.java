package com.pat.crewhive.security.sanitizer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link NoHtmlValidator}.
 */
class NoHtmlValidatorTest {

    private final NoHtmlValidator validator = new NoHtmlValidator();

    @Test
    void null_isValid() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Team meeting", "Riunione 10:00 - sala 2", "Più tardi"})
    void plainText_isValid(String value) {
        assertThat(validator.isValid(value, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"<b>bold</b>", "<script>alert(1)</script>", "x<img src=x onerror=y>", "a <i>b</i>"})
    void textWithHtml_isInvalid(String value) {
        assertThat(validator.isValid(value, null)).isFalse();
    }
}
