package com.ssafy.ssafy_project.roomparticipant.application.service;

import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.SaveRoomParticipantCommand;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.SaveRoomParticipantPortIn;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.LeaveRoomCommand;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.LeaveRoomPortIn;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.SaveRoomParticipantPortOut;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipantRole;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomParticipantService implements SaveRoomParticipantPortIn, LeaveRoomPortIn {
    private final SaveRoomParticipantPortOut saveRoomParticipantPortOut;
    private final LoadRoomPortOut loadRoomPortOut;
    private final LoadUserPortOut loadUserPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;

    @Transactional
    @Override
    public void saveRoomParticipant(SaveRoomParticipantCommand saveRoomParticipantCommand) {
        String roomCode = saveRoomParticipantCommand.roomCode();
        Room room = loadRoomPortOut.loadByRoomCode(roomCode);

        Long userId = saveRoomParticipantCommand.userId();
        User user = loadUserPortOut.loadById(userId);

        Optional<RoomParticipant> found = findRoomParticipantPortOut.findByRoomAndUser(room, user);

        if(found.isPresent()){
            RoomParticipant roomParticipant = found.get();
            roomParticipant.join();
            saveRoomParticipantPortOut.saveRoomParticipant(roomParticipant);
        }

        if(found.isEmpty()){
            RoomParticipant roomParticipant = new RoomParticipant(room, user, RoomParticipantRole.PARTICIPANT);
            saveRoomParticipantPortOut.saveRoomParticipant(roomParticipant);
        }
    }

    @Transactional
    @Override
    public void leaveRoom(LeaveRoomCommand leaveRoomCommand) {
        Long roomId = leaveRoomCommand.roomId();
        Room room = loadRoomPortOut.loadById(roomId);

        Long userId = leaveRoomCommand.userId();
        User user = loadUserPortOut.loadById(userId);

        Optional<RoomParticipant> found = findRoomParticipantPortOut.findByRoomAndUserAndIsActiveTrue(room, user);

        if(found.isPresent()){
            RoomParticipant roomParticipant = found.get();
            roomParticipant.leave();
            saveRoomParticipantPortOut.saveRoomParticipant(roomParticipant);
        }

        if(found.isEmpty()){
            throw new RuntimeException("방의 참가자가 아닙니다.");
        }
    }
}
