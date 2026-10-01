package com.pat.crewhive.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link StringUtils}.
 */
class StringUtilsTest {

    private final StringUtils stringUtils = new StringUtils();

    @Test
    void normalizeString_stripsSurroundingWhitespaceAndLowercases() {
        assertThat(stringUtils.normalizeString("  Mario.Rossi@Example.COM \t")).isEqualTo("mario.rossi@example.com");
    }

    @Test
    void normalizeString_keepsInnerWhitespace() {
        assertThat(stringUtils.normalizeString(" Acme  Srl ")).isEqualTo("acme  srl");
    }

    @Test
    void normalizeString_isLocaleIndependent() {
        // con il locale turco "I".toLowerCase() darebbe "ı": Locale.ROOT deve evitarlo
        assertThat(stringUtils.normalizeString("TITLE")).isEqualTo("title");
    }

    @Test
    void normalizeString_nullInput_throwsNullPointerException() {
        assertThatThrownBy(() -> stringUtils.normalizeString(null)).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "cashier,      ROLE_CASHIER",
            "  cashier  ,  ROLE_CASHIER",
            "ROLE_CASHIER, ROLE_CASHIER",
            "role_cashier, ROLE_CASHIER",
            "Manager,      ROLE_MANAGER"
    })
    void normalizeRole_addsPrefixAndUppercases(String raw, String expected) {
        assertThat(stringUtils.normalizeRole(raw)).isEqualTo(expected);
    }

    @Test
    void normalizeRole_isIdempotent() {
        String once = stringUtils.normalizeRole("cashier");

        assertThat(stringUtils.normalizeRole(once)).isEqualTo(once);
    }

    @Test
    void normalizeRole_nullInput_throwsNullPointerException() {
        assertThatThrownBy(() -> stringUtils.normalizeRole(null)).isInstanceOf(NullPointerException.class);
    }
}
