package com.ssafy.ssafy_project.Room.application.service;

import com.ssafy.ssafy_project.Room.adapter.in.web.dto.request.CreateRoomRequest;
import com.ssafy.ssafy_project.Room.adapter.in.web.dto.response.CreateRoomResponse;
import com.ssafy.ssafy_project.Room.application.port.in.CreateRoomPortIn;
import com.ssafy.ssafy_project.Room.application.port.out.SaveRoomPortOut;
import com.ssafy.ssafy_project.Room.domain.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomService implements CreateRoomPortIn {

    private final SaveRoomPortOut saveRoomPortOut;

    @Override
    public CreateRoomResponse createRoom(String title, Long userId) {
        return saveRoomPortOut.saveRoom(new Room(title, userId));
    }
}
