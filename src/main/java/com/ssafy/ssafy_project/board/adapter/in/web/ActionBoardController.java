package com.ssafy.ssafy_project.board.adapter.in.web;

import com.ssafy.ssafy_project.board.adapter.out.persistence.ActionItemJpaEntity;
import com.ssafy.ssafy_project.board.application.service.ActionBoardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ActionBoardController {

    private final ActionBoardService actionBoardService;

    @GetMapping("/api/rooms/{roomId}/action-items")
    public ResponseEntity<List<ActionItemResponse>> getBoard(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(
                actionBoardService.getBoard(roomId, userId).stream().map(ActionItemResponse::from).toList()
        );
    }

    @PatchMapping("/api/action-items/{itemId}")
    public ResponseEntity<ActionItemResponse> updateStatus(
            @PathVariable Long itemId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(
                ActionItemResponse.from(actionBoardService.updateStatus(itemId, userId, request.status()))
        );
    }

    public record UpdateStatusRequest(@NotBlank String status) {
    }

    public record ActionItemResponse(
            Long id,
            Long roomId,
            String assignee,
            String task,
            String due,
            String status
    ) {
        static ActionItemResponse from(ActionItemJpaEntity entity) {
            return new ActionItemResponse(
                    entity.getId(),
                    entity.getRoomId(),
                    entity.getAssignee(),
                    entity.getTask(),
                    entity.getDue(),
                    entity.getStatus()
            );
        }
    }
}
