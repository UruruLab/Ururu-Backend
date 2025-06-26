package com.ururulab.ururu.global.domain.entity.enumerated;

public enum SkinType {
	OILY,
	DRY,
	SENSITIVE,
	COMBINATION,
	DRY_LIGHT,
	TROUBLE,
	NEUTRAL;

	public static SkinType from(String value) {
		return EnumParser.fromString(SkinType.class, value, "SkinType");
	}
}
