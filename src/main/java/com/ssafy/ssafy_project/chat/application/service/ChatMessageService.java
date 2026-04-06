package com.ssafy.ssafy_project.chat.application.service;

import com.ssafy.ssafy_project.chat.application.port.in.*;
import com.ssafy.ssafy_project.chat.application.port.out.ChatMessageDeletedData;
import com.ssafy.ssafy_project.chat.application.port.out.ChatMessagePublishedData;
import com.ssafy.ssafy_project.chat.application.port.out.CreateMessagePortOut;
import com.ssafy.ssafy_project.chat.application.port.out.DeleteMessagePortOut;
import com.ssafy.ssafy_project.chat.application.port.out.GetMessagesPortOut;
import com.ssafy.ssafy_project.chat.application.port.out.LoadMessagePortOut;
import com.ssafy.ssafy_project.chat.application.port.out.PublishDeletedChatMessagePortOut;
import com.ssafy.ssafy_project.chat.application.port.out.PublishChatMessagePortOut;
import com.ssafy.ssafy_project.chat.domain.ChatMessage;
import com.ssafy.ssafy_project.room.application.port.out.LoadRoomPortOut;
import com.ssafy.ssafy_project.room.domain.Room;
import com.ssafy.ssafy_project.roomparticipant.application.port.out.FindRoomParticipantPortOut;
import com.ssafy.ssafy_project.user.application.port.out.LoadUserPortOut;
import com.ssafy.ssafy_project.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService implements CreateMessagePortIn, DeleteMessagePortIn,
        GetMessagesPortIn {
    private final CreateMessagePortOut createMessagePortOut;
    private final LoadRoomPortOut loadRoomPortOut;
    private final LoadUserPortOut loadUserPortOut;
    private final FindRoomParticipantPortOut findRoomParticipantPortOut;
    private final DeleteMessagePortOut deleteMessagePortOut;
    private final LoadMessagePortOut loadMessagePortOut;
    private final GetMessagesPortOut getMessagesPortOut;
    private final PublishChatMessagePortOut publishChatMessagePortOut;
    private final PublishDeletedChatMessagePortOut publishDeletedChatMessagePortOut;

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

        ChatMessage savedChatMessage = createMessagePortOut.createMessage(chatMessage);
        publishChatMessagePortOut.publish(
                new ChatMessagePublishedData(
                        roomId,
                        savedChatMessage.getId(),
                        savedChatMessage.getUser().getId(),
                        savedChatMessage.getSenderNickname(),
                        savedChatMessage.getMessage(),
                        savedChatMessage.getCreatedTime()
                )
        );
    }

    @Transactional
    @Override
    public void deleteMessage(DeleteMessageCommand deleteMessageCommand) {
        Long messageId = deleteMessageCommand.messageId();
        Long loginUserId = deleteMessageCommand.userId();
        ChatMessage targetMessage = loadMessagePortOut.findByMessageId(messageId);
        if(!targetMessage.getUser().getId().equals(loginUserId)){
            throw new RuntimeException("작성자만 삭제할 수 있습니다.");
        }
        targetMessage.deleteMessage();
        deleteMessagePortOut.deleteMessage(targetMessage);
        publishDeletedChatMessagePortOut.publish(
                new ChatMessageDeletedData(
                        targetMessage.getRoom().getId(),
                        targetMessage.getId()
                )
        );
    }

    @Override
    public GetMessagesResult getMessages(GetMessagesCommand getMessagesCommand) {
        Long roomId = getMessagesCommand.roomId();
        Long userId = getMessagesCommand.userId();

        boolean isActiveParticipant = findRoomParticipantPortOut.existsByRoom_IdAndUser_IdAndIsActiveTrue(roomId, userId);

        if(!isActiveParticipant){
            throw new RuntimeException("방의 참가자만 조회가 가능합니다.");
        }

        List<ChatMessage> chatMessages = getMessagesPortOut.getMessages(roomId);

        return new GetMessagesResult(
                chatMessages
                        .stream()
                        .map(cm-> new MessageDetailResult(
                                cm.getId(),
                                cm.getUser().getId(),
                                cm.getSenderNickname(),
                                cm.getMessage(),
                                cm.getCreatedTime()
                        ))
                        .toList()
        );

    }
}
