package com.ssafy.ssafy_project.global.infrastructure.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;

@ConfigurationProperties(prefix = "cloud.aws")
public record S3Properties(
        S3 s3,
        Region region,
        Credentials credentials
) {
    public record S3(
            String bucket,
            Long presignedUrlExpirationSeconds,
            String endpoint
    ) {
        public boolean hasEndpointOverride() {
            return endpoint != null && !endpoint.isBlank();
        }
    }

    public record Region(
           @Name("static") String value
    ) {
    }

    public record Credentials(
            String accessKey,
            String secretKey
    ) {
    }
}
