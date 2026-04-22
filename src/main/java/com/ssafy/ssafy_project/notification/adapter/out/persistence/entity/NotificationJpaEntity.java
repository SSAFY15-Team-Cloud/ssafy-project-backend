package com.ssafy.ssafy_project.notification.adapter.out.persistence.entity;

import com.ssafy.ssafy_project.notification.domain.NotificationType;
import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name="notification")
public class NotificationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType notificationType;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "content", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_id", nullable = false)
    private UserJpaEntity toId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_id")
    private UserJpaEntity fromId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private RoomJpaEntity roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private Long reportId;

    @Column(name="room_code")
    private String roomCode;

    @Column(name = "created_time", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdTime;

    public NotificationJpaEntity(NotificationType notificationType, UserJpaEntity toId, UserJpaEntity fromId, String content, RoomJpaEntity roomId, String roomCode) {
        this.notificationType = notificationType;
        this.toId = toId;
        this.fromId = fromId;
        this.content = content;
        this.roomId = roomId;
        this.roomCode = roomCode;
    }

    public NotificationJpaEntity(NotificationType notificationType, String content, UserJpaEntity toId, Long reportId) {
        this.notificationType = notificationType;
        this.content = content;
        this.toId = toId;
        this.reportId = reportId;
    }
}
