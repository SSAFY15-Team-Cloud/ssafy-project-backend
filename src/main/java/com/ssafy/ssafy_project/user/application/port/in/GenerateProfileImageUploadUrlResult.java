package com.ssafy.ssafy_project.user.application.port.in;

public record GenerateProfileImageUploadUrlResult(
        String uploadUrl,
        String objectKey
) {
}
