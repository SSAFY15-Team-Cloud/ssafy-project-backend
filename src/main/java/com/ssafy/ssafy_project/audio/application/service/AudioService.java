package com.ssafy.ssafy_project.audio.application.service;

import com.ssafy.ssafy_project.audio.application.event.AudioCreatedEvent;
import com.ssafy.ssafy_project.audio.application.event.AudioSttCompletedEvent;
import com.ssafy.ssafy_project.audio.application.port.in.CreateAudioMetadataCommand;
import com.ssafy.ssafy_project.audio.application.port.in.CreateAudioMetadataPortIn;
import com.ssafy.ssafy_project.audio.application.port.in.GenerateAudioUploadUrlCommand;
import com.ssafy.ssafy_project.audio.application.port.in.GenerateAudioUploadUrlPortIn;
import com.ssafy.ssafy_project.audio.application.port.in.GenerateAudioUploadUrlResult;
import com.ssafy.ssafy_project.audio.application.port.in.ProcessRoomAudiosPortIn;
import com.ssafy.ssafy_project.audio.application.port.out.AudioCommandPort;
import com.ssafy.ssafy_project.audio.application.port.out.AudioFilePortOut;
import com.ssafy.ssafy_project.audio.application.port.out.AudioQueryPort;
import com.ssafy.ssafy_project.audio.application.port.out.AudioStoragePortOut;
import com.ssafy.ssafy_project.audio.application.port.out.AudioTranscriptionPortOut;
import com.ssafy.ssafy_project.audio.domain.Audio;
import com.ssafy.ssafy_project.audio.domain.AudioSttStatus;
import com.ssafy.ssafy_project.audio.domain.AudioUploadStatus;
import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.room.domain.RoomStatus;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AudioService implements CreateAudioMetadataPortIn, ProcessRoomAudiosPortIn, GenerateAudioUploadUrlPortIn {

    private static final String DEFAULT_AUDIO_EXTENSION = ".ogg";
    private static final List<String> ALLOWED_AUDIO_EXTENSIONS = List.of("ogg", "webm", "mp3", "wav", "m4a");

    private final AudioCommandPort audioCommandPort;
    private final AudioQueryPort audioQueryPort;
    private final AudioFilePortOut audioFilePortOut;
    private final AudioStoragePortOut audioStoragePortOut;
    private final AudioTranscriptionPortOut audioTranscriptionPortOut;
    private final LoadRoomPortOut loadRoomPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${app.audio.prefix}")
    private String audioPrefix;

    @Override
    @Transactional
    public void create(CreateAudioMetadataCommand command) {
        validateRoomIsUploadable(command.roomId());
        validateActiveParticipant(command.roomId(), command.speakerId());

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
    public GenerateAudioUploadUrlResult generateUrl(GenerateAudioUploadUrlCommand command) {
        validateRoomIsUploadable(command.roomId());
        validateActiveParticipant(command.roomId(), command.userId());

        String objectKey = generateAudioObjectKey(command.roomId(), resolveExtension(command.extension()));
        String uploadUrl = audioStoragePortOut.generateUploadUrl(objectKey);
        return new GenerateAudioUploadUrlResult(uploadUrl);
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

    private void validateRoomIsUploadable(Long roomId) {
        Room room = loadRoomPortOut.loadById(roomId);
        if (room.getStatus() == RoomStatus.ENDED) {
            throw new CustomException(CommonErrorCode.ROOM_ALREADY_ENDED);
        }
    }

    private void validateActiveParticipant(Long roomId, Long userId) {
        if (!findRoomParticipantPortOut.existsByRoom_IdAndUser_IdAndIsActiveTrue(roomId, userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }
    }

    private String generateAudioObjectKey(Long roomId, String extension) {
        return normalizePrefix(audioPrefix)
                + roomId
                + "/"
                + UUID.randomUUID()
                + extension;
    }

    private String resolveExtension(String requested) {
        if (requested == null || requested.isBlank()) {
            return DEFAULT_AUDIO_EXTENSION;
        }
        String normalized = requested.toLowerCase().replace(".", "");
        if (!ALLOWED_AUDIO_EXTENSIONS.contains(normalized)) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }
        return "." + normalized;
    }

    private String normalizePrefix(String prefix) {
        if (prefix.endsWith("/")) {
            return prefix;
        }

        return prefix + "/";
    }
}
