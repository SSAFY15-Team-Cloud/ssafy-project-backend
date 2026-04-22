package com.ssafy.ssafy_project.report.application.port.in;

import java.time.LocalDateTime;

public record GetReportResult(
        Long reportId,
        Long ownerId,
        Long roomId,
        String content,
        LocalDateTime createdTime,
        String title
) {
}
