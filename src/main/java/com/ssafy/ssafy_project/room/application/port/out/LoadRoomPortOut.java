package com.ssafy.ssafy_project.room.application.port.out;

import com.ssafy.ssafy_project.room.domain.Room;

import java.util.List;

public interface LoadRoomPortOut {
    Room loadById(Long roomId);

    Room loadByRoomCode(String roomCode);

    List<Room> loadRunningRoomsByHostId(Long hostId);

}
