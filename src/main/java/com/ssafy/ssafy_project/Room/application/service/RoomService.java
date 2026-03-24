package com.ssafy.ssafy_project.Room.application.service;

import com.ssafy.ssafy_project.Room.application.port.in.*;
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
public class RoomService implements CreateRoomPortIn, UpdateRoomPortIn {

    private final SaveRoomPortOut saveRoomPortOut;
    private final UpdateRoomPortOut updateRoomPortOut;
    private final LoadRoomPortOut loadRoomPortOut;

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
        room.updateTitle(title);
        Room updatedRoom = updateRoomPortOut.updateRoom(room);
        return new UpdateRoomResult(
                updatedRoom.getRoomId(),
                updatedRoom.getTitle()
        );
    }

}
