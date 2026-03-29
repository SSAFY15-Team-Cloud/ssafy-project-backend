package com.ssafy.ssafy_project.room.domain;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class Room {
    private Long id;
    private String title;
    private Long hostId;
    private String roomCode;
    private String status;
    private LocalDateTime endedTime;
    private LocalDateTime createdAt;

    public Room(Long id, String title, Long hostId, String roomCode, String status, LocalDateTime endedTime, LocalDateTime createdAt){
        this.id = id;
        this.title = title;
        this.hostId = hostId;
        this.roomCode = roomCode;
        this.status = status;
        this.endedTime = endedTime;
        this.createdAt = createdAt;
    }

    public Room(String title, Long hostId){
        this.title = title;
        this.hostId = hostId;
    }

    public void updateTitle(String title) {
        this.title = title;
    }
}