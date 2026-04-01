package com.ssafy.ssafy_project.chat.adapter.in.web;

import com.ssafy.ssafy_project.chat.adapter.out.persistence.entity.ChatMessageJpaEntity;
import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.support.ControllerIntegrationTestSupport;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatMessageControllerIntegrationTest extends ControllerIntegrationTestSupport {

    @BeforeEach
    void setUp() {
        clearPersistence();
    }

    @Test
    void createMessage_saves_message_for_active_participant() throws Exception {
        UserJpaEntity owner = saveUser("chat-owner@test.com", "password123!", "owner", "Owner");
        UserJpaEntity participant = saveUser("chat-participant@test.com", "password123!", "participant", "Participant");
        RoomJpaEntity room = saveRoom("Chat Room", owner);

        mockMvc.perform(post("/api/rooms/{roomCode}/join", room.getRoomCode())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(participant.getId())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/rooms/{roomId}/messages", room.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(participant.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "hello chat"
                                }
                                """))
                .andExpect(status().isCreated());

        assertThat(chatMessageJpaRepository.count()).isEqualTo(1);
        ChatMessageJpaEntity savedMessage = chatMessageJpaRepository.findAll().getFirst();

        assertThat(savedMessage.getRoomJpaEntity().getId()).isEqualTo(room.getId());
        assertThat(savedMessage.getUserJpaEntity().getId()).isEqualTo(participant.getId());
        assertThat(savedMessage.getSenderNickname()).isEqualTo(participant.getNickname());
        assertThat(savedMessage.getMessage()).isEqualTo("hello chat");
        assertThat(savedMessage.getCreatedTime()).isNotNull();
        assertThat(savedMessage.isDeleted()).isFalse();
    }

    @Test
    void createMessage_requires_authentication() throws Exception {
        mockMvc.perform(post("/api/rooms/{roomId}/messages", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "hello"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createMessage_fails_for_non_participant() {
        UserJpaEntity owner = saveUser("chat-owner2@test.com", "password123!", "owner2", "Owner2");
        UserJpaEntity outsider = saveUser("chat-outsider@test.com", "password123!", "outsider", "Outsider");
        RoomJpaEntity room = saveRoom("Chat Guard Room", owner);

        assertThatThrownBy(() -> mockMvc.perform(post("/api/rooms/{roomId}/messages", room.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(outsider.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "should fail"
                                }
                                """)))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Request processing failed");
    }

    @Test
    void createMessage_rejects_blank_message() throws Exception {
        UserJpaEntity owner = saveUser("chat-owner3@test.com", "password123!", "owner3", "Owner3");
        RoomJpaEntity room = saveRoom("Blank Chat Room", owner);

        mockMvc.perform(post("/api/rooms/{roomId}/messages", room.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(owner.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "   "
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
