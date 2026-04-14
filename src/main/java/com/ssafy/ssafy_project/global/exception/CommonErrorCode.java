package com.ssafy.ssafy_project.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "50010", "서버 내부 오류가 발생했습니다."),
    TEST_CUSTOM_ERROR(HttpStatus.BAD_REQUEST, "40010", "AudioController 강제 커스텀 예외입니다.");

    private final HttpStatus statusCode;
    private final String errorCode;
    private final String message;
}
