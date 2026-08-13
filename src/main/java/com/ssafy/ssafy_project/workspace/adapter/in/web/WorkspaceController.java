package com.ssafy.ssafy_project.workspace.adapter.in.web;

import com.ssafy.ssafy_project.workspace.application.service.MeetingBriefingService;
import com.ssafy.ssafy_project.workspace.application.service.WorkspaceOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceOverviewService workspaceOverviewService;
    private final MeetingBriefingService meetingBriefingService;

    @GetMapping("/api/users/me/overview")
    public ResponseEntity<WorkspaceOverviewService.Overview> getOverview(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(workspaceOverviewService.getOverview(userId));
    }

    @GetMapping("/api/rooms/{roomId}/briefing")
    public ResponseEntity<MeetingBriefingService.Briefing> getBriefing(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(meetingBriefingService.getBriefing(roomId, userId));
    }
}
