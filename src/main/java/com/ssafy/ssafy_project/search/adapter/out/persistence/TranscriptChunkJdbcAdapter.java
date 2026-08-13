package com.ssafy.ssafy_project.search.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회의 발화(트랜스크립트) 임베딩 저장소 — 통합 시맨틱 검색용.
 * knowledge_chunks와 마찬가지로 pgvector 컬럼은 JDBC로 직접 다룬다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranscriptChunkJdbcAdapter {

    public static final int EMBEDDING_DIMENSION = 1536;

    private final JdbcTemplate jdbcTemplate;

    @Order(1)
    @EventListener(ApplicationReadyEvent.class)
    public void ensureSchema() {
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS transcript_chunks (
                        id BIGSERIAL PRIMARY KEY,
                        audio_id BIGINT NOT NULL UNIQUE,
                        room_id BIGINT NOT NULL,
                        speaker_name VARCHAR(100),
                        content TEXT NOT NULL,
                        spoke_time TIMESTAMP,
                        embedding vector(%d) NOT NULL
                    )
                    """.formatted(EMBEDDING_DIMENSION));
            jdbcTemplate.execute(
                    "CREATE INDEX IF NOT EXISTS idx_transcript_chunks_room_id ON transcript_chunks (room_id)");
        } catch (Exception e) {
            log.error("Failed to prepare transcript_chunks schema. Unified search will not work.", e);
        }
    }

    public void save(Long audioId, Long roomId, String speakerName, String content,
                     LocalDateTime spokeTime, float[] embedding) {
        jdbcTemplate.update(
                "INSERT INTO transcript_chunks (audio_id, room_id, speaker_name, content, spoke_time, embedding) " +
                        "VALUES (?, ?, ?, ?, ?, CAST(? AS vector)) ON CONFLICT (audio_id) DO NOTHING",
                audioId, roomId, speakerName, content, spokeTime, toVectorLiteral(embedding)
        );
    }

    /** 아직 임베딩되지 않은 발화들 (백필용) */
    public List<UnindexedSegment> findUnindexed(int limit) {
        return jdbcTemplate.query(
                """
                        SELECT t.audio_id, a.room_id,
                               COALESCE(u.nickname, u.name, 'speaker-' || CAST(a.speaker_id AS VARCHAR)) AS speaker_name,
                               t.text, a.start_time
                        FROM audio_text t
                        JOIN audio a ON a.id = t.audio_id
                        LEFT JOIN app_user u ON u.id = a.speaker_id
                        WHERE NOT EXISTS (SELECT 1 FROM transcript_chunks c WHERE c.audio_id = t.audio_id)
                          AND t.text IS NOT NULL AND LENGTH(TRIM(t.text)) > 0
                        ORDER BY t.audio_id
                        LIMIT ?
                        """,
                (rs, rowNum) -> new UnindexedSegment(
                        rs.getLong("audio_id"),
                        rs.getLong("room_id"),
                        rs.getString("speaker_name"),
                        rs.getString("text"),
                        rs.getTimestamp("start_time") != null
                                ? rs.getTimestamp("start_time").toLocalDateTime() : null
                ),
                limit
        );
    }

    public List<TranscriptMatch> searchSimilarInRooms(float[] queryEmbedding, List<Long> roomIds, int limit) {
        if (roomIds.isEmpty()) {
            return List.of();
        }

        String vector = toVectorLiteral(queryEmbedding);
        String placeholders = String.join(",", roomIds.stream().map(id -> "?").toList());

        Object[] params = new Object[roomIds.size() + 3];
        params[0] = vector;
        for (int i = 0; i < roomIds.size(); i++) {
            params[i + 1] = roomIds.get(i);
        }
        params[roomIds.size() + 1] = vector;
        params[roomIds.size() + 2] = limit;

        return jdbcTemplate.query(
                """
                        SELECT c.room_id, r.title AS room_title, c.speaker_name, c.content, c.spoke_time,
                               1 - (c.embedding <=> CAST(? AS vector)) AS score
                        FROM transcript_chunks c
                        JOIN rooms r ON r.id = c.room_id
                        WHERE c.room_id IN (%s)
                        ORDER BY c.embedding <=> CAST(? AS vector)
                        LIMIT ?
                        """.formatted(placeholders),
                (rs, rowNum) -> new TranscriptMatch(
                        rs.getLong("room_id"),
                        rs.getString("room_title"),
                        rs.getString("speaker_name"),
                        rs.getString("content"),
                        rs.getTimestamp("spoke_time") != null
                                ? rs.getTimestamp("spoke_time").toLocalDateTime() : null,
                        rs.getDouble("score")
                ),
                params
        );
    }

    private String toVectorLiteral(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        return sb.append(']').toString();
    }

    public record UnindexedSegment(
            Long audioId,
            Long roomId,
            String speakerName,
            String text,
            LocalDateTime spokeTime
    ) {
    }

    public record TranscriptMatch(
            Long roomId,
            String roomTitle,
            String speakerName,
            String content,
            LocalDateTime spokeTime,
            double score
    ) {
    }
}
