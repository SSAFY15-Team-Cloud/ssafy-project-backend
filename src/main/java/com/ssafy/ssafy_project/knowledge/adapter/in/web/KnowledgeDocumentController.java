package com.ssafy.ssafy_project.knowledge.adapter.in.web;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.knowledge.adapter.out.persistence.KnowledgeDocumentJpaEntity;
import com.ssafy.ssafy_project.knowledge.application.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/knowledge/documents")
public class KnowledgeDocumentController {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    private final KnowledgeDocumentService knowledgeDocumentService;

    @PostMapping
    public ResponseEntity<UploadDocumentResponse> upload(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title
    ) throws IOException {
        if (file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.md";
        String resolvedTitle = (title != null && !title.isBlank()) ? title : stripExtension(filename);

        Long documentId = knowledgeDocumentService.upload(
                userId,
                resolvedTitle,
                filename,
                file.getContentType() != null ? file.getContentType() : "text/plain",
                file.getBytes()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(new UploadDocumentResponse(documentId));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> list() {
        List<DocumentResponse> documents = knowledgeDocumentService.list().stream()
                .map(DocumentResponse::from)
                .toList();
        return ResponseEntity.ok(documents);
    }

    @GetMapping("/{documentId}/download-url")
    public ResponseEntity<DownloadUrlResponse> downloadUrl(@PathVariable Long documentId) {
        return ResponseEntity.ok(new DownloadUrlResponse(
                knowledgeDocumentService.generateDownloadUrl(documentId)
        ));
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long documentId,
            @AuthenticationPrincipal Long userId
    ) {
        knowledgeDocumentService.delete(documentId, userId);
        return ResponseEntity.noContent().build();
    }

    private String stripExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx > 0 ? filename.substring(0, idx) : filename;
    }

    public record UploadDocumentResponse(Long documentId) {
    }

    public record DownloadUrlResponse(String downloadUrl) {
    }

    public record DocumentResponse(
            Long documentId,
            Long ownerId,
            String title,
            String filename,
            String status,
            int chunkCount,
            Long fileSize,
            LocalDateTime createdTime
    ) {
        static DocumentResponse from(KnowledgeDocumentJpaEntity entity) {
            return new DocumentResponse(
                    entity.getId(),
                    entity.getOwnerId(),
                    entity.getTitle(),
                    entity.getFilename(),
                    entity.getStatus().name(),
                    entity.getChunkCount(),
                    entity.getFileSize(),
                    entity.getCreatedTime()
            );
        }
    }
}
