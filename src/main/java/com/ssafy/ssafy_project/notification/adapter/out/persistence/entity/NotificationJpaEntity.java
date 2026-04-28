package com.ssafy.ssafy_project.notification.adapter.out.persistence.entity;

import com.ssafy.ssafy_project.notification.domain.NotificationType;
import com.ssafy.ssafy_project.report.adapter.out.persistence.ReportJpaEntity;
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
    private UserJpaEntity toUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_id")
    private UserJpaEntity fromUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private RoomJpaEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id")
    private ReportJpaEntity report;

    @Column(name="room_code")
    private String roomCode;

    @Column(name = "created_time", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdTime;

    public NotificationJpaEntity(NotificationType notificationType, String content, UserJpaEntity toUser, UserJpaEntity fromUser, RoomJpaEntity room, ReportJpaEntity report, String roomCode) {
        this.notificationType = notificationType;
        this.content = content;
        this.toUser = toUser;
        this.fromUser = fromUser;
        this.room = room;
        this.report = report;
        this.roomCode = roomCode;
    }

    public void markAsRead(LocalDateTime readAt) {
        if (this.readAt == null) {
            this.readAt = readAt;
        }
    }
}
