package com.ssafy.ssafy_project.audio.application.event;

public record AudioCreatedEvent(
        Long audioId,
        Long roomId
) {
}
