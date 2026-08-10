package com.ssafy.ssafy_project.audio.adapter.out.messaging;

import com.ssafy.ssafy_project.audio.application.port.out.PublishTranscriptPortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class StompTranscriptPublisherAdapter implements PublishTranscriptPortOut {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public void publish(TranscriptBroadcast transcriptBroadcast) {
        simpMessagingTemplate.convertAndSend(
                "/sub/rooms/" + transcriptBroadcast.roomId() + "/transcripts",
                new TranscriptPayload(
                        transcriptBroadcast.audioId(),
                        transcriptBroadcast.speakerId(),
                        transcriptBroadcast.speakerName(),
                        transcriptBroadcast.text(),
                        transcriptBroadcast.startTime(),
                        transcriptBroadcast.endTime()
                )
        );
    }

    record TranscriptPayload(
            Long audioId,
            Long speakerId,
            String speakerName,
            String text,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
    }
}
