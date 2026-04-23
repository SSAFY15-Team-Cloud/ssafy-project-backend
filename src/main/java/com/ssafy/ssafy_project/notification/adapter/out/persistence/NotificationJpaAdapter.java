package com.ssafy.ssafy_project.notification.adapter.out.persistence;

import com.ssafy.ssafy_project.notification.adapter.out.persistence.entity.NotificationJpaEntity;
import com.ssafy.ssafy_project.notification.adapter.out.persistence.repository.NotificationJpaRepository;
import com.ssafy.ssafy_project.notification.application.port.out.DeleteNotificationPortOut;
import com.ssafy.ssafy_project.notification.application.port.out.LoadNotificationPortOut;
import com.ssafy.ssafy_project.notification.application.port.out.SaveNotificationPortOut;
import com.ssafy.ssafy_project.notification.application.port.out.UpdateNotificationPortOut;
import com.ssafy.ssafy_project.notification.domain.Notification;
import com.ssafy.ssafy_project.report.adapter.out.persistence.ReportJpaEntity;
import com.ssafy.ssafy_project.report.adapter.out.persistence.ReportJpaRepository;
import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.room.adapter.out.persistence.repository.RoomJpaRepository;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationJpaAdapter implements SaveNotificationPortOut, LoadNotificationPortOut, UpdateNotificationPortOut, DeleteNotificationPortOut {

    private final UserJpaRepository userJpaRepository;
    private final RoomJpaRepository roomJpaRepository;
    private final ReportJpaRepository reportJpaRepository;
    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    public void deleteById(Long notificationId) {
        notificationJpaRepository.deleteById(notificationId);
    }

    @Override
    public List<Notification> loadAllByToId(Long toId) {
        return notificationJpaRepository.findAllByToUser_IdOrderByCreatedTimeDesc(toId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<Notification> loadUnreadByToId(Long toId) {
        return notificationJpaRepository.findAllByToUser_IdAndReadAtIsNullOrderByCreatedTimeDesc(toId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countUnreadByToId(Long toId) {
        return notificationJpaRepository.countByToUser_IdAndReadAtIsNull(toId);
    }

    @Override
    public Optional<Notification> loadByIdAndToId(Long notificationId, Long toId) {
        return notificationJpaRepository.findByIdAndToUser_Id(notificationId, toId)
                .map(this::toDomain);
    }

    @Override
    public Notification save(Notification notification) {
        UserJpaEntity toUser = userJpaRepository.getReferenceById(notification.getToId());

        UserJpaEntity fromUser = null;
        if(notification.getFromId() != null) {
            fromUser = userJpaRepository.getReferenceById(notification.getFromId());
        }

        RoomJpaEntity room = null;
        if(notification.getRoomId() != null) {
            room = roomJpaRepository.getReferenceById(notification.getRoomId());
        }

        ReportJpaEntity report = null;
        if(notification.getReportId() != null) {
            report = reportJpaRepository.getReferenceById(notification.getReportId());
        }

        NotificationJpaEntity notificationJpaEntity = notificationJpaRepository.save(
                new NotificationJpaEntity(
                    notification.getType(),
                        notification.getContent(),
                        toUser,
                        fromUser,
                        room,
                        report,
                        notification.getRoomCode()
                )
        );

        return toDomain(notificationJpaEntity);
    }

    @Override
    public void markAsRead(Long notificationId, Long toId, LocalDateTime readAt) {
        NotificationJpaEntity notificationJpaEntity = notificationJpaRepository.findByIdAndToUser_Id(notificationId, toId)
                .orElseThrow(() -> new RuntimeException("알림을 찾을 수 없습니다."));

        notificationJpaEntity.markAsRead(readAt);
    }

    @Override
    public void markAllAsRead(Long toId, LocalDateTime readAt) {
        notificationJpaRepository.markAllAsRead(toId, readAt);
    }

    public Notification toDomain(NotificationJpaEntity notificationJpaEntity) {
        return new Notification(
                notificationJpaEntity.getId(),
                notificationJpaEntity.getNotificationType(),
                notificationJpaEntity.getReadAt(),
                notificationJpaEntity.getContent(),
                notificationJpaEntity.getToUser().getId(),
                notificationJpaEntity.getFromUser() != null ? notificationJpaEntity.getFromUser().getId() : null,
                notificationJpaEntity.getCreatedTime(),
                notificationJpaEntity.getRoom() != null ? notificationJpaEntity.getRoom().getId() : null,
                notificationJpaEntity.getReport() != null ? notificationJpaEntity.getReport().getId() : null,
                notificationJpaEntity.getRoomCode()
        );
    }
}
