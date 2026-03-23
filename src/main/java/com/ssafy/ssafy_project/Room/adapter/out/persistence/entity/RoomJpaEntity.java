package com.ssafy.ssafy_project.Room.adapter.out.persistence.entity;

import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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
    @JoinColumn(name = "host_id")
    private UserJpaEntity userJpaEntity;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public RoomJpaEntity(String title, UserJpaEntity userJpaEntity){
        this.title = title;
        this.userJpaEntity = userJpaEntity;
    }

}
