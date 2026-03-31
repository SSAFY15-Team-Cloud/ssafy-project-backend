package com.ssafy.ssafy_project.room.adapter.in.web;

import com.ssafy.ssafy_project.roomparticipant.application.port.in.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomParticipationController {
    private final SaveRoomParticipantPortIn saveRoomParticipantPortIn;
    private final LeaveRoomPortIn leaveRoomPortIn;

    @PostMapping("/{roomCode}/join")
    public ResponseEntity<JoinRoomResult> joinRoom(
            @PathVariable String roomCode,
            @AuthenticationPrincipal Long userId
    ){
        JoinRoomCommand joinRoomCommand =
                new JoinRoomCommand(roomCode, userId);
        JoinRoomResult joinRoomResult = saveRoomParticipantPortIn.saveRoomParticipant(joinRoomCommand);
        return ResponseEntity.ok(joinRoomResult);
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ){
        LeaveRoomCommand leaveRoomCommand = new LeaveRoomCommand(roomId, userId);
        leaveRoomPortIn.leaveRoom(leaveRoomCommand);
        return ResponseEntity.status(HttpStatus.OK).build();
    }




}
