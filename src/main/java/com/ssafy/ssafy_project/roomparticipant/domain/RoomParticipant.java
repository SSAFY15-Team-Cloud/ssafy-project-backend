package com.ssafy.ssafy_project.roomparticipant.domain;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RoomParticipant {
    private Long roomParticipantId;

    private Long roomId;

    private Long userId;

    private String role;

    private LocalDateTime joinedTime;

    private Long durationTime = 0L;

    private LocalDateTime createdTime;

    private boolean isActive = true;

    public RoomParticipant(Long roomId, Long userId){
        this.roomId = roomId;
        this.userId = userId;
    }
}
