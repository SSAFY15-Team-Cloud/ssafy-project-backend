package com.ssafy.ssafy_project.global.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.profile-image")
public record ProfileImageProperties(
    String baseUrl,
    String prefix
) {
}
