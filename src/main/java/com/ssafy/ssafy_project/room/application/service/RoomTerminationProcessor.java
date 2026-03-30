package com.ssafy.ssafy_project.room.application.service;

import com.ssafy.ssafy_project.room.application.port.out.DeleteRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.LoadActiveRoomParticipantPortOut;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.SaveRoomParticipantsPortOut;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoomTerminationProcessor {
    private final DeleteRoomPortOut deleteRoomPortOut;
    private final SaveRoomParticipantsPortOut saveRoomParticipantsPortOut;
    private final LoadActiveRoomParticipantPortOut loadActiveRoomParticipantPortOut;

    @Transactional
    public void terminate(Room room){
        List<RoomParticipant> roomParticipants = loadActiveRoomParticipantPortOut.loadActiveRoomParticipantsByRoomId(room.getId());
        for(RoomParticipant rp: roomParticipants){
            rp.leave();
        }
        saveRoomParticipantsPortOut.saveRoomParticipants(roomParticipants);
        deleteRoomPortOut.deleteById(room.getId());
    }
}
