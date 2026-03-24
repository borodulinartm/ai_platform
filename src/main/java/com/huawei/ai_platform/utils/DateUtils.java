package com.huawei.ai_platform.utils;

import jakarta.annotation.Nonnull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static com.huawei.ai_platform.common.Constant.ZONE;

/**
 * Static helpful class for working with datetime section
 */
@NoArgsConstructor(access = AccessLevel.NONE)
public class DateUtils {
    /**
     * Performs translation news
     *
     * @param forDate for which date do you want translations
     * @param zoneId  for which zone
     * @return microseconds instant
     */
    public static long getAsMicro(@Nonnull LocalDateTime forDate, @Nonnull ZoneId zoneId) {
        Instant instant = forDate.atZone(ZONE).toInstant();
        long epochSecond = instant.getEpochSecond();
        int nanoAdjustment = instant.getNano();

        return epochSecond * 1_000_000L + nanoAdjustment / 1_000L;
    }
}
