package com.ssafy.ssafy_project.audio.application.port.in;

public interface GenerateAudioUploadUrlPortIn {

    GenerateAudioUploadUrlResult generateUrl(GenerateAudioUploadUrlCommand command);
}
