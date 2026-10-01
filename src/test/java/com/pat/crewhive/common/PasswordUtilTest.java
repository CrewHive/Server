package com.pat.crewhive.common;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordUtilTest {

    @Test
    void burnMatch_runsAMatchAgainstAPrecomputedDummyHash() {
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(encoder.encode(anyString())).thenReturn("dummy-hash");

        new PasswordUtil(encoder).burnMatch("any-password");

        verify(encoder).matches("any-password", "dummy-hash");
    }
}
