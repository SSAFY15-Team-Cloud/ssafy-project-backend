package com.ssafy.ssafy_project.search.application.service;

import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioJpaEntity;
import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioJpaRepository;
import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioTextJpaEntity;
import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioTextJpaRepository;
import com.ssafy.ssafy_project.audio.application.event.AudioSttCompletedEvent;
import com.ssafy.ssafy_project.global.infrastructure.openai.OpenAiEmbeddingClient;
import com.ssafy.ssafy_project.search.adapter.out.persistence.TranscriptChunkJdbcAdapter;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 발화 텍스트를 임베딩해 통합 검색 인덱스(transcript_chunks)에 적재한다.
 * - 실시간: STT 완료 이벤트마다 1건씩
 * - 백필: 기동 시 미인덱싱분을 소량 처리 (과거 데이터 자가 치유)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptIndexingService {

    private static final int MAX_EMBED_CHARS = 4_000;
    private static final int BACKFILL_LIMIT = 200;
    private static final int BACKFILL_BATCH = 20;

    private final AudioJpaRepository audioJpaRepository;
    private final AudioTextJpaRepository audioTextJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final OpenAiEmbeddingClient embeddingClient;
    private final TranscriptChunkJdbcAdapter transcriptChunkAdapter;

    @Value("${openai.api.key:}")
    private String openAiApiKey;

    @Async("reportTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSttCompleted(AudioSttCompletedEvent event) {
        try {
            AudioJpaEntity audio = audioJpaRepository.findById(event.audioId()).orElse(null);
            AudioTextJpaEntity audioText = audioTextJpaRepository
                    .findFirstByAudioIdOrderByCreatedTimeDesc(event.audioId())
                    .orElse(null);
            if (audio == null || audioText == null || audioText.getText().isBlank()) {
                return;
            }

            String speakerName = userJpaRepository.findById(audio.getSpeakerId())
                    .map(u -> u.getNickname() != null ? u.getNickname() : u.getName())
                    .orElse("speaker-" + audio.getSpeakerId());

            float[] embedding = embeddingClient.embedOne(truncate(audioText.getText()));
            transcriptChunkAdapter.save(
                    audio.getId(), audio.getRoomId(), speakerName,
                    audioText.getText(), audio.getStartTime(), embedding
            );
        } catch (Exception e) {
            log.error("Transcript indexing failed. audioId={}", event.audioId(), e);
        }
    }

    @Async("reportTaskExecutor")
    @Order(2)
    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        if (openAiApiKey.isBlank()) {
            return;
        }

        try {
            List<TranscriptChunkJdbcAdapter.UnindexedSegment> pending =
                    transcriptChunkAdapter.findUnindexed(BACKFILL_LIMIT);
            if (pending.isEmpty()) {
                return;
            }

            for (int start = 0; start < pending.size(); start += BACKFILL_BATCH) {
                List<TranscriptChunkJdbcAdapter.UnindexedSegment> batch =
                        pending.subList(start, Math.min(pending.size(), start + BACKFILL_BATCH));
                List<float[]> embeddings = embeddingClient.embed(
                        batch.stream().map(s -> truncate(s.text())).toList()
                );
                for (int i = 0; i < batch.size(); i++) {
                    TranscriptChunkJdbcAdapter.UnindexedSegment segment = batch.get(i);
                    transcriptChunkAdapter.save(
                            segment.audioId(), segment.roomId(), segment.speakerName(),
                            segment.text(), segment.spokeTime(), embeddings.get(i)
                    );
                }
            }
            log.info("Transcript search backfill indexed {} segments.", pending.size());
        } catch (Exception e) {
            log.warn("Transcript search backfill failed (will retry on next startup).", e);
        }
    }

    private String truncate(String text) {
        return text.length() > MAX_EMBED_CHARS ? text.substring(0, MAX_EMBED_CHARS) : text;
    }
}
