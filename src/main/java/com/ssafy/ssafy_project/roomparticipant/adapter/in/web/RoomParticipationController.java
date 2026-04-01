package com.ssafy.ssafy_project.roomparticipant.adapter.in.web;

import com.ssafy.ssafy_project.roomparticipant.adapter.in.web.dto.response.ParticipantResponse;
import com.ssafy.ssafy_project.roomparticipant.adapter.in.web.dto.response.ParticipantsResponse;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomParticipationController {
    private final SaveRoomParticipantPortIn saveRoomParticipantPortIn;
    private final LeaveRoomPortIn leaveRoomPortIn;
    private final GetParticipantsPortIn getParticipantsPortIn;

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

    @GetMapping("/{roomId}/participants")
    public ResponseEntity<ParticipantsResponse> getParticipants(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long loginUserId
    ){
        GetParticipantsCommand getParticipantsCommand = new GetParticipantsCommand(roomId, loginUserId);

        List<ParticipantResponse> participantResponses = getParticipantsPortIn.getParticipants(getParticipantsCommand)
                .stream()
                .map(gpr->new ParticipantResponse(
                        gpr.id(),
                        gpr.email(),
                        gpr.nickname(),
                        gpr.name()
                ))
                .toList();
        ParticipantsResponse participantsResponse = new ParticipantsResponse(participantResponses);
        return ResponseEntity.ok(participantsResponse);
    }


}
