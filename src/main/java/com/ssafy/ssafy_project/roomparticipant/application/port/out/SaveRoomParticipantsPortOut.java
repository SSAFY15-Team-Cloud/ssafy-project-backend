package com.ssafy.ssafy_project.roomparticipant.application.port.out;

public interface SaveRoomParticipantsPortOut {
    void deactivateActiveParticipantsByRoomId(Long roomId);
}
