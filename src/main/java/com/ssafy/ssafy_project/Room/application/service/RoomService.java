package com.ssafy.ssafy_project.Room.application.service;

import com.ssafy.ssafy_project.Room.application.port.in.CreateRoomCommand;
import com.ssafy.ssafy_project.Room.application.port.in.CreateRoomPortIn;
import com.ssafy.ssafy_project.Room.application.port.in.CreateRoomResult;
import com.ssafy.ssafy_project.Room.application.port.out.SaveRoomPortOut;
import com.ssafy.ssafy_project.Room.domain.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomService implements CreateRoomPortIn {

    private final SaveRoomPortOut saveRoomPortOut;

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
}
