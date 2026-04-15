package com.ssafy.ssafy_project.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "50010", "Internal server error."),
    TEST_CUSTOM_ERROR(HttpStatus.BAD_REQUEST, "40010", "Forced custom exception from AudioController."),
    ROOM_ALREADY_ENDED(HttpStatus.CONFLICT, "40910", "Cannot create audio metadata for an ended room.");

    private final HttpStatus statusCode;
    private final String errorCode;
    private final String message;
}
