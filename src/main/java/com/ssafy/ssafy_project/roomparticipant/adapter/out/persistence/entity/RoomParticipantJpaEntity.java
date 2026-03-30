package com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.entity;

import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipant;
import com.ssafy.ssafy_project.roomparticipant.domain.RoomParticipantRole;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "room_participants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_room_participant_room_user", columnNames={"room_id", "user_id"})
    }
)
public class RoomParticipantJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_participant_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomJpaEntity roomJpaEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity userJpaEntity;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private RoomParticipantRole role;

    @Column(name = "joined_time", nullable = false)
    private LocalDateTime joinedTime;

    @Column(name = "duration_time", nullable = false)
    private Long durationTime = 0L;

    @Column(name = "created_time", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdTime;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public RoomParticipantJpaEntity(RoomJpaEntity roomJpaEntity, UserJpaEntity userJpaEntity){
        this.roomJpaEntity = roomJpaEntity;
        this.userJpaEntity = userJpaEntity;
    }

    public void updateFrom(RoomParticipant roomParticipant) {
        this.role = roomParticipant.getRole();
        this.joinedTime = roomParticipant.getJoinedTime();
        this.durationTime = roomParticipant.getDurationTime();
        this.isActive = roomParticipant.isActive();
    }
}
