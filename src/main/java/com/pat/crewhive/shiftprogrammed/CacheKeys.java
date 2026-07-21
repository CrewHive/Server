package com.pat.crewhive.shiftprogrammed;

import com.pat.crewhive.common.Period;

public final class CacheKeys {

    private CacheKeys() {}

    public static String shiftsByUser(Long userId, Period period) {
        return userId + ":" + period;
    }

    public static String shiftsByCompany(Long userId, Period period) {
        return userId + ":" + period;
    }
}

