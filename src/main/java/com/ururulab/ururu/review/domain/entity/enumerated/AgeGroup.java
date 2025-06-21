package com.ururulab.ururu.review.domain.entity.enumerated;

import java.util.Arrays;

public enum AgeGroup {
	TEENS, TWENTIES, THIRTIES, FORTIES_PLUS;

	// TODO: CustomException
	public static AgeGroup from(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("AgeGroup 값이 비어있습니다.");
		}
		return Arrays.stream(values())
				.filter(e -> e.name().equalsIgnoreCase(value.trim()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("유효하지 않은 AgeGroup 값: " + value));
	}
}
