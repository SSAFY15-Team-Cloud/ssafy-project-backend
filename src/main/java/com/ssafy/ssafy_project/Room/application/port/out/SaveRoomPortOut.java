package com.ssafy.ssafy_project.Room.application.port.out;

import com.ssafy.ssafy_project.Room.adapter.in.web.dto.response.CreateRoomResponse;
import com.ssafy.ssafy_project.Room.domain.Room;

public interface SaveRoomPortOut {
    CreateRoomResponse saveRoom(Room room);
}
