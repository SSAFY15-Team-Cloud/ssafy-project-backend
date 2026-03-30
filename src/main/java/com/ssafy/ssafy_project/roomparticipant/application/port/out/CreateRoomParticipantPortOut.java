package com.ssafy.ssafy_project.roomparticipant.application.port.out;

import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;

public interface CreateRoomParticipantPortOut {
    void createParticipant(RoomParticipant roomParticipant);
}
