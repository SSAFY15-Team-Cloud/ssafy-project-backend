package com.ssafy.ssafy_project.notification.application.service;

import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.notification.application.port.in.*;
import com.ssafy.ssafy_project.notification.application.port.out.DeleteNotificationPortOut;
import com.ssafy.ssafy_project.notification.application.port.out.LoadNotificationPortOut;
import com.ssafy.ssafy_project.notification.application.port.out.UpdateNotificationPortOut;
import com.ssafy.ssafy_project.notification.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService implements GetNotificationPortIn, ReadNotificationPortIn, CountUnreadNotificationPortIn, DeleteNotificationPortIn {
    private final LoadNotificationPortOut loadNotificationPortOut;
    private final UpdateNotificationPortOut updateNotificationPortOut;
    private final DeleteNotificationPortOut deleteNotificationPortOut;


    @Override
    public List<GetNotificationResult> getNotifications(GetNotificationCommand command) {
        if(!command.isRead()) {
            return loadNotificationPortOut.loadUnreadByToId(command.userId())
                    .stream()
                    .map(this::toGetNotificationResult)
                    .toList();
        }

        return loadNotificationPortOut.loadAllByToId(command.userId())
                .stream()
                .map(this::toGetNotificationResult)
                .toList();
    }

    @Transactional
    @Override
    public void read(ReadNotificationCommand command) {
        Notification notification = loadNotificationPortOut.loadByIdAndToId(command.notificationId(), command.userId())
                .orElseThrow(() -> new CustomException(CommonErrorCode.NOTIFICATION_NOT_FOUND));

        if(notification.isRead()) {
            return;
        }

        updateNotificationPortOut.markAsRead(notification.getId(), command.userId(), LocalDateTime.now());
    }

    @Transactional
    @Override
    public void readAll(Long userId) {
        updateNotificationPortOut.markAllAsRead(userId, LocalDateTime.now());
    }

    @Override
    public long countUnreadNotifications(Long userId) {
        return loadNotificationPortOut.countUnreadByToId(userId);
    }

    @Transactional
    @Override
    public void delete(DeleteNotificationCommand command) {
        loadNotificationPortOut.loadByIdAndToId(
                command.notificationId(),
                command.userId()
        ).orElseThrow(()-> new CustomException(CommonErrorCode.NOTIFICATION_NOT_FOUND));

        deleteNotificationPortOut.deleteById(command.notificationId());
    }

    private GetNotificationResult toGetNotificationResult(Notification notification) {
        validateNotificationPayload(notification);

        NotificationPayload payload = switch (notification.getType()) {
            case MEETING_INVITE -> new MeetingInvitePayload(
                    notification.getRoomId(),
                    notification.getRoomCode()
            );
            case REPORT_DONE -> new ReportDonePayload(
                notification.getReportId()
            );
        };

        return new GetNotificationResult(
                notification.getId(),
                notification.getType(),
                notification.getContent(),
                notification.getCreatedTime(),
                notification.isRead(),
                payload
        );
    }

    private void validateNotificationPayload(Notification notification) {
        switch (notification.getType()) {
            case MEETING_INVITE -> {
                if (notification.getRoomId() == null || notification.getRoomCode() == null) {
                    throw new CustomException(CommonErrorCode.INVALID_NOTIFICATION_PAYLOAD);
                }
            }
            case REPORT_DONE -> {
                if (notification.getReportId() == null) {
                    throw new CustomException(CommonErrorCode.INVALID_NOTIFICATION_PAYLOAD);
                }
            }
        }
    }
}
