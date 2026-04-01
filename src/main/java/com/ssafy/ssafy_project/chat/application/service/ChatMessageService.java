package com.ssafy.ssafy_project.chat.application.service;

import com.ssafy.ssafy_project.chat.application.port.in.CreateMessageCommand;
import com.ssafy.ssafy_project.chat.application.port.in.CreateMessagePortIn;
import com.ssafy.ssafy_project.chat.application.port.out.CreateMessagePortOut;
import com.ssafy.ssafy_project.chat.domain.ChatMessage;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService implements CreateMessagePortIn {
    private final CreateMessagePortOut createMessagePortOut;
    private final LoadRoomPortOut loadRoomPortOut;
    private final LoadUserPortOut loadUserPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;

    @Transactional
    @Override
    public void createMessage(CreateMessageCommand createMessageCommand) {
        Long roomId = createMessageCommand.roomId();
        String message = createMessageCommand.message();
        Long userId = createMessageCommand.userId();

        Room room = loadRoomPortOut.loadById(roomId);
        User user = loadUserPortOut.loadById(userId);
        boolean isActiveParticipant = findRoomParticipantPortOut.existsByRoom_IdAndUser_IdAndIsActiveTrue(roomId, userId);
        if(!isActiveParticipant){
            throw new RuntimeException("방의 참가자만 채팅할 수 있습니다.");
        }
        ChatMessage chatMessage = new ChatMessage(room, user, user.getNickname(), message);

        createMessagePortOut.createMessage(chatMessage);
    }
}
