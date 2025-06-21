package com.ururulab.ururu.global.common.entity.enumerated;

public enum SkinType {
	OILY,
	DRY,
	SENSITIVE,
	COMBINATION;

	public static SkinType from(String value) {
		return EnumParser.fromString(SkinType.class, value, "SkinType");
	}
}
