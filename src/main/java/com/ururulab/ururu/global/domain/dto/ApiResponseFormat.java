package com.ururulab.ururu.global.domain.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * API 응답 표준화를 위한 래퍼 클래스.
 *
 * <p>모든 API 응답을 일관된 형태로 제공하며, 성공/실패 상태와 메시지, 데이터를 포함합니다.</p>
 *
 * @param <T> 응답 데이터 타입
 */
@Getter
public final class ApiResponseFormat<T> {

    private final boolean success;   // 요청 성공 여부
    private final String message;    // 응답 메시지
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;            // 실제 응답 데이터 (null 가능)

    private ApiResponseFormat(final boolean success, final String message, final T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /**
     * 성공 응답 생성 (데이터 포함).
     *
     * @param <T> 응답 데이터 타입
     * @param message 성공 메시지
     * @param data 응답 데이터
     * @return 성공 응답
     */
    public static <T> ApiResponseFormat<T> success(final String message, final T data) {
        return new ApiResponseFormat<>(true, message, data);
    }

    /**
     * 성공 응답 생성 (데이터 없음).
     *
     * @param <T> 응답 데이터 타입
     * @param message 성공 메시지
     * @return 데이터가 없는 성공 응답
     */
    public static <T> ApiResponseFormat<T> success(final String message) {
        return new ApiResponseFormat<>(true, message, null);
    }

    /**
     * 실패 응답 생성.
     *
     * @param <T> 응답 데이터 타입
     * @param message 실패 메시지
     * @return 실패 응답
     */
    public static <T> ApiResponseFormat<T> fail(final String message) {
        return new ApiResponseFormat<>(false, message, null);
    }
}