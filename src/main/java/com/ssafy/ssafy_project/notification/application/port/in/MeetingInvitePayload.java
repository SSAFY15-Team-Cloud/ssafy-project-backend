package com.ssafy.ssafy_project.notification.application.port.in;

public record MeetingInvitePayload(
        Long roomId,
        String roomCode
) implements NotificationPayload{
}
