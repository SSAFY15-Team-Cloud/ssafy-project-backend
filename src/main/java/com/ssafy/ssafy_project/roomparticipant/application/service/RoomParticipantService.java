package com.ssafy.ssafy_project.roomparticipant.application.service;

import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.application.service.RoomTerminationProcessor;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.*;
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
    private final RoomTerminationProcessor roomTerminationProcessor;

    @Transactional
    @Override
    public JoinRoomResult saveRoomParticipant(JoinRoomCommand joinRoomCommand) {
        String roomCode = joinRoomCommand.roomCode();
        Room room = loadRoomPortOut.loadByRoomCode(roomCode);

        Long userId = joinRoomCommand.userId();
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
        return new JoinRoomResult(
                room.getId(),
                room.getTitle(),
                room.getStatus()
        );
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
            if(roomParticipant.getUser().getId().equals(room.getHostId())){
                roomTerminationProcessor.terminate(room);
                return;
            }
            roomParticipant.leave();
            saveRoomParticipantPortOut.saveRoomParticipant(roomParticipant);
        }

        if(found.isEmpty()){
            throw new RuntimeException("방의 참가자가 아닙니다.");
        }
    }
}
