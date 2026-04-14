package com.ssafy.ssafy_project.audio.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class Audio {

    private Long id;
    private Long roomId;
    private Long speakerId;
    private String path;
    private String mimeType;
    private LocalDateTime createdTime;
    private Integer duration;
    private Long fileSize;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AudioUploadStatus uploadStatus;
    private AudioSttStatus sttStatus;

    public void markProcessing() {
        this.sttStatus = AudioSttStatus.PROCESSING;
    }

    public void markDone() {
        this.sttStatus = AudioSttStatus.DONE;
    }

    public void markFailed() {
        this.sttStatus = AudioSttStatus.FAILED;
    }
}
