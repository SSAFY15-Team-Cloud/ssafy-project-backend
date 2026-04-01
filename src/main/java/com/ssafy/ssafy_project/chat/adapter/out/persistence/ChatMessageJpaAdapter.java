package com.ssafy.ssafy_project.chat.adapter.out.persistence;

import com.ssafy.ssafy_project.chat.adapter.out.persistence.entity.ChatMessageJpaEntity;
import com.ssafy.ssafy_project.chat.adapter.out.persistence.repository.ChatMessageJpaRepository;
import com.ssafy.ssafy_project.chat.application.port.out.CreateMessagePortOut;
import com.ssafy.ssafy_project.chat.domain.ChatMessage;
import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.room.adapter.out.persistence.repository.RoomJpaRepository;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatMessageJpaAdapter implements CreateMessagePortOut {
    private final ChatMessageJpaRepository chatMessageJpaRepository;
    private final RoomJpaRepository roomJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public void createMessage(ChatMessage chatMessage) {
        Room room = chatMessage.getRoom();
        User user = chatMessage.getUser();
        String senderNickname = chatMessage.getSenderNickname();
        String message = chatMessage.getMessage();

        RoomJpaEntity roomJpaEntity = roomJpaRepository.getReferenceById(room.getId());

        UserJpaEntity userJpaEntity = userJpaRepository.getReferenceById(user.getId());

        ChatMessageJpaEntity chatMessageJpaEntity = new ChatMessageJpaEntity(
                senderNickname, message, roomJpaEntity, userJpaEntity);

        chatMessageJpaRepository.save(chatMessageJpaEntity);
    }

}
