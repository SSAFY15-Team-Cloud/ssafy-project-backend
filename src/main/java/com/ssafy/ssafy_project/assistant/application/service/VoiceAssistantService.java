package com.ssafy.ssafy_project.assistant.application.service;

import com.ssafy.ssafy_project.audio.application.port.out.AudioTranscriptionPortOut;
import com.ssafy.ssafy_project.copilot.application.service.MeetingCopilotService;
import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.global.infrastructure.openai.OpenAiTtsClient;
import com.ssafy.ssafy_project.knowledge.adapter.out.s3.KnowledgeStorageAdapter;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 미티니 — 회의 속 AI 음성 참가자.
 * 질문 오디오 → Whisper 전사 → 코파일럿 RAG 답변 → TTS → 회의방 전원에게 브로드캐스트.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceAssistantService {

    private final FindRoomParticipantPortOut findRoomParticipantPortOut;
    private final LoadUserPortOut loadUserPortOut;
    private final AudioTranscriptionPortOut audioTranscriptionPortOut;
    private final MeetingCopilotService meetingCopilotService;
    private final OpenAiTtsClient ttsClient;
    private final KnowledgeStorageAdapter storageAdapter;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public VoiceAnswer ask(Long roomId, Long userId, byte[] questionAudio, String filename) {
        if (!findRoomParticipantPortOut.existsByRoom_IdAndUser_IdAndIsActiveTrue(roomId, userId)) {
            throw new CustomException(CommonErrorCode.NOT_ROOM_PARTICIPANT);
        }

        String question = audioTranscriptionPortOut.transcribe(questionAudio, filename).trim();
        if (question.isBlank()) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }
        log.info("Voice question. roomId={}, userId={}, question={}", roomId, userId, question);

        MeetingCopilotService.CopilotAnswer answer = meetingCopilotService.ask(roomId, userId, question);

        byte[] speech = ttsClient.synthesize(answer.answer());
        String objectKey = "assistant/" + roomId + "/" + UUID.randomUUID() + ".mp3";
        storageAdapter.upload(objectKey, speech, "audio/mpeg");
        String audioUrl = storageAdapter.generateDownloadUrl(objectKey);

        User asker = loadUserPortOut.loadById(userId);
        String askedBy = asker.getNickname() != null ? asker.getNickname() : asker.getName();

        VoiceAnswer payload = new VoiceAnswer(
                question,
                answer.answer(),
                answer.sources(),
                audioUrl,
                askedBy,
                LocalDateTime.now()
        );

        // 회의방 전원에게 — 각 클라이언트가 같은 음성을 재생해 'AI가 회의에서 말하는' 경험을 만든다
        simpMessagingTemplate.convertAndSend("/sub/rooms/" + roomId + "/ai/voice", payload);

        return payload;
    }

    public record VoiceAnswer(
            String question,
            String answer,
            List<MeetingCopilotService.CopilotSource> sources,
            String audioUrl,
            String askedBy,
            LocalDateTime answeredTime
    ) {
    }
}
