package com.ssafy.ssafy_project.room.adapter.in.web;

import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.support.ControllerIntegrationTestSupport;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomControllerIntegrationTest extends ControllerIntegrationTestSupport {

    @BeforeEach
    void setUp() {
        clearPersistence();
    }

    @Test
    void createRoom_creates_room_for_authenticated_user() throws Exception {
        UserJpaEntity owner = saveUser("owner@test.com", "password123!", "owner", "Owner");

        String responseBody = mockMvc.perform(post("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(owner.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Morning Study"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Morning Study"))
                .andExpect(jsonPath("$.hostId").value(owner.getId()))
                .andExpect(jsonPath("$.roomCode").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long roomId = objectMapper.readTree(responseBody).get("roomId").asLong();
        RoomJpaEntity savedRoom = roomJpaRepository.findById(roomId).orElseThrow();

        assertThat(savedRoom.getTitle()).isEqualTo("Morning Study");
        assertThat(savedRoom.getUserJpaEntity().getId()).isEqualTo(owner.getId());
        assertThat(savedRoom.getStatus()).isEqualTo("RUNNING");
    }

    @Test
    void updateRoom_updates_title_for_owner() throws Exception {
        UserJpaEntity owner = saveUser("update-owner@test.com", "password123!", "owner", "Owner");
        RoomJpaEntity room = saveRoom("Old Title", owner);

        mockMvc.perform(put("/api/rooms/{roomId}", room.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(owner.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "New Title"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(room.getId()))
                .andExpect(jsonPath("$.title").value("New Title"));

        RoomJpaEntity updatedRoom = roomJpaRepository.findById(room.getId()).orElseThrow();
        assertThat(updatedRoom.getTitle()).isEqualTo("New Title");
    }

    @Test
    void deleteRoom_marks_room_as_ended_for_owner() throws Exception {
        UserJpaEntity owner = saveUser("delete-owner@test.com", "password123!", "owner", "Owner");
        RoomJpaEntity room = saveRoom("Delete Me", owner);

        mockMvc.perform(delete("/api/rooms/{roomId}", room.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(owner.getId())))
                .andExpect(status().isNoContent());

        RoomJpaEntity deletedRoom = roomJpaRepository.findById(room.getId()).orElseThrow();
        assertThat(deletedRoom.getStatus()).isEqualTo("ENDED");
        assertThat(deletedRoom.getEndedTime()).isNotNull();
    }

    @Test
    void createRoom_requires_authentication() throws Exception {
        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "No Auth"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
