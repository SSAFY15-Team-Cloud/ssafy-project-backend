package com.ssafy.ssafy_project.notification.adapter.in.web.dto.response;

import com.ssafy.ssafy_project.notification.application.port.in.GetNotificationResult;

import java.util.List;

public record GetNotificationResponse(
        List<GetNotificationResult> notifications
) {
}
