package com.ssafy.ssafy_project.room.application.port.in;

public record IssueRtcTokenResult(
        String serverUrl,
        String token,
        String roomName,
        String identity,
        String displayName
) {
}
