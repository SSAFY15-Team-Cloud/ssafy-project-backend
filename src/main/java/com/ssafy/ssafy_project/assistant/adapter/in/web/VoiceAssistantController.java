package com.ssafy.ssafy_project.assistant.adapter.in.web;

import com.ssafy.ssafy_project.assistant.application.service.VoiceAssistantService;
import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class VoiceAssistantController {

    private static final long MAX_QUESTION_AUDIO_BYTES = 3L * 1024 * 1024; // 약 20초 분량이면 충분

    private final VoiceAssistantService voiceAssistantService;

    @PostMapping("/api/rooms/{roomId}/voice-ask")
    public ResponseEntity<VoiceAssistantService.VoiceAnswer> voiceAsk(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId,
            @RequestParam("audio") MultipartFile audio
    ) throws IOException {
        if (audio.isEmpty() || audio.getSize() > MAX_QUESTION_AUDIO_BYTES) {
            throw new CustomException(CommonErrorCode.VALIDATION_ERROR);
        }

        String filename = audio.getOriginalFilename() != null ? audio.getOriginalFilename() : "question.webm";
        return ResponseEntity.ok(voiceAssistantService.ask(roomId, userId, audio.getBytes(), filename));
    }
}
