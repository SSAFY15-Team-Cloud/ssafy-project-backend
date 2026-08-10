package com.ssafy.ssafy_project.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "50010", "Internal server error."),
    TEST_CUSTOM_ERROR(HttpStatus.BAD_REQUEST, "40010", "Forced custom exception from AudioController."),

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "40011", "Request validation failed."),
    INVALID_OBJECT_KEY(HttpStatus.BAD_REQUEST, "40012", "Invalid object key."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "40013", "Password does not match."),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST, "40014", "New password must be different from the current password."),

    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "40110", "Invalid email or password."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "40111", "Invalid token."),

    NOT_ROOM_HOST(HttpStatus.FORBIDDEN, "40310", "Only the room host can perform this action."),
    NOT_ROOM_PARTICIPANT(HttpStatus.FORBIDDEN, "40311", "Only active room participants can perform this action."),
    NOT_MESSAGE_AUTHOR(HttpStatus.FORBIDDEN, "40312", "Only the message author can perform this action."),
    USER_DELETED(HttpStatus.FORBIDDEN, "40313", "This user has been deleted."),

    AUDIO_NOT_FOUND(HttpStatus.NOT_FOUND, "40410", "Audio not found."),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "40411", "Room not found."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "40412", "Report not found."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "40413", "User not found."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "40414", "Message not found."),
    PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "40415", "Room participant not found."),
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "40416", "Document not found."),

    ROOM_ALREADY_ENDED(HttpStatus.CONFLICT, "40910", "The room has already ended."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "40911", "Email is already in use."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "40912", "Nickname is already in use.");

    private final HttpStatus statusCode;
    private final String errorCode;
    private final String message;
}
