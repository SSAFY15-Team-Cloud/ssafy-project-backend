package com.ssafy.ssafy_project.roomparticipant.application.service;

import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.RoomParticipantCreateCommand;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.RoomParticipantCreatePortIn;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.RoomParticipantCreatePortOut;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomParticipantService implements RoomParticipantCreatePortIn {
    private final RoomParticipantCreatePortOut roomParticipantCreatePortOut;
    private final LoadRoomPortOut loadRoomPortOut;
    private final LoadUserPortOut loadUserPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;

    @Transactional
    @Override
    public void createParticipant(RoomParticipantCreateCommand roomParticipantCreateCommand) {
        String roomCode = roomParticipantCreateCommand.roomCode();
        Room room = loadRoomPortOut.loadByRoomCode(roomCode);

        Long userId = roomParticipantCreateCommand.userId();
        User user = loadUserPortOut.loadById(userId);

        Optional<RoomParticipant> found = findRoomParticipantPortOut.findByRoomAndUser(room, user);

        if(found.isPresent()){
            RoomParticipant roomParticipant = found.get();
            roomParticipant.join();
            roomParticipantCreatePortOut.createParticipant(roomParticipant);
        }

        if(found.isEmpty()){
            RoomParticipant roomParticipant = new RoomParticipant(room, user);
            roomParticipantCreatePortOut.createParticipant(roomParticipant);
        }
    }
}
