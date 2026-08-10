package com.ssafy.ssafy_project.chat.adapter.out.persistence;

import com.ssafy.ssafy_project.chat.adapter.out.persistence.entity.ChatMessageJpaEntity;
import com.ssafy.ssafy_project.global.exception.CommonErrorCode;
import com.ssafy.ssafy_project.global.exception.CustomException;
import com.ssafy.ssafy_project.chat.adapter.out.persistence.repository.ChatMessageJpaRepository;
import com.ssafy.ssafy_project.chat.application.port.out.CreateMessagePortOut;
import com.ssafy.ssafy_project.chat.application.port.out.DeleteMessagePortOut;
import com.ssafy.ssafy_project.chat.application.port.out.GetMessagesPortOut;
import com.ssafy.ssafy_project.chat.application.port.out.LoadMessagePortOut;
import com.ssafy.ssafy_project.chat.domain.ChatMessage;
import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.room.adapter.out.persistence.repository.RoomJpaRepository;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import com.ssafy.ssafy_project.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ChatMessageJpaAdapter implements CreateMessagePortOut, LoadMessagePortOut,
        DeleteMessagePortOut, GetMessagesPortOut {
    private final ChatMessageJpaRepository chatMessageJpaRepository;
    private final RoomJpaRepository roomJpaRepository;
    private final UserJpaRepository userJpaRepository;

    @Override
    public ChatMessage createMessage(ChatMessage chatMessage) {
        Room room = chatMessage.getRoom();
        User user = chatMessage.getUser();
        String senderNickname = chatMessage.getSenderNickname();
        String message = chatMessage.getMessage();

        RoomJpaEntity roomJpaEntity = roomJpaRepository.getReferenceById(room.getId());

        UserJpaEntity userJpaEntity = userJpaRepository.getReferenceById(user.getId());

        ChatMessageJpaEntity chatMessageJpaEntity = new ChatMessageJpaEntity(
                senderNickname, message, roomJpaEntity, userJpaEntity);

        ChatMessageJpaEntity savedChatMessage = chatMessageJpaRepository.save(chatMessageJpaEntity);
        return toDomain(savedChatMessage);
    }

    @Override
    public ChatMessage findByMessageId(Long messageId) {
        ChatMessageJpaEntity chatMessageJpaEntity = chatMessageJpaRepository.findById(messageId)
                .orElseThrow(()-> new CustomException(CommonErrorCode.MESSAGE_NOT_FOUND));

        return toDomain(chatMessageJpaEntity);
    }

    @Override
    public void deleteMessage(ChatMessage chatMessage) {
        ChatMessageJpaEntity chatMessageJpaEntity = toEntity(chatMessage);
        chatMessageJpaRepository.save(chatMessageJpaEntity);
    }

    @Override
    public List<ChatMessage> getMessages(Long roomId) {
        return chatMessageJpaRepository.findAllByRoomJpaEntity_IdAndIsDeletedFalseOrderByCreatedTimeAscIdAsc(roomId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private ChatMessageJpaEntity toEntity(ChatMessage chatMessage){
        RoomJpaEntity roomJpaEntity = roomJpaRepository.findById(chatMessage.getRoom().getId())
                .orElseThrow(()-> new CustomException(CommonErrorCode.ROOM_NOT_FOUND));

        UserJpaEntity userJpaEntity = userJpaRepository.findById(chatMessage.getUser().getId())
                .orElseThrow(()-> new CustomException(CommonErrorCode.USER_NOT_FOUND));

        return new ChatMessageJpaEntity(
                chatMessage.getId(),
                roomJpaEntity,
                userJpaEntity,
                chatMessage.getSenderNickname(),
                chatMessage.getMessage(),
                chatMessage.getCreatedTime(),
                chatMessage.isDeleted()
                );
    }

    private ChatMessage toDomain(ChatMessageJpaEntity chatMessageJpaEntity){
        Room room = toDomain(chatMessageJpaEntity.getRoomJpaEntity());
        User user = toDomain(chatMessageJpaEntity.getUserJpaEntity());
        return new ChatMessage(
                chatMessageJpaEntity.getId(),
                room,
                user,
                chatMessageJpaEntity.getSenderNickname(),
                chatMessageJpaEntity.getMessage(),
                chatMessageJpaEntity.getCreatedTime(),
                chatMessageJpaEntity.isDeleted()
                );
    }

    private Room toDomain(RoomJpaEntity roomJpaEntity) {
        return new Room(
                roomJpaEntity.getId(),
                roomJpaEntity.getTitle(),
                roomJpaEntity.getUserJpaEntity().getId(),
                roomJpaEntity.getRoomCode(),
                roomJpaEntity.getStatus(),
                roomJpaEntity.getEndedTime(),
                roomJpaEntity.getCreatedTime()
        );
    }

    private User toDomain(UserJpaEntity userJpaEntity) {
        return new User(
                userJpaEntity.getId(),
                userJpaEntity.getEmail(),
                userJpaEntity.getPassword(),
                userJpaEntity.getRole(),
                userJpaEntity.getNickname(),
                userJpaEntity.getName(),
                userJpaEntity.getProfileImageKey(),
                userJpaEntity.isDeleted()
        );
    }
}
