package com.ssafy.ssafy_project.Room.adapter.out.persistence.entity;

import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Table(name="rooms")
@NoArgsConstructor
public class RoomJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name ="room_id")
    private Long roomId;

    @Column(name = "title")
    private String title;


    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserJpaEntity userJpaEntity;

    @Column(name = "room_code", unique = true)
    private String roomCode;

    @Column(name = "status")
    private String status;

    @Column(name = "ended_time")
    private LocalDateTime endedTime;

    @Column(name = "created_time", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdTime;

    public RoomJpaEntity(String title, UserJpaEntity userJpaEntity){
        this.title = title;
        this.userJpaEntity = userJpaEntity;
        this.status = "RUNNING";
        this.roomCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public void endRoom() {
        this.status = "ENDED";
        this.endedTime = LocalDateTime.now();
    }

    public void updateTitle(String title) {
        this.title = title;
    }
}
