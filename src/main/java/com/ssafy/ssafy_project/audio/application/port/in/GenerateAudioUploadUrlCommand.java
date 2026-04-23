package com.ssafy.ssafy_project.audio.application.port.in;

public record GenerateAudioUploadUrlCommand(
        Long roomId,
        Long userId
) {
}
