package com.ssafy.ssafy_project.audio.application.service;

import com.ssafy.ssafy_project.audio.application.event.AudioCreatedEvent;
import com.ssafy.ssafy_project.audio.application.event.AudioSttCompletedEvent;
import com.ssafy.ssafy_project.audio.application.port.in.CreateAudioMetadataCommand;
import com.ssafy.ssafy_project.audio.application.port.in.CreateAudioMetadataPortIn;
import com.ssafy.ssafy_project.audio.application.port.in.ProcessRoomAudiosPortIn;
import com.ssafy.ssafy_project.audio.application.port.out.AudioCommandPort;
import com.ssafy.ssafy_project.audio.application.port.out.AudioFilePortOut;
import com.ssafy.ssafy_project.audio.application.port.out.AudioQueryPort;
import com.ssafy.ssafy_project.audio.application.port.out.AudioTranscriptionPortOut;
import com.ssafy.ssafy_project.audio.domain.Audio;
import com.ssafy.ssafy_project.audio.domain.AudioSttStatus;
import com.ssafy.ssafy_project.audio.domain.AudioUploadStatus;
import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.room.domain.RoomStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AudioService implements CreateAudioMetadataPortIn, ProcessRoomAudiosPortIn {

    private final AudioCommandPort audioCommandPort;
    private final AudioQueryPort audioQueryPort;
    private final AudioFilePortOut audioFilePortOut;
    private final AudioTranscriptionPortOut audioTranscriptionPortOut;
    private final LoadRoomPortOut loadRoomPortOut;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void create(CreateAudioMetadataCommand command) {
        Room room = loadRoomPortOut.loadById(command.roomId());
        if (room.getStatus() == RoomStatus.ENDED) {
            throw new CustomException(CommonErrorCode.ROOM_ALREADY_ENDED);
        }

        Audio audio = Audio.builder()
                .roomId(command.roomId())
                .speakerId(command.speakerId())
                .path(command.path())
                .mimeType(command.mimeType())
                .duration(command.duration())
                .fileSize(command.fileSize())
                .startTime(command.startTime())
                .endTime(command.endTime())
                .createdTime(LocalDateTime.now())
                .uploadStatus(AudioUploadStatus.UPLOADED)
                .sttStatus(AudioSttStatus.PENDING)
                .build();

        Audio saved = audioCommandPort.save(audio);
        applicationEventPublisher.publishEvent(new AudioCreatedEvent(saved.getId(), saved.getRoomId()));
    }

    @Override
    @Transactional
    public void processRoomAudios(Long roomId) {
        List<Audio> audios = audioQueryPort.loadPendingByRoomId(roomId);

        for (Audio audio : audios) {
            processSingleAudio(audio);
        }
    }

    @Transactional
    public void processSingleAudioById(Long audioId) {
        processSingleAudio(audioQueryPort.loadById(audioId));
    }

    private void processSingleAudio(Audio audio) {
        try {
            audio.markProcessing();
            audioCommandPort.updateSttStatus(audio.getId(), audio.getSttStatus());

            byte[] bytes = audioFilePortOut.downloadAudio(audio.getPath());
            String filename = audioFilePortOut.extractFilename(audio.getPath());
            String text = audioTranscriptionPortOut.transcribe(bytes, filename);

            audioCommandPort.saveTranscription(audio.getId(), text);

            audio.markDone();
            audioCommandPort.updateSttStatus(audio.getId(), audio.getSttStatus());
            applicationEventPublisher.publishEvent(new AudioSttCompletedEvent(audio.getId(), audio.getRoomId()));
        } catch (Exception e) {
            audio.markFailed();
            audioCommandPort.updateSttStatus(audio.getId(), audio.getSttStatus());
            throw new RuntimeException("audio STT processing failed. audioId=" + audio.getId(), e);
        }
    }
}
