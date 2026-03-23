package com.ssafy.ssafy_project.Room.adapter.in.web;

import com.ssafy.ssafy_project.Room.adapter.in.web.dto.request.CreateRoomRequest;
import com.ssafy.ssafy_project.Room.adapter.in.web.dto.response.CreateRoomResponse;
import com.ssafy.ssafy_project.Room.application.port.in.CreateRoomPortIn;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms")
public class RoomController {
    private final CreateRoomPortIn createRoomPortIn;

    @PostMapping
    public ResponseEntity<CreateRoomResponse> login(@RequestBody CreateRoomRequest createRoomRequest,
                                                    @AuthenticationPrincipal Long userId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createRoomPortIn.createRoom(createRoomRequest.title(), userId));
    }
}
