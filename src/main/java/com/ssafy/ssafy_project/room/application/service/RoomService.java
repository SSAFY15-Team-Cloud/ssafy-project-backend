package com.ssafy.ssafy_project.room.application.service;

import com.ssafy.ssafy_project.room.application.port.in.*;
import com.ssafy.ssafy_project.room.application.port.out.DeleteRoomPortOut;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.application.port.out.SaveRoomPortOut;
import com.ssafy.ssafy_project.room.application.port.out.UpdateRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.SaveRoomParticipantPortOut;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.LoadActiveRoomParticipantPortOut;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.SaveRoomParticipantsPortOut;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipantRole;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService implements CreateRoomPortIn, UpdateRoomPortIn, DeleteRoomPortIn {

    private final SaveRoomPortOut saveRoomPortOut;
    private final UpdateRoomPortOut updateRoomPortOut;
    private final LoadRoomPortOut loadRoomPortOut;
    private final LoadUserPortOut loadUserPortOut;
    private final DeleteRoomPortOut deleteRoomPortOut;
    private final SaveRoomParticipantPortOut saveRoomParticipantPortOut;
    private final LoadActiveRoomParticipantPortOut loadActiveRoomParticipantPortOut;
    private final SaveRoomParticipantsPortOut saveRoomParticipantsPortOut;

    @Transactional
    @Override
    public CreateRoomResult createRoom(CreateRoomCommand createRoomCommand) {
        Room room = new Room(createRoomCommand.title(), createRoomCommand.hostId());
        Room savedRoom = saveRoomPortOut.saveRoom(room);
        User host = loadUserPortOut.loadById(savedRoom.getHostId());
        RoomParticipant roomParticipant = new RoomParticipant(savedRoom, host, RoomParticipantRole.OWNER);
        saveRoomParticipantPortOut.saveRoomParticipant(roomParticipant);
        return new CreateRoomResult(
                savedRoom.getId(),
                savedRoom.getTitle(),
                savedRoom.getHostId(),
                savedRoom.getRoomCode(),
                savedRoom.getCreatedAt()
        );
    }

    @Transactional
    @Override
    public UpdateRoomResult updateRoom(UpdateRoomCommand updateRoomCommand) {
        Long roomId = updateRoomCommand.roomId();
        Long userId = updateRoomCommand.userId();
        String title = updateRoomCommand.title();
        Room room = loadRoomPortOut.loadById(roomId);
        if(!room.getHostId().equals(userId)){
            throw new RuntimeException("방 생성자만 수정할 수 있습니다.");
        }

        if(!"RUNNING".equals(room.getStatus())){
            throw new RuntimeException("닫힌 방은 수정할 수 없습니다.");
        }

        room.updateTitle(title);
        Room updatedRoom = updateRoomPortOut.updateRoom(room);
        return new UpdateRoomResult(
                updatedRoom.getId(),
                updatedRoom.getTitle()
        );
    }

    @Transactional
    @Override
    public void deleteRoom(DeleteRoomCommand deleteRoomCommand) {
        Long roomId = deleteRoomCommand.roomId();
        Long userId = deleteRoomCommand.userId();
        Room room = loadRoomPortOut.loadById(roomId);
        if(!room.getHostId().equals(userId)){
            throw new RuntimeException("작성자만 방을 삭제할 수 있습니다.");
        }
        if(!"RUNNING".equals(room.getStatus())){
            throw new RuntimeException("이미 닫힌 방입니다.");
        }
        List<RoomParticipant> roomParticipants = loadActiveRoomParticipantPortOut.loadActiveRoomParticipantsByRoomId(roomId);
        for(RoomParticipant rp: roomParticipants){
            rp.leave();
        }
        saveRoomParticipantsPortOut.saveRoomParticipants(roomParticipants);


        deleteRoomPortOut.deleteById(roomId);
    }
}
