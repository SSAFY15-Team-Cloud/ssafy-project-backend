package com.ssafy.ssafy_project.replay.adapter.in.web;

import com.ssafy.ssafy_project.replay.application.service.MeetingReplayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ReplayController {

    private final MeetingReplayService meetingReplayService;

    @GetMapping("/api/rooms/{roomId}/replay")
    public ResponseEntity<MeetingReplayService.ReplayResult> getReplay(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(meetingReplayService.getReplay(roomId, userId));
    }
}
