package com.ssafy.ssafy_project.room.adapter.in.web;

import com.ssafy.ssafy_project.room.adapter.in.web.dto.request.CreateRoomRequest;
import com.ssafy.ssafy_project.room.adapter.in.web.dto.request.UpdateRoomRequest;
import com.ssafy.ssafy_project.room.adapter.in.web.dto.response.CreateRoomResponse;
import com.ssafy.ssafy_project.room.adapter.in.web.dto.response.UpdateRoomResponse;
import com.ssafy.ssafy_project.room.application.port.in.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomController {
    private final CreateRoomPortIn createRoomPortIn;
    private final UpdateRoomPortIn updateRoomPortIn;
    private final DeleteRoomPortIn deleteRoomPortIn;

    @PostMapping
    public ResponseEntity<CreateRoomResponse> createRoom(
            @RequestBody CreateRoomRequest createRoomRequest,
            @AuthenticationPrincipal Long userId) {
        CreateRoomCommand createRoomCommand = new CreateRoomCommand(createRoomRequest.title(), userId);
        CreateRoomResult createRoomResult = createRoomPortIn.createRoom(createRoomCommand);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateRoomResponse(
                        createRoomResult.roomId(),
                        createRoomResult.title(),
                        createRoomResult.hostId(),
                        createRoomResult.roomCode(),
                        createRoomResult.createdAt()
                ));
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<UpdateRoomResponse> updateRoom(
            @RequestBody UpdateRoomRequest updateRoomRequest,
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId) {
        UpdateRoomCommand updateRoomCommand = new UpdateRoomCommand(roomId, updateRoomRequest.title(), userId);
        UpdateRoomResult updateRoomResult = updateRoomPortIn.updateRoom(updateRoomCommand);
        return ResponseEntity.status(HttpStatus.OK)
                .body(new UpdateRoomResponse(
                        updateRoomResult.roomId(),
                        updateRoomResult.title()
                ));
    }

    @DeleteMapping("/{roomId}")
    public ResponseEntity<Void> closeRoom(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ){
        DeleteRoomCommand deleteRoomCommand = new DeleteRoomCommand(roomId, userId);
        deleteRoomPortIn.deleteRoom(deleteRoomCommand);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }


}
