package com.ssafy.ssafy_project.roomparticipant.adapter.in.web;

import com.ssafy.ssafy_project.roomparticipant.adapter.in.web.dto.request.RoomParticipantCreateRequest;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.RoomParticipantCreateCommand;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.RoomParticipantCreatePortIn;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/room-participants")
public class RoomParticipantController {
    private final RoomParticipantCreatePortIn roomParticipantCreatePortIn;

    @PostMapping
    public ResponseEntity<Void> createParticipant(
            @RequestBody RoomParticipantCreateRequest roomParticipantCreateRequest,
            @AuthenticationPrincipal Long userId
    ){
        String roomCode = roomParticipantCreateRequest.roomCode();
        RoomParticipantCreateCommand roomParticipantCreateCommand =
                new RoomParticipantCreateCommand(roomCode, userId);
        roomParticipantCreatePortIn.createParticipant(roomParticipantCreateCommand);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
