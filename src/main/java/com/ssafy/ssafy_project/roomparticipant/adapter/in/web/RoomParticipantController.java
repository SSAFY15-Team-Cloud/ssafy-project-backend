package com.ssafy.ssafy_project.roomparticipant.adapter.in.web;

import com.ssafy.ssafy_project.roomparticipant.application.port.in.RoomParticipantCreateCommand;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.RoomParticipantCreatePortIn;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{roomId}/participants")
public class RoomParticipantController {
    private final RoomParticipantCreatePortIn roomParticipantCreatePortIn;

    @PostMapping
    public ResponseEntity<Void> createParticipant(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ){
        RoomParticipantCreateCommand roomParticipantCreateCommand =
                new RoomParticipantCreateCommand(roomId, userId);
        roomParticipantCreatePortIn.createParticipant(roomParticipantCreateCommand);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
