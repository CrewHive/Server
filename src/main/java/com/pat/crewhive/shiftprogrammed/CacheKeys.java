package com.pat.crewhive.shiftprogrammed;

import com.pat.crewhive.common.Period;

import java.util.UUID;

public final class CacheKeys {

    private CacheKeys() {}

    public static String shiftsByUser(UUID userId, Period period) {
        return userId + ":" + period;
    }

    public static String shiftsByCompany(UUID userId, Period period) {
        return userId + ":" + period;
    }
}

