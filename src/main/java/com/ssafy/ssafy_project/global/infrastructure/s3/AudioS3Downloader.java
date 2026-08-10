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
    private final S3Properties s3Properties;

    @Value("${app.audio.prefix}")
    private String audioPrefix;

    @Override
    public byte[] downloadAudio(String fullPath) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(s3Properties.s3().bucket())
                .key(extractKey(fullPath))
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

    // virtual-hosted(https://bucket.s3.../key)와 path-style(http://host/bucket/key) URL 모두 지원
    private String extractKey(String fullPath) {
        URI uri = URI.create(fullPath);
        String key = uri.getPath();
        if (key.startsWith("/")) {
            key = key.substring(1);
        }

        String bucketSegment = s3Properties.s3().bucket() + "/";
        if (key.startsWith(bucketSegment)) {
            key = key.substring(bucketSegment.length());
        }

        if (!key.startsWith(audioPrefix)) {
            throw new IllegalArgumentException("audio prefix does not match path. path=" + fullPath);
        }

        return key;
    }
}
