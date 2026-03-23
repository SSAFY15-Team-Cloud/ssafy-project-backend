package com.ssafy.ssafy_project.Room.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
public class Room {
    private String title;
    private Long userId;

    public Room(String title, Long userId){
        this.title = title;
        this.userId = userId;
    }
}