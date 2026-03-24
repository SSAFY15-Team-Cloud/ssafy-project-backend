package com.ssafy.ssafy_project.Room.domain;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class Room {
    private Long roomId;
    private String title;
    private Long hostId;
    private String roomCode;
    private String status;
    private LocalDateTime endedTime;
    private LocalDateTime createdAt;

    public Room(Long roomId, String title, Long hostId, String roomCode, String status, LocalDateTime endedTime, LocalDateTime createdAt){
        this.roomId = roomId;
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