package com.ssafy.ssafy_project.Room.application.port.in;

import com.ssafy.ssafy_project.Room.adapter.in.web.dto.request.CreateRoomRequest;
import com.ssafy.ssafy_project.Room.adapter.in.web.dto.response.CreateRoomResponse;

public interface CreateRoomPortIn {
    CreateRoomResponse createRoom(String title, Long userId);
}
