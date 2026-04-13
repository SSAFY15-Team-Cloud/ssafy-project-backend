package com.ssafy.ssafy_project.user.application.support;

import com.ssafy.ssafy_project.global.infrastructure.config.ProfileImageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileImageUrlResolver {

    private final ProfileImageProperties profileImageProperties;


    public String resolve(String profileImageKey) {
        if(profileImageKey == null || profileImageKey.isBlank()) {
            return profileImageProperties.baseUrl() + "/" + profileImageProperties.prefix() + "/default.jpg";
        }

        return profileImageProperties.baseUrl() + "/" + profileImageKey;
    }

}
