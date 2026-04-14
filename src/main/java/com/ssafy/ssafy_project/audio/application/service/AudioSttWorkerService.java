package com.ssafy.ssafy_project.audio.application.service;

import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioJpaEntity;
import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioJpaRepository;
import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioTextJpaEntity;
import com.ssafy.ssafy_project.audio.adapter.out.persistence.AudioTextJpaRepository;
import com.ssafy.ssafy_project.audio.domain.AudioSttStatus;
import com.ssafy.ssafy_project.global.infrastructure.openai.OpenAiWhisperClient;
import com.ssafy.ssafy_project.global.infrastructure.s3.AudioS3Downloader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioSttWorkerService {

    private final AudioJpaRepository audioJpaRepository;
    private final AudioTextJpaRepository audioTextJpaRepository;
    private final AudioS3Downloader audioS3Downloader;
    private final OpenAiWhisperClient openAiWhisperClient;

    @Transactional
    public void processSingleAudioById(Long audioId) {
        AudioJpaEntity audio = audioJpaRepository.findById(audioId)
                .orElseThrow(() -> new IllegalArgumentException("audio not found. id=" + audioId));

        if (audio.getSttStatus() == AudioSttStatus.DONE) {
            log.info("Audio is already DONE. audioId={}", audioId);
            return;
        }

        try {
            audio.updateSttStatus(AudioSttStatus.PROCESSING);
            audioJpaRepository.save(audio);

            byte[] bytes = audioS3Downloader.downloadAudio(audio.getPath());
            String filename = audioS3Downloader.extractFilename(audio.getPath());
            String text = openAiWhisperClient.transcribe(bytes, filename);

            AudioTextJpaEntity audioText = AudioTextJpaEntity.builder()
                    .audioId(audio.getId())
                    .text(text)
                    .createdTime(LocalDateTime.now())
                    .build();

            audioTextJpaRepository.save(audioText);

            audio.updateSttStatus(AudioSttStatus.DONE);
            audioJpaRepository.save(audio);

            log.info("STT completed. audioId={}", audioId);
        } catch (Exception e) {
            audio.updateSttStatus(AudioSttStatus.FAILED);
            audioJpaRepository.save(audio);
            log.error("STT failed. audioId={}", audioId, e);
            throw new RuntimeException("Failed to process audio STT. audioId=" + audioId, e);
        }
    }
}
