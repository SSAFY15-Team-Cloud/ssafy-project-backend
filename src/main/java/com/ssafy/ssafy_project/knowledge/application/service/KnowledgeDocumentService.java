package com.ssafy.ssafy_project.knowledge.application.service;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.global.infrastructure.openai.OpenAiEmbeddingClient;
import com.ssafy.ssafy_project.knowledge.adapter.out.persistence.KnowledgeChunkJdbcAdapter;
import com.ssafy.ssafy_project.knowledge.adapter.out.persistence.KnowledgeDocumentJpaEntity;
import com.ssafy.ssafy_project.knowledge.adapter.out.persistence.KnowledgeDocumentJpaRepository;
import com.ssafy.ssafy_project.knowledge.adapter.out.s3.KnowledgeStorageAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KnowledgeDocumentService {

    private static final int CHUNK_SIZE = 800;
    private static final int CHUNK_OVERLAP = 100;
    private static final int MAX_CHUNKS = 200;

    private final KnowledgeDocumentJpaRepository documentRepository;
    private final KnowledgeChunkJdbcAdapter chunkAdapter;
    private final KnowledgeStorageAdapter storageAdapter;
    private final OpenAiEmbeddingClient embeddingClient;

    private static final int MAX_EXTRACT_CHARS = 500_000;
    private final Tika tika = new Tika();

    @Transactional
    public Long upload(Long ownerId, String title, String filename, String contentType, byte[] bytes) {
        String text = extractText(filename, bytes);
        List<String> chunks = chunkText(text);

        if (chunks.isEmpty()) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }

        String objectKey = "knowledge/" + UUID.randomUUID() + "/" + filename;
        storageAdapter.upload(objectKey, bytes, contentType);

        KnowledgeDocumentJpaEntity document = documentRepository.save(
                new KnowledgeDocumentJpaEntity(ownerId, title, filename, objectKey, contentType, (long) bytes.length)
        );

        try {
            List<float[]> embeddings = embeddingClient.embed(chunks);
            chunkAdapter.saveChunks(document.getId(), chunks, embeddings);
            document.markReady(chunks.size());
            log.info("Knowledge document indexed. documentId={}, chunks={}", document.getId(), chunks.size());
        } catch (Exception e) {
            document.markFailed();
            log.error("Knowledge document embedding failed. documentId={}", document.getId(), e);
        }

        return document.getId();
    }

    public List<KnowledgeDocumentJpaEntity> list() {
        return documentRepository.findAllByOrderByCreatedTimeDesc();
    }

    public String generateDownloadUrl(Long documentId) {
        KnowledgeDocumentJpaEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.DOCUMENT_NOT_FOUND));
        return storageAdapter.generateDownloadUrl(document.getObjectKey());
    }

    @Transactional
    public void delete(Long documentId, Long userId) {
        KnowledgeDocumentJpaEntity document = documentRepository.findById(documentId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getOwnerId().equals(userId)) {
            throw new CustomException(CommonErrorCode.NOT_MESSAGE_AUTHOR);
        }

        chunkAdapter.deleteByDocumentId(documentId);
        documentRepository.delete(document);

        try {
            storageAdapter.delete(document.getObjectKey());
        } catch (Exception e) {
            log.warn("Failed to delete knowledge object from storage. objectKey={}", document.getObjectKey(), e);
        }
    }

    /** md/txt는 그대로, PDF/DOCX 등은 Tika로 텍스트 추출 */
    private String extractText(String filename, byte[] bytes) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".md") || lower.endsWith(".txt") || lower.endsWith(".markdown")) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        try {
            return tika.parseToString(new ByteArrayInputStream(bytes), new org.apache.tika.metadata.Metadata(), MAX_EXTRACT_CHARS);
        } catch (Exception e) {
            log.warn("Tika text extraction failed. filename={}", filename, e);
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }
    }

    // 문단 경계를 존중하면서 CHUNK_SIZE 근처로 자르고, 문단이 너무 길면 오버랩 슬라이딩으로 분할
    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();
        String[] paragraphs = text.split("\\n\\s*\\n");
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.length() > CHUNK_SIZE) {
                flush(chunks, current);
                for (int start = 0; start < trimmed.length(); start += CHUNK_SIZE - CHUNK_OVERLAP) {
                    int end = Math.min(trimmed.length(), start + CHUNK_SIZE);
                    chunks.add(trimmed.substring(start, end));
                    if (end == trimmed.length()) {
                        break;
                    }
                }
                continue;
            }

            if (current.length() + trimmed.length() + 2 > CHUNK_SIZE) {
                flush(chunks, current);
            }
            if (!current.isEmpty()) {
                current.append("\n\n");
            }
            current.append(trimmed);
        }
        flush(chunks, current);

        if (chunks.size() > MAX_CHUNKS) {
            return chunks.subList(0, MAX_CHUNKS);
        }
        return chunks;
    }

    private void flush(List<String> chunks, StringBuilder current) {
        if (!current.isEmpty()) {
            chunks.add(current.toString());
            current.setLength(0);
        }
    }
}
