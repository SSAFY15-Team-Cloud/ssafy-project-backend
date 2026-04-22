package com.ssafy.ssafy_project.audio.application.event;

public record AudioSttCompletedEvent(
        Long audioId,
        Long roomId
) {
}
