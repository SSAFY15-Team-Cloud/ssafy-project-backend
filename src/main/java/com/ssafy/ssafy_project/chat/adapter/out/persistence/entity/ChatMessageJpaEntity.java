package com.ssafy.ssafy_project.chat.adapter.out.persistence.entity;

import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "chat_messages")
public class ChatMessageJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomJpaEntity roomJpaEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserJpaEntity userJpaEntity;

    @Column(name = "sender_nickname", nullable = false)
    private String senderNickname;


    @Column(name = "message", nullable = false)
    private String message;

    @CreationTimestamp
    @Column(name="created_time", nullable = false)
    private LocalDateTime createdTime;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    public ChatMessageJpaEntity(String senderNickname, String message, RoomJpaEntity roomJpaEntity, UserJpaEntity userJpaEntity){
        this.senderNickname = senderNickname;
        this.message = message;
        this.roomJpaEntity = roomJpaEntity;
        this.userJpaEntity = userJpaEntity;
    }
}

