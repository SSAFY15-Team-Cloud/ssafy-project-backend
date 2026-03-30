package com.ssafy.ssafy_project.room.application.port.out;

import com.ssafy.ssafy_project.room.domain.Room;

public interface SaveRoomPortOut {
    Room saveRoom(Room room);
}
