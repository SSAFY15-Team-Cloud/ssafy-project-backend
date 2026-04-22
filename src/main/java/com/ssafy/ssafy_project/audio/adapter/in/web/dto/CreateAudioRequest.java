package com.ssafy.ssafy_project.audio.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
public class CreateAudioRequest {

    @NotNull
    private Long speakerId;

    @NotBlank
    private String path;

    @NotBlank
    private String mimeType;

    private Integer duration;

    private Long fileSize;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}