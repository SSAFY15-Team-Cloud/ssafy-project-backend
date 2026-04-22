package com.ssafy.ssafy_project.chat.adapter.in.web;

import com.ssafy.ssafy_project.chat.adapter.out.persistence.entity.ChatMessageJpaEntity;
import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.support.ControllerIntegrationTestSupport;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ChatMessageControllerIntegrationTest extends ControllerIntegrationTestSupport {

    @BeforeEach
    void setUp() {
        clearPersistence();
    }

    @Test
    void getMessages_returns_messages_for_active_participant() throws Exception {
        UserJpaEntity owner = saveUser("chat-owner@test.com", "password123!", "owner", "Owner");
        UserJpaEntity participant = saveUser("chat-participant@test.com", "password123!", "participant", "Participant");
        RoomJpaEntity room = saveRoom("Chat Room", owner);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/rooms/{roomCode}/join", room.getRoomCode())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(participant.getId())))
                .andExpect(status().isOk());

        chatMessageJpaRepository.save(new ChatMessageJpaEntity("owner", "first message", room, owner));
        chatMessageJpaRepository.save(new ChatMessageJpaEntity("participant", "second message", room, participant));

        mockMvc.perform(get("/api/room/{roomId}/messages", room.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(participant.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].senderId").value(owner.getId()))
                .andExpect(jsonPath("$.messages[0].senderNickname").value("owner"))
                .andExpect(jsonPath("$.messages[0].message").value("first message"))
                .andExpect(jsonPath("$.messages[0].createdTime").isNotEmpty())
                .andExpect(jsonPath("$.messages[1].senderId").value(participant.getId()))
                .andExpect(jsonPath("$.messages[1].senderNickname").value("participant"))
                .andExpect(jsonPath("$.messages[1].message").value("second message"))
                .andExpect(jsonPath("$.messages[1].createdTime").isNotEmpty());
    }

    @Test
    void getMessages_requires_authentication() throws Exception {
        mockMvc.perform(get("/api/room/{roomId}/messages", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMessages_fails_for_non_participant() {
        UserJpaEntity owner = saveUser("chat-owner2@test.com", "password123!", "owner2", "Owner2");
        UserJpaEntity outsider = saveUser("chat-outsider@test.com", "password123!", "outsider", "Outsider");
        RoomJpaEntity room = saveRoom("Chat Guard Room", owner);

        assertThatThrownBy(() -> mockMvc.perform(get("/api/room/{roomId}/messages", room.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(outsider.getId()))))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Request processing failed");
    }

    @Test
    void deleteMessage_marks_message_as_deleted_for_author() throws Exception {
        UserJpaEntity owner = saveUser("chat-owner4@test.com", "password123!", "owner4", "Owner4");
        RoomJpaEntity room = saveRoom("Delete Chat Room", owner);
        ChatMessageJpaEntity savedMessage = chatMessageJpaRepository.save(
                new ChatMessageJpaEntity("owner4", "delete me", room, owner)
        );

        mockMvc.perform(delete("/api/messages/{messageId}", savedMessage.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(owner.getId())))
                .andExpect(status().isNoContent());

        ChatMessageJpaEntity deletedMessage = chatMessageJpaRepository.findById(savedMessage.getId()).orElseThrow();
        assertThat(deletedMessage.isDeleted()).isTrue();
        assertThat(deletedMessage.getMessage()).isEqualTo("delete me");
    }

    @Test
    void deleteMessage_requires_authentication() throws Exception {
        mockMvc.perform(delete("/api/messages/{messageId}", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteMessage_fails_for_non_author() {
        UserJpaEntity owner = saveUser("chat-owner5@test.com", "password123!", "owner5", "Owner5");
        UserJpaEntity otherUser = saveUser("chat-other@test.com", "password123!", "other", "Other");
        RoomJpaEntity room = saveRoom("Delete Guard Room", owner);
        ChatMessageJpaEntity savedMessage = chatMessageJpaRepository.save(
                new ChatMessageJpaEntity("owner5", "protected", room, owner)
        );

        assertThatThrownBy(() -> mockMvc.perform(delete("/api/messages/{messageId}", savedMessage.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(otherUser.getId()))))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Request processing failed");
    }
}
