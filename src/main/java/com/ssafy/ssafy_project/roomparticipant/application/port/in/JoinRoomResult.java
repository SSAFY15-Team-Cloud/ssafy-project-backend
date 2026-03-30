package com.ssafy.ssafy_project.roomparticipant.application.port.in;

import com.ssafy.ssafy_project.room.domain.RoomStatus;

public record JoinRoomResult(
        Long roomId,
        String title,
        RoomStatus status
) {
}
