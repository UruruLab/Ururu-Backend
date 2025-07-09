package com.ururulab.ururu.groupBuy.util;

import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.time.Instant;

@UtilityClass
public class TimeCalculator {
    /**
     * 남은 시간을 초 단위로 계산
     */
    public Long calculateRemainingSeconds(Instant endsAt, Instant now) {
        if (endsAt.isBefore(now)) {
            return 0L;  // 종료 시 0초
        }

        Duration remaining = Duration.between(now, endsAt);
        return remaining.getSeconds();  // 남은 초만
    }

    /**
     * 현재 시간 기준으로 남은 시간 계산
     */
    public Long calculateRemainingSeconds(Instant endsAt) {
        return calculateRemainingSeconds(endsAt, Instant.now());
    }
}
