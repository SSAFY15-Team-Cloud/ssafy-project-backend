package com.ssafy.ssafy_project.audio.application.port.in;

import java.time.LocalDateTime;

public record CreateAudioMetadataCommand(
        Long roomId,
        Long speakerId,
        String path,
        String mimeType,
        Integer duration,
        Long fileSize,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
