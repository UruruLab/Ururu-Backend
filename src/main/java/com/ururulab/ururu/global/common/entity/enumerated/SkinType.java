package com.ururulab.ururu.global.common.entity.enumerated;

import java.util.Arrays;

public enum SkinType {
	OILY,
	DRY,
	SENSITIVE,
	COMBINATION;

	// TODO: CustomException
	public static SkinType from(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("SkinType 값이 비어있습니다.");
		}
		return Arrays.stream(values())
				.filter(e -> e.name().equalsIgnoreCase(value.trim()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("유효하지 않은 SkinType 값: " + value));
	}
}
