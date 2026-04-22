package com.ssafy.ssafy_project.room.adapter.in.web.dto.response;

import java.time.LocalDateTime;

public record GetReportResponse(
        Long reportId,
        Long ownerId,
        Long roomId,
        String content,
        LocalDateTime createdTime,
        String title
) {
}
