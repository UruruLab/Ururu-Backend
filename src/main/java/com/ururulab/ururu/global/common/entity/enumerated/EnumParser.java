package com.ururulab.ururu.global.common.entity.enumerated;

import java.util.Arrays;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EnumParser {

	public static <T extends Enum<T>> T fromString(Class<T> enumClass, String value, String typeName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(typeName + " 값이 비어있습니다.");
		}

		return Arrays.stream(enumClass.getEnumConstants())
				.filter(e -> e.name().equalsIgnoreCase(value.trim()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("유효하지 않은 " + typeName + " 값: " + value));
	}
}
