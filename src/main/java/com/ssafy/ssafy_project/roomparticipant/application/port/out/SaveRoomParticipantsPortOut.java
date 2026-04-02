package com.ssafy.ssafy_project.roomparticipant.application.port.out;

import java.time.LocalDateTime;

public interface SaveRoomParticipantsPortOut {
    void deactivateActiveParticipantsByRoomId(Long roomId, LocalDateTime now);
    void deactivateActiveParticipantsByUserId(Long userId, LocalDateTime now);
}
