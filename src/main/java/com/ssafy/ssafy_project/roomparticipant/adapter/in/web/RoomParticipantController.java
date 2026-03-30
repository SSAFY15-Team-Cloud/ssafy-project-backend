package com.ssafy.ssafy_project.roomparticipant.adapter.in.web;

import com.ssafy.ssafy_project.roomparticipant.adapter.in.web.dto.request.RoomParticipantCreateRequest;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.SaveRoomParticipantCommand;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.SaveRoomParticipantPortIn;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.LeaveRoomCommand;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.LeaveRoomPortIn;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/room-participants")
public class RoomParticipantController {
    private final SaveRoomParticipantPortIn saveRoomParticipantPortIn;
    private final LeaveRoomPortIn leaveRoomPortIn;

    @PostMapping
    public ResponseEntity<Void> saveRoomParticipant(
            @RequestBody RoomParticipantCreateRequest roomParticipantCreateRequest,
            @AuthenticationPrincipal Long userId
    ){
        String roomCode = roomParticipantCreateRequest.roomCode();
        SaveRoomParticipantCommand saveRoomParticipantCommand =
                new SaveRoomParticipantCommand(roomCode, userId);
        saveRoomParticipantPortIn.saveRoomParticipant(saveRoomParticipantCommand);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/rooms/{roomId}/me")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ){
        LeaveRoomCommand leaveRoomCommand = new LeaveRoomCommand(roomId, userId);
        leaveRoomPortIn.leaveRoom(leaveRoomCommand);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
