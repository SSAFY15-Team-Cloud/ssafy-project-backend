package com.ssafy.ssafy_project.roomparticipant.application.port.out;

import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;

import java.util.List;

public interface LoadParticipantsPortOut {
    List<RoomParticipant> loadAllByRoomIdAndIsActiveTrue(Long roomId);
}
