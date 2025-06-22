package com.ururulab.ururu.global.validation;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EnumValueValidator implements ConstraintValidator<EnumValue, String> {

	private Set<String> acceptedValues;
	private boolean ignoreCase;
	private boolean allowNull;

	@Override
	public void initialize(EnumValue annotation) {
		this.ignoreCase = annotation.ignoreCase();
		this.allowNull = annotation.allowNull();

		if (ignoreCase) {
			acceptedValues = Arrays.stream(annotation.enumClass().getEnumConstants())
					.map(e -> e.name().toUpperCase())
					.collect(Collectors.toSet());
		} else {
			acceptedValues = Arrays.stream(annotation.enumClass().getEnumConstants())
					.map(Enum::name)
					.collect(Collectors.toSet());
		}
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		if (value == null) return allowNull;

		String checkValue = ignoreCase ? value.trim().toUpperCase() : value.trim();

		boolean valid = acceptedValues.contains(checkValue);

		if (!valid) {
			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate(
					context.getDefaultConstraintMessageTemplate()
							+ String.format(ValidationMessages.ENUM_VALUE_INVALID_VALUE, value)
			).addConstraintViolation();
		}

		return valid;
	}
}
