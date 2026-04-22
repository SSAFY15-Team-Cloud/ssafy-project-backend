package com.ssafy.ssafy_project.roomparticipant.application.service;

import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.application.service.RoomTerminationProcessor;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.room.domain.RoomStatus;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.GetParticipantsResult;
import com.ssafy.ssafy_project.roomparticipant.application.port.in.*;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.CloseUserWebSocketSessionsPortOut;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.LoadParticipantsPortOut;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.SaveRoomParticipantPortOut;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipantRole;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomParticipantService implements SaveRoomParticipantPortIn, LeaveRoomPortIn,
        GetParticipantsPortIn{
    private final SaveRoomParticipantPortOut saveRoomParticipantPortOut;
    private final LoadRoomPortOut loadRoomPortOut;
    private final LoadUserPortOut loadUserPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;
    private final RoomTerminationProcessor roomTerminationProcessor;
    private final LoadParticipantsPortOut loadParticipantsPortOut;
    private final CloseUserWebSocketSessionsPortOut closeUserWebSocketSessionsPortOut;

    @Transactional
    @Override
    public JoinRoomResult saveRoomParticipant(JoinRoomCommand joinRoomCommand) {
        String roomCode = joinRoomCommand.roomCode();
        Room room = loadRoomPortOut.loadByRoomCode(roomCode);

        if(room.getStatus().equals(RoomStatus.ENDED)){
            throw new RuntimeException("이미 종료된 방입니다.");
        }


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
                closeWebSocketAfterCommit(userId);
                return;
            }
            roomParticipant.leave();
            saveRoomParticipantPortOut.saveRoomParticipant(roomParticipant);
            closeWebSocketAfterCommit(userId);
        }

        if(found.isEmpty()){
            throw new RuntimeException("방의 참가자가 아닙니다.");
        }
    }

    @Override
    public List<GetParticipantsResult> getParticipants(GetParticipantsCommand getParticipantsCommand) {
        Long roomId = getParticipantsCommand.roomId();
        Long loginUserId = getParticipantsCommand.userId();
        boolean isActiveParticipant = findRoomParticipantPortOut.existsByRoom_IdAndUser_IdAndIsActiveTrue(roomId, loginUserId);
        List<GetParticipantsResult> getParticipantsResults = List.of();
        if(isActiveParticipant){
            getParticipantsResults =  loadParticipantsPortOut.loadAllByRoomIdAndIsActiveTrue(roomId)
                    .stream()
                    .map(rp->new GetParticipantsResult(
                            rp.getUser().getId(),
                            rp.getUser().getEmail(),
                            rp.getUser().getNickname(),
                            rp.getUser().getName()
                    ))
                    .toList();
        }
        if(!isActiveParticipant){
            throw new RuntimeException("방의 참가자만 다른 참가자들을 조회할 수 있습니다.");
        }

        return getParticipantsResults;
    }

    private void closeWebSocketAfterCommit(Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    closeUserWebSocketSessionsPortOut.closeByUserId(userId);
                }
            });
            return;
        }

        closeUserWebSocketSessionsPortOut.closeByUserId(userId);
    }
}
