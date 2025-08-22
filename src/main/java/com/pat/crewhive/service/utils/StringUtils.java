package com.pat.crewhive.service.utils;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class StringUtils {

    public StringUtils() {}

    public String normalizeString(String input) {
        return input.strip().toLowerCase(Locale.ROOT);
    }

    public String normalizeRole(String raw) {

        String r = raw.trim().toUpperCase();
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }
}
