package com.ssafy.ssafy_project.roomparticipant.application.port.out;

import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;

import java.util.List;

public interface LoadActiveRoomParticipantPortOut {
    List<RoomParticipant>  loadActiveRoomParticipantsByRoomId(Long roomId);
    List<RoomParticipant> loadActiveRoomParticipantsByUserId(Long userId);
}
