package com.ssafy.ssafy_project.audio.application.port.out;

import java.time.LocalDateTime;

public record TranscriptSegment(
        String speakerName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String text
) {
}
