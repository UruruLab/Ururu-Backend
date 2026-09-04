package com.ururulab.ururu.global.domain.dto;

import com.ururulab.ururu.global.exception.error.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseFormatTest {

    @Test
    void failureKeepsMachineReadableErrorCode() {
        ApiResponseFormat<Void> response = ApiResponseFormat.fail(ErrorCode.PAYMENT_NOT_FOUND);

        assertThat(response.getCode()).isEqualTo("PAYMENT001");
        assertThat(response.isSuccess()).isFalse();
    }
}
