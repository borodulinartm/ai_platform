package com.huawei.ai_platform.utils;

import jakarta.annotation.Nonnull;
import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Static helpful class for working with datetime section
 */
@UtilityClass
public class DateUtils {
    /**
     * Performs translation news
     *
     * @param forDate for which date do you want translations
     * @param zoneId  for which zone
     * @return microseconds instant
     */
    public static long getAsMicro(@Nonnull LocalDateTime forDate, @Nonnull ZoneId zoneId) {
        Instant instant = forDate.atZone(zoneId).toInstant();
        long epochSecond = instant.getEpochSecond();
        int nanoAdjustment = instant.getNano();

        return epochSecond * 1_000_000L + nanoAdjustment / 1_000L;
    }

    /**
     * Extracts local date time as seconds
     *
     * @param forDate which date do you want
     * @param zoneId  zone id
     * @return seconds representation
     */
    public static long getAsSeconds(@Nonnull LocalDateTime forDate, @Nonnull ZoneId zoneId) {
        Instant instant = forDate.atZone(zoneId).toInstant();
        return instant.getEpochSecond();
    }

    /**
     * Extracts local datetime structure from the instant side
     *
     * @param micro  microseconds
     * @param zoneId zone ID
     * @return local date time
     */
    public static LocalDateTime getFromMicro(long micro, ZoneId zoneId) {
        Instant instant = Instant.ofEpochMilli(micro / 1000).plusNanos(micro % 1000);
        return LocalDateTime.ofInstant(instant, zoneId);
    }
}
