package com.ssafy.ssafy_project.insight.application.service;

import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioTextJpaRepository;
import com.ssafy.ssafy_project.audio.application.event.AudioSttCompletedEvent;
import com.ssafy.ssafy_project.global.infrastructure.openai.OpenAiInsightClient;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 회의 진행 중 트랜스크립트가 쌓이면 롤링 요약/액션아이템/논점을 추출해 브로드캐스트한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingInsightService {

    private static final long THROTTLE_SECONDS = 45;
    private static final int MIN_NEW_SEGMENTS = 3;
    private static final int MAX_TRANSCRIPT_CHARS = 12_000;

    private final AudioTextJpaRepository audioTextJpaRepository;
    private final LoadRoomPortOut loadRoomPortOut;
    private final OpenAiInsightClient insightClient;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final ObjectMapper objectMapper;

    private final Map<Long, InsightState> stateByRoom = new ConcurrentHashMap<>();

    @Async("reportTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSttCompleted(AudioSttCompletedEvent event) {
        Long roomId = event.roomId();

        try {
            var segments = audioTextJpaRepository.findTranscriptSegmentsByRoomIdOrderByAudioTime(roomId);
            if (segments.isEmpty() || !shouldRun(roomId, segments.size())) {
                return;
            }

            StringBuilder transcript = new StringBuilder();
            for (var segment : segments) {
                if (segment.getText() == null || segment.getText().isBlank()) {
                    continue;
                }
                transcript.append(segment.getSpeakerName())
                        .append(": ")
                        .append(segment.getText().trim())
                        .append('\n');
            }
            String transcriptText = transcript.toString();
            if (transcriptText.isBlank()) {
                return;
            }
            if (transcriptText.length() > MAX_TRANSCRIPT_CHARS) {
                transcriptText = transcriptText.substring(transcriptText.length() - MAX_TRANSCRIPT_CHARS);
            }

            Room room = loadRoomPortOut.loadById(roomId);
            String insightJson = insightClient.extractInsightJson(room.getTitle(), transcriptText);

            JsonNode insight = objectMapper.readTree(insightJson);
            simpMessagingTemplate.convertAndSend(
                    "/sub/rooms/" + roomId + "/ai/insights",
                    new InsightPayload(insight, LocalDateTime.now())
            );
            log.info("Meeting insight broadcast. roomId={}, segments={}", roomId, segments.size());
        } catch (Exception e) {
            log.error("Meeting insight generation failed. roomId={}", roomId, e);
        }
    }

    private boolean shouldRun(Long roomId, int totalSegments) {
        Instant now = Instant.now();
        InsightState state = stateByRoom.get(roomId);

        if (state != null) {
            boolean throttled = state.lastRun().plusSeconds(THROTTLE_SECONDS).isAfter(now);
            boolean tooFewNewSegments = totalSegments - state.segmentCount() < MIN_NEW_SEGMENTS;
            if (throttled || tooFewNewSegments) {
                return false;
            }
        }

        stateByRoom.put(roomId, new InsightState(now, totalSegments));
        return true;
    }

    private record InsightState(Instant lastRun, int segmentCount) {
    }

    public record InsightPayload(JsonNode insight, LocalDateTime generatedTime) {
    }
}
