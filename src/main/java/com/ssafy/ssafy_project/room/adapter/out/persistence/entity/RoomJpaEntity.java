package com.ssafy.ssafy_project.room.adapter.out.persistence.entity;

import com.ssafy.ssafy_project.room.domain.RoomStatus;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name="room")
@NoArgsConstructor
public class RoomJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name ="id")
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserJpaEntity userJpaEntity;

    @Column(name = "room_code", unique = true, nullable = false)
    private String roomCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus status;

    @Column(name = "ended_time")
    private LocalDateTime endedTime;

    @Column(name = "created_time", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdTime;

    public RoomJpaEntity(String title, UserJpaEntity userJpaEntity){
        this.title = title;
        this.userJpaEntity = userJpaEntity;
        this.status = RoomStatus.RUNNING;
        this.roomCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public void endRoom() {
        this.status = RoomStatus.ENDED;
        this.endedTime = LocalDateTime.now();
    }

    public void updateTitle(String title) {
        this.title = title;
    }
}
