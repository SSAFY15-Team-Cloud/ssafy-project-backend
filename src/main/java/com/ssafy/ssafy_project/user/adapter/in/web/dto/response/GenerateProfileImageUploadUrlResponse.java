package com.ssafy.ssafy_project.user.adapter.in.web.dto.response;

public record GenerateProfileImageUploadUrlResponse(
        String uploadUrl,
        String objectKey
) {
}
