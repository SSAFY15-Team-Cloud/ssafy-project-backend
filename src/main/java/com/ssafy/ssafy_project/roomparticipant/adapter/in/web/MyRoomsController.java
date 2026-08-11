package com.ssafy.ssafy_project.roomparticipant.adapter.in.web;

import com.ssafy.ssafy_project.roomparticipant.application.service.MyRoomHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MyRoomsController {

    private final MyRoomHistoryService myRoomHistoryService;

    @GetMapping("/api/users/me/rooms")
    public ResponseEntity<List<MyRoomHistoryService.MyRoomEntry>> getMyRooms(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(myRoomHistoryService.getMyRooms(userId));
    }
}
