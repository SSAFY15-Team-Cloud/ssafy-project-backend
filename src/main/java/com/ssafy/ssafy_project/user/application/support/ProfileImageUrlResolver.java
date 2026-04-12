package com.ssafy.ssafy_project.user.application.support;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileImageUrlResolver {

    private static final String DEFAULT_IMAGE_KEY = "default.jpg";

    @Value("${app.profile-image.base-url}")
    private String baseUrl;

    public String resolve(String profileImageKey) {
        if(profileImageKey == null || profileImageKey.isBlank()) {
            return baseUrl + "/" + DEFAULT_IMAGE_KEY;
        }

        return baseUrl + "/" + profileImageKey;
    }

}
