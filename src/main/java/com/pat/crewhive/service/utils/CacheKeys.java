package com.pat.crewhive.service.utils;

import com.pat.crewhive.model.util.Period;

public final class CacheKeys {

    private CacheKeys() {}

    public static String shiftsByUser(Long userId, Period period) {
        return userId + ":" + period;
    }

    public static String shiftsByCompany(Long userId, Period period) {
        return userId + ":" + period;
    }
}

