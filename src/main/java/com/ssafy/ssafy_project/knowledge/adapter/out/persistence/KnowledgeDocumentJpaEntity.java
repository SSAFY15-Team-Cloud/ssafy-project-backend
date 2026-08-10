package com.ssafy.ssafy_project.knowledge.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "knowledge_documents")
public class KnowledgeDocumentJpaEntity {

    public enum Status {PROCESSING, READY, FAILED}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 300)
    private String filename;

    @Column(name = "object_key", nullable = false, length = 500)
    private String objectKey;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "chunk_count", nullable = false)
    private int chunkCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @CreationTimestamp
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    public KnowledgeDocumentJpaEntity(Long ownerId, String title, String filename,
                                      String objectKey, String contentType, Long fileSize) {
        this.ownerId = ownerId;
        this.title = title;
        this.filename = filename;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.chunkCount = 0;
        this.status = Status.PROCESSING;
    }

    public void markReady(int chunkCount) {
        this.chunkCount = chunkCount;
        this.status = Status.READY;
    }

    public void markFailed() {
        this.status = Status.FAILED;
    }
}
