package com.ssafy.ssafy_project.audio.adapter.out.persistence;

import com.ssafy.ssafy_project.audio.application.port.out.AudioCommandPort;
import com.ssafy.ssafy_project.audio.application.port.out.AudioQueryPort;
import com.ssafy.ssafy_project.audio.application.port.out.TranscriptSegment;
import com.ssafy.ssafy_project.audio.domain.Audio;
import com.ssafy.ssafy_project.audio.domain.AudioSttStatus;
import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AudioPersistenceAdapter implements AudioCommandPort, AudioQueryPort {

    private final AudioJpaRepository audioJpaRepository;
    private final AudioTextJpaRepository audioTextJpaRepository;

    @Override
    public Audio save(Audio audio) {
        AudioJpaEntity savedEntity = audioJpaRepository.save(
                AudioJpaEntity.builder()
                        .id(audio.getId())
                        .roomId(audio.getRoomId())
                        .speakerId(audio.getSpeakerId())
                        .path(audio.getPath())
                        .mimeType(audio.getMimeType())
                        .createdTime(audio.getCreatedTime())
                        .duration(audio.getDuration())
                        .fileSize(audio.getFileSize())
                        .startTime(audio.getStartTime())
                        .endTime(audio.getEndTime())
                        .uploadStatus(audio.getUploadStatus())
                        .sttStatus(audio.getSttStatus())
                        .build()
        );

        return toDomain(savedEntity);
    }

    @Override
    public void updateSttStatus(Long audioId, AudioSttStatus sttStatus) {
        AudioJpaEntity audioJpaEntity = audioJpaRepository.findById(audioId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.AUDIO_NOT_FOUND));

        audioJpaEntity.updateSttStatus(sttStatus);
    }

    @Override
    public void saveTranscription(Long audioId, String text) {
        AudioTextJpaEntity audioTextJpaEntity = AudioTextJpaEntity.builder()
                .audioId(audioId)
                .text(text)
                .createdTime(LocalDateTime.now())
                .build();

        audioTextJpaRepository.save(audioTextJpaEntity);
    }

    @Override
    public Audio loadById(Long audioId) {
        AudioJpaEntity audioJpaEntity = audioJpaRepository.findById(audioId)
                .orElseThrow(() -> new CustomException(CommonErrorCode.AUDIO_NOT_FOUND));

        return toDomain(audioJpaEntity);
    }

    @Override
    public List<Long> loadAudioIdsByRoomId(Long roomId) {
        return audioJpaRepository.findAllByRoomId(roomId)
                .stream()
                .map(AudioJpaEntity::getId)
                .toList();
    }

    @Override
    public List<Audio> loadPendingByRoomId(Long roomId) {
        return audioJpaRepository.findAllByRoomIdAndSttStatus(roomId, AudioSttStatus.PENDING)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsUnfinishedByRoomId(Long roomId) {
        // FAILED는 재시도 대상이 아니므로 리포트 생성을 막지 않는다 (포함하면 리포트가 영원히 생성되지 않음)
        return audioJpaRepository.existsByRoomIdAndSttStatusIn(
                roomId,
                Set.of(AudioSttStatus.PENDING, AudioSttStatus.PROCESSING)
        );
    }

    @Override
    public List<TranscriptSegment> loadTranscriptSegmentsByRoomId(Long roomId) {
        return audioTextJpaRepository.findTranscriptSegmentsByRoomIdOrderByAudioTime(roomId)
                .stream()
                .map(segment -> new TranscriptSegment(
                        segment.getSpeakerName(),
                        segment.getStartTime(),
                        segment.getEndTime(),
                        segment.getText()
                ))
                .toList();
    }

    private Audio toDomain(AudioJpaEntity audioJpaEntity) {
        return Audio.builder()
                .id(audioJpaEntity.getId())
                .roomId(audioJpaEntity.getRoomId())
                .speakerId(audioJpaEntity.getSpeakerId())
                .path(audioJpaEntity.getPath())
                .mimeType(audioJpaEntity.getMimeType())
                .createdTime(audioJpaEntity.getCreatedTime())
                .duration(audioJpaEntity.getDuration())
                .fileSize(audioJpaEntity.getFileSize())
                .startTime(audioJpaEntity.getStartTime())
                .endTime(audioJpaEntity.getEndTime())
                .uploadStatus(audioJpaEntity.getUploadStatus())
                .sttStatus(audioJpaEntity.getSttStatus())
                .build();
    }
}
