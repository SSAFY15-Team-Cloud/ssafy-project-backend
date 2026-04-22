package com.ssafy.ssafy_project.global.exception;

public record ErrorResponse(
        String errorCode,
        int statusCode,
        String message
) {

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(
                errorCode.getErrorCode(),
                errorCode.getStatusCode().value(),
                errorCode.getMessage()
        );
    }
}
