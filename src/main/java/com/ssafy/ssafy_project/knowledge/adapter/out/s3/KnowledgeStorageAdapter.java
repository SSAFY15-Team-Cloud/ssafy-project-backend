package com.ssafy.ssafy_project.knowledge.adapter.out.s3;

import com.ssafy.ssafy_project.global.infrastructure.s3.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class KnowledgeStorageAdapter {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    public void upload(String objectKey, byte[] bytes, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(s3Properties.s3().bucket())
                        .key(objectKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(bytes)
        );
    }

    public void delete(String objectKey) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(s3Properties.s3().bucket())
                        .key(objectKey)
                        .build()
        );
    }

    public String generateDownloadUrl(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.s3().bucket())
                .key(objectKey)
                .build();

        return s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofSeconds(s3Properties.s3().presignedUrlExpirationSeconds()))
                        .getObjectRequest(getObjectRequest)
                        .build()
        ).url().toString();
    }
}
