package com.ssafy.ssafy_project.chat.adapter.in.web;

import com.ssafy.ssafy_project.chat.adapter.in.web.dto.request.CreateMessageRequest;
import com.ssafy.ssafy_project.chat.adapter.in.web.dto.response.GetMessagesResponse;
import com.ssafy.ssafy_project.chat.adapter.in.web.dto.response.MessageDetailResponse;
import com.ssafy.ssafy_project.chat.application.port.in.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class ChatMessageController {
    private final DeleteMessagePortIn deleteMessagePortIn;
    private final GetMessagesPortIn getMessagesPortIn;

    @DeleteMapping("/api/messages/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal Long userId
    ){
        DeleteMessageCommand deleteMessageCommand = new DeleteMessageCommand(messageId, userId);
        deleteMessagePortIn.deleteMessage(deleteMessageCommand);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/api/room/{roomId}/messages")
    public ResponseEntity<GetMessagesResponse> getMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal Long userId
    ){
        GetMessagesCommand getMessagesCommand = new GetMessagesCommand(roomId, userId);
        GetMessagesResult getMessagesResult = getMessagesPortIn.getMessages(getMessagesCommand);

        GetMessagesResponse getMessagesResponse = new GetMessagesResponse(
                getMessagesResult.messages()
                        .stream()
                        .map(gmr -> new MessageDetailResponse(
                                gmr.messageId(),
                                gmr.senderId(),
                                gmr.senderNickname(),
                                gmr.message(),
                                gmr.createdTime()
                        ))
                        .toList()
        );

        return ResponseEntity.ok(getMessagesResponse);
    }
}
