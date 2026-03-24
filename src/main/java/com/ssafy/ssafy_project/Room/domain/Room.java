package com.ssafy.ssafy_project.Room.domain;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class Room {
    private Long roomId;
    private String title;
    private Long hostId;
    private LocalDateTime createdAt;

    public Room(Long roomId, String title, Long hostId, LocalDateTime createdAt){
        this.roomId = roomId;
        this.title = title;
        this.hostId = hostId;
        this.createdAt = createdAt;
    }

    public Room(String title, Long hostId){
        this.title = title;
        this.hostId = hostId;
    }
}