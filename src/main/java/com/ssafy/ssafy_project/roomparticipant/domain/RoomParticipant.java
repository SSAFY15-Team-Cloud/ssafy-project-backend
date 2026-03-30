package com.ssafy.ssafy_project.roomparticipant.domain;

import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RoomParticipant {
    private Long id;

    private Room room;

    private User user;

    private RoomParticipantRole role;

    private LocalDateTime joinedTime;

    private Long durationTime;

    private LocalDateTime createdTime;

    private boolean isActive;

    public RoomParticipant(Room room, User user, RoomParticipantRole role){
        this.room = room;
        this.user = user;
        this.role = role;
        this.joinedTime = LocalDateTime.now();
        this.durationTime = 0L;
        this.isActive = true;
    }

    public void join(){
        this.isActive = true;
        this.joinedTime = LocalDateTime.now();
    }

    public void leave(){
        if(isActive) {
            durationTime += Duration.between(joinedTime, LocalDateTime.now()).toMillis();
            isActive = false;
        }else{
            throw new RuntimeException("이미 퇴장한 유저입니다.");
        }
    }
}
