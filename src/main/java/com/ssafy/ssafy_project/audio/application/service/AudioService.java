package com.ssafy.ssafy_project.audio.application.service;

import com.ssafy.ssafy_project.audio.adapter.in.web.dto.CreateAudioRequest;
import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioJpaEntity;
import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioJpaRepository;
import com.ssafy.ssafy_project.audio.application.port.in.CreateAudioMetadataPortIn;
import com.ssafy.ssafy_project.audio.domain.AudioSttStatus;
import com.ssafy.ssafy_project.audio.domain.AudioUploadStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class AudioService implements CreateAudioMetadataPortIn {

    private final AudioJpaRepository audioJpaRepository;

    @Override
    public void create(Long roomId, CreateAudioRequest request) {
        AudioJpaEntity entity = AudioJpaEntity.builder()
                .roomId(roomId)
                .speakerId(request.getSpeakerId())
                .path(request.getPath())
                .mimeType(request.getMimeType())
                .duration(request.getDuration())
                .fileSize(request.getFileSize())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .createdTime(LocalDateTime.now())
                .uploadStatus(AudioUploadStatus.UPLOADED)
                .sttStatus(AudioSttStatus.PENDING)
                .build();

        audioJpaRepository.save(entity);
    }
}