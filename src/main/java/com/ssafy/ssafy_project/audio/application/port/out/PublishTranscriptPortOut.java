package com.ssafy.ssafy_project.audio.application.port.out;

import java.time.LocalDateTime;

public interface PublishTranscriptPortOut {

    void publish(TranscriptBroadcast transcriptBroadcast);

    record TranscriptBroadcast(
            Long roomId,
            Long audioId,
            Long speakerId,
            String speakerName,
            String text,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
    }
}
