package com.ururulab.ururu.global.common.entity.enumerated;

public enum Gender {
	MALE,
	FEMALE,
	NONE;

	public static Gender from(String value) {
		return EnumParser.fromString(Gender.class, value, "Gender");
	}
}
