package com.ssafy.ssafy_project.audio.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class CreateAudioRequest {

    // speakerId는 인증 주체(@AuthenticationPrincipal)에서 얻는다 — 요청 바디로 받지 않는다

    @NotBlank
    private String path;

    @NotBlank
    private String mimeType;

    private Integer duration;

    private Long fileSize;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
