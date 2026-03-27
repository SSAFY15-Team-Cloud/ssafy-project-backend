package com.ssafy.ssafy_project.roomparticipant.application.service;

import com.ssafy.ssafy_project.roomparticipant.application.port.in.RoomParticipantCreateCommand;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.RoomParticipantCreatePortIn;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.RoomParticipantCreatePortOut;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomParticipantService implements RoomParticipantCreatePortIn {
    private final RoomParticipantCreatePortOut roomParticipantCreatePortOut;

    @Transactional
    @Override
    public void createParticipant(RoomParticipantCreateCommand roomParticipantCreateCommand) {
        Long roomId = roomParticipantCreateCommand.roomId();
        Long userId = roomParticipantCreateCommand.userId();
        RoomParticipant roomParticipant = new RoomParticipant(roomId, userId);
        roomParticipantCreatePortOut.createParticipant(roomParticipant);
    }
}
