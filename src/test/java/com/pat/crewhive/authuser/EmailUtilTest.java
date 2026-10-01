package com.pat.crewhive.authuser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link EmailUtil}.
 */
class EmailUtilTest {

    private final EmailUtil emailUtil = new EmailUtil();

    @ParameterizedTest
    @ValueSource(strings = {
            "mario.rossi@example.com",
            "mario+tag@example.com",
            "m_r%x-1@sub.example.co.uk",
            "A@B.IT"
    })
    void isValidEmail_acceptsWellFormedAddresses(String email) {
        assertThat(emailUtil.isValidEmail(email)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "plainaddress",
            "@example.com",
            "mario@",
            "mario@example",
            "mario@example.c",
            "mario@@example.com",
            "mario rossi@example.com",
            " mario@example.com",
            "mario@example.com ",
            "mario@exa mple.com"
    })
    void isValidEmail_rejectsMalformedAddresses(String email) {
        assertThat(emailUtil.isValidEmail(email)).isFalse();
    }

    @Test
    void isValidEmail_nullInput_throwsNullPointerException() {
        assertThatThrownBy(() -> emailUtil.isValidEmail(null)).isInstanceOf(NullPointerException.class);
    }
}
