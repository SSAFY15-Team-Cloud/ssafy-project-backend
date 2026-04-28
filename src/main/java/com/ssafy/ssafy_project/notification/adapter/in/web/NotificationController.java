package com.ssafy.ssafy_project.notification.adapter.in.web;

import com.ssafy.ssafy_project.notification.adapter.in.web.dto.response.CountUnreadNotificationResponse;
import com.ssafy.ssafy_project.notification.adapter.in.web.dto.response.GetNotificationResponse;
import com.ssafy.ssafy_project.notification.application.port.in.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final GetNotificationPortIn getNotificationPortIn;
    private final CountUnreadNotificationPortIn countUnreadNotificationPortIn;
    private final ReadNotificationPortIn readNotificationPortIn;
    private final DeleteNotificationPortIn deleteNotificationPortIn;

    @GetMapping
    public ResponseEntity<GetNotificationResponse> getNotification(
            @AuthenticationPrincipal Long userId,
            @RequestParam(name="is_read", defaultValue = "true") boolean isRead
    ) {
        List<GetNotificationResult> notifications = getNotificationPortIn.getNotifications(
                new GetNotificationCommand(userId, isRead)
        );

        return ResponseEntity.ok(
                new GetNotificationResponse(notifications)
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<CountUnreadNotificationResponse> countUnreadNotification(@AuthenticationPrincipal Long userId) {
        long count = countUnreadNotificationPortIn.countUnreadNotifications(userId);

        return ResponseEntity.ok(
                new CountUnreadNotificationResponse(
                        count
                )
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> readNotification(@AuthenticationPrincipal Long userId, @PathVariable Long notificationId) {

        readNotificationPortIn.read(
                new ReadNotificationCommand(
                        userId,
                        notificationId
                )
        );

        return ResponseEntity.noContent()
                .build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> readAllNotifications(@AuthenticationPrincipal Long userId) {
        readNotificationPortIn.readAll(userId);

        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@AuthenticationPrincipal Long userId, @PathVariable Long notificationId) {
        deleteNotificationPortIn.delete(new DeleteNotificationCommand(
                userId,
                notificationId
        ));

        return ResponseEntity.noContent()
                .build();
    }
}
