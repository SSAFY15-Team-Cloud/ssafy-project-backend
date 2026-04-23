package com.ssafy.ssafy_project.audio.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AudioMetadataResponse {
    private Long audioId;
    private Long roomId;
    private Long speakerId;
    private String uploadStatus;
    private String sttStatus;
}