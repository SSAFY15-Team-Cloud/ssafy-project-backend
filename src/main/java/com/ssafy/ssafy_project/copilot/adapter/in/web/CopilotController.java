package com.ssafy.ssafy_project.copilot.adapter.in.web;

import com.ssafy.ssafy_project.copilot.application.service.MeetingCopilotService;
import com.ssafy.ssafy_project.copilot.application.service.TranslateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CopilotController {

    private final MeetingCopilotService meetingCopilotService;
    private final TranslateService translateService;

    @PostMapping("/api/rooms/{roomId}/copilot")
    public ResponseEntity<MeetingCopilotService.CopilotAnswer> ask(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AskRequest request
    ) {
        List<MeetingCopilotService.HistoryTurn> history = request.history() == null
                ? List.of()
                : request.history().stream()
                        .map(turn -> new MeetingCopilotService.HistoryTurn(turn.question(), turn.answer()))
                        .toList();
        return ResponseEntity.ok(meetingCopilotService.ask(roomId, userId, request.question(), history));
    }

    @PostMapping("/api/ai/translate")
    public ResponseEntity<TranslateService.TranslateResult> translate(
            @Valid @RequestBody TranslateRequest request
    ) {
        return ResponseEntity.ok(translateService.translate(request.texts(), request.targetLang()));
    }

    public record AskRequest(
            @NotBlank @Size(max = 500) String question,
            @Size(max = 10) List<@Valid @NotNull HistoryTurnRequest> history
    ) {
    }

    public record HistoryTurnRequest(
            @NotBlank @Size(max = 500) String question,
            @NotBlank @Size(max = 2000) String answer
    ) {
    }

    public record TranslateRequest(
            @NotEmpty @Size(max = 20) List<@NotBlank @Size(max = 1000) String> texts,
            @NotBlank String targetLang
    ) {
    }
}
