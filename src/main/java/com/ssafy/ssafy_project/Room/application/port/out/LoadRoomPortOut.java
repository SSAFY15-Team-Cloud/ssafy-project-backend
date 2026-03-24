package com.ssafy.ssafy_project.Room.application.port.out;

import com.ssafy.ssafy_project.Room.domain.Room;

public interface LoadRoomPortOut {
    Room loadById(Long roomId);
}
