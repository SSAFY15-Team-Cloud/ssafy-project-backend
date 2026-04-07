package com.ssafy.ssafy_project.chat.domain;


import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatMessage {
    private Long id;

    private Room room;

    private User user;

    private String senderNickname;

    private String message;

    private LocalDateTime createdTime;

    private boolean isDeleted = false;

    public void deleteMessage(){
        this.isDeleted = true;
    }

    public ChatMessage(Room room, User user, String senderNickname, String message){
        this.room = room;
        this.user = user;
        this.senderNickname = senderNickname;
        this.message = message;
    }
}
