package com.ururulab.ururu.image.domain;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.Getter;

@Getter
public enum ImageFormat {

	JPG("jpg", "image/jpeg"),
	JPEG("jpeg", "image/jpeg"),
	PNG("png", "image/png"),
	WEBP("webp", "image/webp");

	private final String extension;
	private final String mimeType;

	ImageFormat(String extension, String mimeType) {
		this.extension = extension;
		this.mimeType = mimeType;
	}

	private static final Map<String, ImageFormat> EXT_MAP = Stream.of(values())
			.collect(Collectors.toMap(
					ImageFormat::getExtension,
					fmt -> fmt
			));

	private static final Map<String, ImageFormat> MIME_MAP = Stream.of(values())
			.collect(Collectors.toMap(
					ImageFormat::getMimeType,
					fmt -> fmt,
					(existing, replacement) -> existing
			));

	public static Optional<ImageFormat> fromExtension(String ext) {
		return Optional.ofNullable(ext)
				.map(String::toLowerCase)
				.flatMap(e -> Optional.ofNullable(EXT_MAP.get(e)));
	}

	public static Optional<ImageFormat> fromMimeType(String mime) {
		return Optional.ofNullable(mime)
				.map(String::toLowerCase)
				.flatMap(m -> Optional.ofNullable(MIME_MAP.get(m)));
	}
}
