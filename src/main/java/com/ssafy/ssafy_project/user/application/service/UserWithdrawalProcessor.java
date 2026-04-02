package com.ssafy.ssafy_project.user.application.service;

import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.application.service.RoomTerminationProcessor;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.LoadActiveRoomParticipantPortOut;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.SaveRoomParticipantsPortOut;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserWithdrawalProcessor {

    private final LoadRoomPortOut loadRoomPortOut;
    private final LoadActiveRoomParticipantPortOut loadActiveRoomParticipantPortOut;
    private final SaveRoomParticipantsPortOut saveRoomParticipantsPortOut;
    private final RoomTerminationProcessor roomTerminationProcessor;

    @Transactional
    public void process(Long userId) {
        terminateHostedRooms(userId);
        leaveOtherActiveParticipants(userId);
    }

    private void terminateHostedRooms(Long userId) {
        List<Room> rooms = loadRoomPortOut.loadRunningRoomsByHostId(userId);

        for(Room room : rooms) {
            roomTerminationProcessor.terminate(room);
        }
    }

    private void leaveOtherActiveParticipants(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        saveRoomParticipantsPortOut.deactivateActiveParticipantsByUserId(userId, now);
    }


}
