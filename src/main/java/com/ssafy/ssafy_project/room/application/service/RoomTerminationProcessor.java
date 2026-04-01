package com.ssafy.ssafy_project.room.application.service;

import com.ssafy.ssafy_project.room.application.port.out.DeleteRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.SaveRoomParticipantsPortOut;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RoomTerminationProcessor {
    private final DeleteRoomPortOut deleteRoomPortOut;
    private final SaveRoomParticipantsPortOut saveRoomParticipantsPortOut;

    @Transactional
    public void terminate(Room room){
        saveRoomParticipantsPortOut.deactivateActiveParticipantsByRoomId(room.getId());
        deleteRoomPortOut.deleteById(room.getId());
    }
}
