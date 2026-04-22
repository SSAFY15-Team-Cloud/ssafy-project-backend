package com.ssafy.ssafy_project.audio.adapter.out.s3;

import com.ssafy.ssafy_project.audio.application.port.out.AudioStoragePortOut;
import com.ssafy.ssafy_project.global.infrastructure.s3.S3Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class AudioS3UploadAdapter implements AudioStoragePortOut {

    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public String generateUploadUrl(String objectKey) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Properties.s3().bucket())
                .key(objectKey)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(s3Properties.s3().presignedUrlExpirationSeconds()))
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }
}
