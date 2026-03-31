package com.ssafy.ssafy_project.roomparticipant.application.port.in;

public interface SaveRoomParticipantPortIn {
    JoinRoomResult saveRoomParticipant(JoinRoomCommand joinRoomCommand);
}
