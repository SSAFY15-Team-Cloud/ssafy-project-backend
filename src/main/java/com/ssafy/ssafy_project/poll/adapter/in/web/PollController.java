package com.ssafy.ssafy_project.poll.adapter.in.web;

import com.ssafy.ssafy_project.poll.application.service.PollService;
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
public class PollController {

    private final PollService pollService;

    @PostMapping("/api/rooms/{roomId}/polls")
    public ResponseEntity<PollService.PollView> createPoll(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePollRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pollService.createPoll(roomId, userId, request.question(), request.options()));
    }

    @GetMapping("/api/rooms/{roomId}/polls")
    public ResponseEntity<List<PollService.PollView>> getPolls(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(pollService.getPolls(roomId, userId));
    }

    @PostMapping("/api/polls/{pollId}/vote")
    public ResponseEntity<PollService.PollView> vote(
            @PathVariable Long pollId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VoteRequest request
    ) {
        return ResponseEntity.ok(pollService.vote(pollId, userId, request.optionIndex()));
    }

    @PostMapping("/api/polls/{pollId}/close")
    public ResponseEntity<PollService.PollView> close(
            @PathVariable Long pollId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(pollService.closePoll(pollId, userId));
    }

    public record CreatePollRequest(
            @NotBlank @Size(max = 200) String question,
            @NotEmpty @Size(min = 2, max = 6) List<@NotBlank @Size(max = 100) String> options
    ) {
    }

    public record VoteRequest(@NotNull Integer optionIndex) {
    }
}
