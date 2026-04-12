package com.ssafy.ssafy_project.user.adapter.out.s3;

import com.ssafy.ssafy_project.global.infrastructure.s3.S3Properties;
import com.ssafy.ssafy_project.user.application.port.out.ProfileImageStoragePortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class ProfileImageS3Adapter implements ProfileImageStoragePortOut {
    private static final String CONTENT_TYPE = "image/jpeg";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public String generateUploadUrl(String objectKey) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.s3().bucket())
                .key(objectKey)
                .contentType(CONTENT_TYPE)
                .build();

        PutObjectPresignRequest putObjectPresignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(s3Properties.s3().presignedUrlExpirationSeconds()))
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(putObjectPresignRequest).url().toString();
    }

    @Override
    public void delete(String objectKey) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3Properties.s3().bucket())
                .key(objectKey)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }
}
