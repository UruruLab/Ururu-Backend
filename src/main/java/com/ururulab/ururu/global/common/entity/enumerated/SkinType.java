package com.ururulab.ururu.global.common.entity.enumerated;

public enum SkinType {
	OILY,
	DRY,
	SENSITIVE,
	COMBINATION,
	DRY_LIGHT,
	NEUTRAL;

	public static SkinType from(String value) {
		return EnumParser.fromString(SkinType.class, value, "SkinType");
	}
}
