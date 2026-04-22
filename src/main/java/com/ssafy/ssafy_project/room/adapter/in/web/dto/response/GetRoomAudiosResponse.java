package com.ssafy.ssafy_project.room.adapter.in.web.dto.response;

import java.util.List;

public record GetRoomAudiosResponse(
        Long roomId,
        List<Long> audioIds
) {
}
