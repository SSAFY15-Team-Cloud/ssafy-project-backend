package com.ssafy.ssafy_project.notification.adapter.out.persistence;

import com.ssafy.ssafy_project.notification.adapter.out.persistence.entity.NotificationJpaEntity;
import com.ssafy.ssafy_project.notification.adapter.out.persistence.repository.NotificationJpaRepository;
import com.ssafy.ssafy_project.notification.application.port.out.DeleteNotificationPortOut;
import com.ssafy.ssafy_project.notification.application.port.out.LoadNotificationPortOut;
import com.ssafy.ssafy_project.notification.application.port.out.SaveNotificationPortOut;
import com.ssafy.ssafy_project.notification.application.port.out.UpdateNotificationPortOut;
import com.ssafy.ssafy_project.notification.domain.Notification;
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
    private final NotificationJpaRepository notificationJpaRepository;

    @Override
    public void delete(Notification notification) {

    }

    @Override
    public List<Notification> loadAllByToId(Long toId) {
        return List.of();
    }

    @Override
    public List<Notification> loadUnreadByToId(Long toId) {
        return List.of();
    }

    @Override
    public long countUnreadByToId(Long toId) {
        return 0;
    }

    @Override
    public Optional<Notification> loadByIdAndToId(Long notificationId, Long toId) {
        return Optional.empty();
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

        NotificationJpaEntity notificationJpaEntity = notificationJpaRepository.save(
                new NotificationJpaEntity(
                    notification.getType(),
                        notification.getContent(),
                        toUser,
                        fromUser,
                        room,
                        notification.getReportId(),
                        notification.getRoomCode()
                )
        );

        return toDomain(notificationJpaEntity);
    }

    @Override
    public void markAsRead(Long notificationId, Long toId, LocalDateTime readAt) {

    }

    @Override
    public void markAllAsRead(Long toId, LocalDateTime readAt) {

    }

    public Notification toDomain(NotificationJpaEntity notificationJpaEntity) {
        return new Notification(
                notificationJpaEntity.getId(),
                notificationJpaEntity.getNotificationType(),
                notificationJpaEntity.getReadAt(),
                notificationJpaEntity.getContent(),
                notificationJpaEntity.getToUser().getId(),
                notificationJpaEntity.getFromUser().getId(),
                notificationJpaEntity.getCreatedTime(),
                notificationJpaEntity.getRoom().getId(),
                notificationJpaEntity.getReportId(),
                notificationJpaEntity.getRoomCode()
        );
    }
}
