package com.ururulab.ururu.global.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class TimeUtils {
    
    /**
     * Instant를 한국 시간대의 ZonedDateTime으로 변환
     */
    public static ZonedDateTime toKoreaZonedDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.of("Asia/Seoul"));
    }
    
    /**
     * Instant를 UTC 시간대의 ZonedDateTime으로 변환
     */
    public static ZonedDateTime toUtcZonedDateTime(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.of("UTC"));
    }
} 