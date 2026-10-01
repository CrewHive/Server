package com.pat.crewhive.security.sanitizer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link HtmlSanitizer}.
 */
class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void stripAll_removesEveryTagButKeepsText() {
        assertThat(sanitizer.stripAll("<b>Ciao</b> <i>mondo</i>")).isEqualTo("Ciao mondo");
    }

    @Test
    void stripAll_removesScriptElementsWithTheirContent() {
        String cleaned = sanitizer.stripAll("ok<script>alert(1)</script>");

        assertThat(cleaned).isEqualTo("ok");
    }

    @Test
    void stripAll_leavesPlainTextUntouched() {
        assertThat(sanitizer.stripAll("Turno mattina")).isEqualTo("Turno mattina");
    }

    @Test
    void stripAll_null_returnsNull() {
        assertThat(sanitizer.stripAll(null)).isNull();
    }

    @Test
    void basic_keepsAllowedFormattingTags() {
        assertThat(sanitizer.basic("<b>Ciao</b>")).isEqualTo("<b>Ciao</b>");
    }

    @Test
    void basic_removesScriptsAndEventHandlers() {
        String cleaned = sanitizer.basic("<b onclick=\"x()\">Ciao</b><script>alert(1)</script>");

        assertThat(cleaned).isEqualTo("<b>Ciao</b>");
    }

    @Test
    void basic_removesJavascriptLinks() {
        String cleaned = sanitizer.basic("<a href=\"javascript:alert(1)\">x</a>");

        assertThat(cleaned).doesNotContain("javascript:");
    }

    @Test
    void basic_null_returnsNull() {
        assertThat(sanitizer.basic(null)).isNull();
    }
}
