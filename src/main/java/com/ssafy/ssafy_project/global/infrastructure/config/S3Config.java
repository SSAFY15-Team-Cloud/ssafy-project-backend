package com.ssafy.ssafy_project.global.infrastructure.config;

import com.ssafy.ssafy_project.global.infrastructure.s3.S3Properties;
import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties({S3Properties.class, ProfileImageProperties.class})
public class S3Config {

    @Bean
    public S3Presigner s3Presigner(S3Properties s3Properties) {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(s3Properties.region().value()))
                .credentialsProvider(credentials(s3Properties));

        // MinIO 등 S3 호환 스토리지는 endpoint override + path-style 접근이 필요하다
        if (s3Properties.s3().hasEndpointOverride()) {
            builder.endpointOverride(URI.create(s3Properties.s3().endpoint()))
                    .serviceConfiguration(S3Configuration.builder()
                            .pathStyleAccessEnabled(true)
                            .build());
        }
        return builder.build();
    }

    @Bean
    public S3Client s3Client(S3Properties s3Properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3Properties.region().value()))
                .credentialsProvider(credentials(s3Properties));

        if (s3Properties.s3().hasEndpointOverride()) {
            builder.endpointOverride(URI.create(s3Properties.s3().endpoint()))
                    .forcePathStyle(true);
        }
        return builder.build();
    }

    private StaticCredentialsProvider credentials(S3Properties s3Properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        s3Properties.credentials().accessKey(),
                        s3Properties.credentials().secretKey()
                )
        );
    }
}
