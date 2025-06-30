package com.ururulab.ururu.global.exception.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ErrorResponse(
        String errorCode,
        String message,
        String path,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp
) {

    public static ErrorResponse of(final String errorCode, final String message, final String path) {
        return new ErrorResponse(errorCode, message, path, LocalDateTime.now());
    }

    public static ErrorResponse of(final String errorCode, final String message) {
        return new ErrorResponse(errorCode, message, null, LocalDateTime.now());
    }
}
