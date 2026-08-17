package com.ssafy.ssafy_project.braille.adapter.in.web;

import com.ssafy.ssafy_project.braille.application.service.BrailleLearningService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class BrailleController {

    private final BrailleLearningService brailleLearningService;

    @GetMapping("/api/rooms/{roomId}/braille/context")
    public ResponseEntity<BrailleLearningService.ClassContext> getContext(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(brailleLearningService.getContext(roomId, userId));
    }

    @PostMapping("/api/rooms/{roomId}/braille/problems")
    public ResponseEntity<BrailleLearningService.ProblemView> createProblem(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateProblemRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(brailleLearningService.createProblem(roomId, userId, request.text()));
    }

    @GetMapping("/api/rooms/{roomId}/braille/problems")
    public ResponseEntity<List<BrailleLearningService.ProblemView>> getProblems(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(brailleLearningService.getProblems(roomId, userId));
    }

    @GetMapping("/api/rooms/{roomId}/braille/board")
    public ResponseEntity<List<BrailleLearningService.ProblemBoard>> getBoard(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(brailleLearningService.getBoard(roomId, userId));
    }

    @PostMapping("/api/braille/problems/{problemId}/attempts")
    public ResponseEntity<BrailleLearningService.AttemptResult> submitAttempt(
            @PathVariable Long problemId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AttemptRequest request
    ) {
        return ResponseEntity.ok(brailleLearningService.submitAttempt(problemId, userId, request.cells()));
    }

    @GetMapping("/api/braille/skills/me")
    public ResponseEntity<List<BrailleLearningService.SkillView>> getMySkills(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(brailleLearningService.getMySkills(userId));
    }

    @PostMapping("/api/braille/review/generate")
    public ResponseEntity<List<BrailleLearningService.ProblemView>> generateReview(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(brailleLearningService.generateReview(userId));
    }

    @GetMapping("/api/braille/review")
    public ResponseEntity<List<BrailleLearningService.ProblemView>> getMyReview(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(brailleLearningService.getMyReview(userId));
    }

    public record CreateProblemRequest(
            @NotBlank @Size(max = 30) String text
    ) {
    }

    public record AttemptRequest(
            @NotEmpty @Size(max = 64) List<@NotNull Integer> cells
    ) {
    }
}
