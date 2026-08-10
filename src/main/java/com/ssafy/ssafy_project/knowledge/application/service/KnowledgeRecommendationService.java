package com.ssafy.ssafy_project.knowledge.application.service;

import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioTextJpaRepository;
import com.ssafy.ssafy_project.audio.application.event.AudioSttCompletedEvent;
import com.ssafy.ssafy_project.global.infrastructure.openai.OpenAiEmbeddingClient;
import com.ssafy.ssafy_project.knowledge.adapter.out.persistence.KnowledgeChunkJdbcAdapter;
import com.ssafy.ssafy_project.knowledge.adapter.out.s3.KnowledgeStorageAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 최근 발화 내용을 임베딩해 지식 위키에서 관련 문서를 찾아 회의방에 실시간 추천한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRecommendationService {

    private static final int TRANSCRIPT_WINDOW = 6;
    private static final int MAX_QUERY_CHARS = 4_000;
    private static final int CHUNK_SEARCH_LIMIT = 8;
    private static final int MAX_RECOMMENDATIONS = 3;
    private static final double MIN_SCORE = 0.25;
    private static final long THROTTLE_SECONDS = 20;

    private final AudioTextJpaRepository audioTextJpaRepository;
    private final OpenAiEmbeddingClient embeddingClient;
    private final KnowledgeChunkJdbcAdapter chunkAdapter;
    private final KnowledgeStorageAdapter storageAdapter;
    private final SimpMessagingTemplate simpMessagingTemplate;

    private final Map<Long, Instant> lastRunByRoom = new ConcurrentHashMap<>();

    @Async("reportTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSttCompleted(AudioSttCompletedEvent event) {
        Long roomId = event.roomId();
        if (isThrottled(roomId)) {
            return;
        }

        try {
            List<AudioTextJpaRepository.TranscriptSegmentProjection> segments =
                    audioTextJpaRepository.findTranscriptSegmentsByRoomIdOrderByAudioTime(roomId);
            if (segments.isEmpty()) {
                return;
            }

            String window = segments.stream()
                    .skip(Math.max(0, segments.size() - TRANSCRIPT_WINDOW))
                    .map(AudioTextJpaRepository.TranscriptSegmentProjection::getText)
                    .filter(text -> text != null && !text.isBlank())
                    .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
            if (window.isBlank()) {
                return;
            }
            // 임베딩 API 입력 한도(8192 토큰) 보호 — 최근 발화가 더 중요하므로 뒤에서 자른다
            if (window.length() > MAX_QUERY_CHARS) {
                window = window.substring(window.length() - MAX_QUERY_CHARS);
            }

            float[] queryEmbedding = embeddingClient.embedOne(window);
            List<KnowledgeChunkJdbcAdapter.ChunkMatch> matches =
                    chunkAdapter.searchSimilar(queryEmbedding, CHUNK_SEARCH_LIMIT);

            List<RecommendedDocument> recommendations = new ArrayList<>();
            Set<Long> seenDocuments = new HashSet<>();
            for (KnowledgeChunkJdbcAdapter.ChunkMatch match : matches) {
                if (match.score() < MIN_SCORE || !seenDocuments.add(match.documentId())) {
                    continue;
                }
                recommendations.add(new RecommendedDocument(
                        match.documentId(),
                        match.title(),
                        match.filename(),
                        snippet(match.content()),
                        Math.round(match.score() * 1000d) / 1000d,
                        storageAdapter.generateDownloadUrl(match.objectKey())
                ));
                if (recommendations.size() >= MAX_RECOMMENDATIONS) {
                    break;
                }
            }

            if (recommendations.isEmpty()) {
                return;
            }

            simpMessagingTemplate.convertAndSend(
                    "/sub/rooms/" + roomId + "/ai/recommendations",
                    new RecommendationPayload(recommendations)
            );
        } catch (Exception e) {
            log.error("Knowledge recommendation failed. roomId={}", roomId, e);
        }
    }

    private boolean isThrottled(Long roomId) {
        Instant now = Instant.now();
        Instant lastRun = lastRunByRoom.get(roomId);
        if (lastRun != null && lastRun.plusSeconds(THROTTLE_SECONDS).isAfter(now)) {
            return true;
        }
        lastRunByRoom.put(roomId, now);
        return false;
    }

    private String snippet(String content) {
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() > 160 ? normalized.substring(0, 160) + "…" : normalized;
    }

    public record RecommendationPayload(List<RecommendedDocument> documents) {
    }

    public record RecommendedDocument(
            Long documentId,
            String title,
            String filename,
            String snippet,
            double score,
            String downloadUrl
    ) {
    }
}
