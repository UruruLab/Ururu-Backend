package com.ururulab.ururu.global.domain.entity.enumerated;

public enum SkinTone {
    WARM, COOL, NEUTRAL, SPRING_WARM, SUMMER_COOL, AUTUMN_WARM, WINTER_COOL;

    public static SkinTone from(String value) {
        return EnumParser.fromString(SkinTone.class, value, "SkinTone");
    }
}
