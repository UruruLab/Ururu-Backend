package com.ururulab.ururu.review.domain.entity.enumerated;

import com.ururulab.ururu.global.domain.entity.enumerated.EnumParser;

public enum AgeGroup {
	TEENS, TWENTIES, THIRTIES, FORTIES_PLUS;

	public static AgeGroup from(String value) {
		return EnumParser.fromString(AgeGroup.class, value, "AgeGroup");
	}
}
