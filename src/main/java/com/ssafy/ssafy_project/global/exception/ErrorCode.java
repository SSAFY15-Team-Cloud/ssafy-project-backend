package com.ssafy.ssafy_project.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    String getErrorCode();

    HttpStatus getStatusCode();

    String getMessage();
}
