package com.ssafy.ssafy_project.global.infrastructure.s3;

import com.ssafy.ssafy_project.audio.application.port.out.AudioFilePortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.net.URI;

@Component
@RequiredArgsConstructor
public class AudioS3Downloader implements AudioFilePortOut {

    private final S3Client s3Client;

    @Value("${app.profile-image.base-url}")
    private String baseUrl;

    @Value("${app.audio.prefix}")
    private String audioPrefix;

    @Override
    public byte[] downloadAudio(String fullPath) {
        String bucket = extractBucketName();
        String key = extractKey(fullPath);

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        return response.asByteArray();
    }

    @Override
    public String extractFilename(String fullPath) {
        String key = extractKey(fullPath);
        int idx = key.lastIndexOf('/');
        return idx >= 0 ? key.substring(idx + 1) : key;
    }

    private String extractBucketName() {
        URI uri = URI.create(baseUrl);
        String host = uri.getHost();
        int idx = host.indexOf(".s3.");
        if (idx < 0) {
            throw new IllegalStateException("S3 bucket host format is invalid: " + host);
        }
        return host.substring(0, idx);
    }

    private String extractKey(String fullPath) {
        URI uri = URI.create(fullPath);
        String key = uri.getPath();
        if (key.startsWith("/")) {
            key = key.substring(1);
        }

        if (!key.startsWith(audioPrefix)) {
            throw new IllegalArgumentException("audio prefix does not match path. path=" + fullPath);
        }

        return key;
    }
}
