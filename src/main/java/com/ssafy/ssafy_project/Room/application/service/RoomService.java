package com.ssafy.ssafy_project.Room.application.service;

import com.ssafy.ssafy_project.Room.application.port.in.*;
import com.ssafy.ssafy_project.Room.application.port.out.DeleteRoomPortOut;
import com.ssafy.ssafy_project.Room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.Room.application.port.out.SaveRoomPortOut;
import com.ssafy.ssafy_project.Room.application.port.out.UpdateRoomPortOut;
import com.ssafy.ssafy_project.Room.domain.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService implements CreateRoomPortIn, UpdateRoomPortIn, DeleteRoomPortIn {

    private final SaveRoomPortOut saveRoomPortOut;
    private final UpdateRoomPortOut updateRoomPortOut;
    private final LoadRoomPortOut loadRoomPortOut;
    private final DeleteRoomPortOut deleteRoomPortOut;

    @Transactional
    @Override
    public CreateRoomResult createRoom(CreateRoomCommand createRoomCommand) {
        Room room = new Room(createRoomCommand.title(), createRoomCommand.hostId());
        Room savedRoom = saveRoomPortOut.saveRoom(room);
        return new CreateRoomResult(
                savedRoom.getRoomId(),
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

        if(!room.getStatus().equals("RUNNING")){
            throw new RuntimeException("닫힌 방은 수정할 수 없습니다.");
        }

        room.updateTitle(title);
        Room updatedRoom = updateRoomPortOut.updateRoom(room);
        return new UpdateRoomResult(
                updatedRoom.getRoomId(),
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
        if(!room.getStatus().equals("RUNNING")){
            throw new RuntimeException("이미 닫힌 방입니다.");
        }

        deleteRoomPortOut.deleteRoom(roomId);
    }
}
