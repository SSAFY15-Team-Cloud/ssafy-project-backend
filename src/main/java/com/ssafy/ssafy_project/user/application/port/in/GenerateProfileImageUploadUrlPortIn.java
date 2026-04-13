package com.ssafy.ssafy_project.user.application.port.in;

public interface GenerateProfileImageUploadUrlPortIn {
    GenerateProfileImageUploadUrlResult generateUrl(GenerateProfileImageUploadUrlCommand command);
}
