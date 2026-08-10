package com.ssafy.ssafy_project.room.application.port.in;

public record IssueRtcTokenCommand(
        Long roomId,
        Long userId
) {
}
