package com.ururulab.ururu.global.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * API 응답 표준화를 위한 래퍼 클래스.
 *
 * <p>모든 API 응답을 일관된 형태로 제공하며, 성공/실패 상태와 메시지, 데이터를 포함합니다.</p>
 *
 * @param <T> 응답 데이터 타입
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data // null 가능, 에러 발생 시에는 null
) {

    /**
     * 성공 응답 생성 (데이터 포함).
     *
     * @param <T> 응답 데이터 타입
     * @param data 응답 데이터
     * @return 성공 응답
     * @throws IllegalArgumentException data가 null인 경우
     */
    public static <T> ApiResponse<T> success(final T data) {
        if (data == null) {
            throw new IllegalArgumentException("성공 응답의 데이터는 null일 수 없습니다.");
        }
        return new ApiResponse<>(true, "성공", data);
    }

    /**
     * 성공 응답 생성 (커스텀 메시지 포함).
     *
     * @param <T> 응답 데이터 타입
     * @param message 성공 메시지
     * @param data 응답 데이터
     * @return 성공 응답
     */
    public static <T> ApiResponse<T> success(final String message, final T data) {
        if (data == null) {
            throw new IllegalArgumentException("성공 응답의 데이터는 null일 수 없습니다.");
        }
        return new ApiResponse<>(true, message, data);
    }

    /**
     * 성공 응답 생성 (메시지만 포함, 데이터 없음).
     *
     * @param message 성공 메시지
     * @return 데이터가 없는 성공 응답
     */
    public static ApiResponse<Void> success(final String message) {
        return new ApiResponse<>(true, message, null);
    }

    /**
     * 실패 응답 생성.
     *
     * @param message 에러 메시지
     * @return 실패 응답
     */
    public static ApiResponse<Void> error(final String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("에러 메시지는 필수입니다.");
        }
        return new ApiResponse<>(false, message, null);
    }
}