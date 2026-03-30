package com.ssafy.ssafy_project.roomparticipant.adapter.in.web;

import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.entity.RoomParticipantJpaEntity;
import com.ssafy.ssafy_project.support.ControllerIntegrationTestSupport;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomParticipantControllerIntegrationTest extends ControllerIntegrationTestSupport {

    @BeforeEach
    void setUp() {
        clearPersistence();
    }

    @Test
    void joinRoomParticipant_creates_new_participant_for_room() throws Exception {
        UserJpaEntity owner = saveUser("owner@test.com", "password123!", "owner", "Owner");
        UserJpaEntity participant = saveUser("participant@test.com", "password123!", "participant", "Participant");
        RoomJpaEntity room = saveRoom("Room A", owner);

        mockMvc.perform(post("/api/room-participants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(participant.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomCode": "%s"
                                }
                                """.formatted(room.getRoomCode())))
                .andExpect(status().isCreated());

        RoomParticipantJpaEntity savedParticipant = roomParticipantJpaRepository
                .findByRoomJpaEntity_IdAndUserJpaEntity_Id(room.getId(), participant.getId())
                .orElseThrow();

        assertThat(savedParticipant.getRoomJpaEntity().getId()).isEqualTo(room.getId());
        assertThat(savedParticipant.getUserJpaEntity().getId()).isEqualTo(participant.getId());
        assertThat(savedParticipant.isActive()).isTrue();
        assertThat(savedParticipant.getJoinedTime()).isNotNull();
    }

    @Test
    void joinRoomParticipant_rejoins_existing__without_creating_duplicate() throws Exception {
        UserJpaEntity owner = saveUser("owner2@test.com", "password123!", "owner2", "Owner2");
        UserJpaEntity participant = saveUser("participant2@test.com", "password123!", "participant2", "Participant2");
        RoomJpaEntity room = saveRoom("Room B", owner);

        mockMvc.perform(post("/api/room-participants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(participant.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomCode": "%s"
                                }
                                """.formatted(room.getRoomCode())))
                .andExpect(status().isCreated());

        RoomParticipantJpaEntity firstParticipant = roomParticipantJpaRepository
                .findByRoomJpaEntity_IdAndUserJpaEntity_Id(room.getId(), participant.getId())
                .orElseThrow();
        assertThat(firstParticipant.getId()).isNotNull();

        Thread.sleep(20L);

        mockMvc.perform(post("/api/room-participants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(participant.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomCode": "%s"
                                }
                                """.formatted(room.getRoomCode())))
                .andExpect(status().isCreated());

        RoomParticipantJpaEntity updatedParticipant = roomParticipantJpaRepository
                .findByRoomJpaEntity_IdAndUserJpaEntity_Id(room.getId(), participant.getId())
                .orElseThrow();

        assertThat(roomParticipantJpaRepository.count()).isEqualTo(1);
        assertThat(updatedParticipant.getId()).isEqualTo(firstParticipant.getId());
        assertThat(updatedParticipant.getJoinedTime()).isAfterOrEqualTo(firstParticipant.getJoinedTime());
    }

    @Test
    void joinRoom_requires_authentication() throws Exception {
        mockMvc.perform(post("/api/room-participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomCode": "ABCDEFGH"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void leaveRoom_deactivates_active_participant() throws Exception {
        UserJpaEntity owner = saveUser("leave-owner@test.com", "password123!", "owner", "Owner");
        UserJpaEntity participant = saveUser("leave-participant@test.com", "password123!", "participant", "Participant");
        RoomJpaEntity room = saveRoom("Leave Room", owner);

        mockMvc.perform(post("/api/room-participants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(participant.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomCode": "%s"
                                }
                                """.formatted(room.getRoomCode())))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/room-participants/rooms/{roomId}/me", room.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(participant.getId())))
                .andExpect(status().isNoContent());

        RoomParticipantJpaEntity savedParticipant = roomParticipantJpaRepository
                .findByRoomJpaEntity_IdAndUserJpaEntity_Id(room.getId(), participant.getId())
                .orElseThrow();

        assertThat(savedParticipant.isActive()).isFalse();
        assertThat(savedParticipant.getDurationTime()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void leaveRoom_requires_authentication() throws Exception {
        mockMvc.perform(delete("/api/room-participants/rooms/{roomId}/me", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void leaveRoom_fails_for_non_participant() throws Exception {
        UserJpaEntity owner = saveUser("non-participant-owner@test.com", "password123!", "owner", "Owner");
        UserJpaEntity outsider = saveUser("outsider@test.com", "password123!", "outsider", "Outsider");
        RoomJpaEntity room = saveRoom("Non Participant Room", owner);

        assertThatThrownBy(() -> mockMvc.perform(delete("/api/room-participants/rooms/{roomId}/me", room.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(outsider.getId()))))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("Request processing failed");
    }

    @Test
    void leaveRoom_ends_room_when_host_leaves() throws Exception {
        UserJpaEntity owner = saveUser("host-leave-owner@test.com", "password123!", "owner", "Owner");
        UserJpaEntity participant = saveUser("host-leave-participant@test.com", "password123!", "participant", "Participant");

        String responseBody = mockMvc.perform(post("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(owner.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Host Leave Room"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long roomId = objectMapper.readTree(responseBody).get("roomId").asLong();
        RoomJpaEntity room = roomJpaRepository.findById(roomId).orElseThrow();

        mockMvc.perform(post("/api/room-participants")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(participant.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomCode": "%s"
                                }
                                """.formatted(room.getRoomCode())))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/room-participants/rooms/{roomId}/me", roomId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(owner.getId())))
                .andExpect(status().isNoContent());

        RoomJpaEntity endedRoom = roomJpaRepository.findById(roomId).orElseThrow();
        RoomParticipantJpaEntity ownerParticipant = roomParticipantJpaRepository
                .findByRoomJpaEntity_IdAndUserJpaEntity_Id(roomId, owner.getId())
                .orElseThrow();
        RoomParticipantJpaEntity joinedParticipant = roomParticipantJpaRepository
                .findByRoomJpaEntity_IdAndUserJpaEntity_Id(roomId, participant.getId())
                .orElseThrow();

        assertThat(endedRoom.getStatus()).isEqualTo("ENDED");
        assertThat(endedRoom.getEndedTime()).isNotNull();
        assertThat(ownerParticipant.isActive()).isFalse();
        assertThat(joinedParticipant.isActive()).isFalse();
    }
}
