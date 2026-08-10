package com.ssafy.ssafy_project.knowledge.adapter.out.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * pgvector 컬럼은 Hibernate 매핑 대신 JDBC로 직접 다룬다.
 * (임베딩 삽입/유사도 검색 모두 네이티브 SQL이 필요하기 때문)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeChunkJdbcAdapter {

    public static final int EMBEDDING_DIMENSION = 1536;

    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void ensureSchema() {
        try {
            createSchema();
        } catch (Exception e) {
            // pgvector가 없는 DB에서도 앱 기동 자체는 가능해야 한다 (추천 기능만 비활성)
            log.error("Failed to prepare knowledge_chunks schema. Knowledge recommendation will not work.", e);
        }
    }

    private void createSchema() {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS knowledge_chunks (
                    id BIGSERIAL PRIMARY KEY,
                    document_id BIGINT NOT NULL,
                    chunk_index INT NOT NULL,
                    content TEXT NOT NULL,
                    embedding vector(%d) NOT NULL
                )
                """.formatted(EMBEDDING_DIMENSION));
        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_knowledge_chunks_document_id ON knowledge_chunks (document_id)");
    }

    public void saveChunks(Long documentId, List<String> contents, List<float[]> embeddings) {
        for (int i = 0; i < contents.size(); i++) {
            jdbcTemplate.update(
                    "INSERT INTO knowledge_chunks (document_id, chunk_index, content, embedding) " +
                            "VALUES (?, ?, ?, CAST(? AS vector))",
                    documentId, i, contents.get(i), toVectorLiteral(embeddings.get(i))
            );
        }
    }

    public void deleteByDocumentId(Long documentId) {
        jdbcTemplate.update("DELETE FROM knowledge_chunks WHERE document_id = ?", documentId);
    }

    public List<ChunkMatch> searchSimilar(float[] queryEmbedding, int limit) {
        String vector = toVectorLiteral(queryEmbedding);
        return jdbcTemplate.query(
                """
                        SELECT c.document_id, c.content, d.title, d.filename, d.object_key,
                               1 - (c.embedding <=> CAST(? AS vector)) AS score
                        FROM knowledge_chunks c
                        JOIN knowledge_documents d ON d.id = c.document_id
                        WHERE d.status = 'READY'
                        ORDER BY c.embedding <=> CAST(? AS vector)
                        LIMIT ?
                        """,
                (rs, rowNum) -> new ChunkMatch(
                        rs.getLong("document_id"),
                        rs.getString("title"),
                        rs.getString("filename"),
                        rs.getString("object_key"),
                        rs.getString("content"),
                        rs.getDouble("score")
                ),
                vector, vector, limit
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

    public record ChunkMatch(
            Long documentId,
            String title,
            String filename,
            String objectKey,
            String content,
            double score
    ) {
    }
}
