package com.ssafy.ssafy_project.roomparticipant.application.port.out;

import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;

import java.time.LocalDateTime;

public interface SaveRoomParticipantPortOut {
    void saveRoomParticipant(RoomParticipant roomParticipant);

}
