package com.ssafy.ssafy_project.chat.adapter.in.web;

import com.ssafy.ssafy_project.chat.adapter.in.web.dto.request.CreateMessageRequest;
import com.ssafy.ssafy_project.chat.application.port.in.CreateMessageCommand;
import com.ssafy.ssafy_project.chat.application.port.in.CreateMessagePortIn;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatMessageController {
    private final CreateMessagePortIn createMessagePortIn;

    @PostMapping("/api/rooms/{roomId}/messages")
    public ResponseEntity<Void> createMessage(
            @PathVariable Long roomId,
            @Valid @RequestBody CreateMessageRequest createMessageRequest,
            @AuthenticationPrincipal Long userId
    ){
        CreateMessageCommand createMessageCommand = new CreateMessageCommand(roomId, createMessageRequest.message(), userId);
        createMessagePortIn.createMessage(createMessageCommand);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
