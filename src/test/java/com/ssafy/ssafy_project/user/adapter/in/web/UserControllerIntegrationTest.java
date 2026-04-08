package com.ssafy.ssafy_project.user.adapter.in.web;

import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.room.domain.RoomStatus;
import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.entity.RoomParticipantJpaEntity;
import com.ssafy.ssafy_project.support.ControllerIntegrationTestSupport;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerIntegrationTest extends ControllerIntegrationTestSupport {

    @BeforeEach
    void setUp() {
        clearPersistence();
    }

    @Test
    void getMyInfo_returns_authenticated_user_info() throws Exception {
        UserJpaEntity user = saveUser("me@test.com", "password123!", "me-user", "Me User");

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(user.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.email").value("me@test.com"))
                .andExpect(jsonPath("$.nickname").value("me-user"))
                .andExpect(jsonPath("$.name").value("Me User"))
                .andExpect(jsonPath("$.profileImageKey").isEmpty());
    }

    @Test
    void getMyInfo_requires_authentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUserNickname_updates_persisted_nickname() throws Exception {
        UserJpaEntity user = saveUser("nickname@test.com", "password123!", "before-name", "Nickname User");

        mockMvc.perform(patch("/api/users/me/nickname")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "after-name"
                                }
                                """))
                .andExpect(status().isNoContent());

        UserJpaEntity updatedUser = userJpaRepository.findById(user.getId()).orElseThrow();
        assertThat(updatedUser.getNickname()).isEqualTo("after-name");
    }

    @Test
    void updateUserNickname_rejects_blank_nickname() throws Exception {
        UserJpaEntity user = saveUser("invalid-nickname@test.com", "password123!", "valid-name", "Valid User");

        mockMvc.perform(patch("/api/users/me/nickname")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(user.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUserNickname_requires_authentication() throws Exception {
        mockMvc.perform(patch("/api/users/me/nickname")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "after-name"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void withdrawUser_soft_deletes_user_and_cleans_related_state() throws Exception {
        UserJpaEntity withdrawingUser = saveUser("withdraw@test.com", "password123!", "withdraw-user", "Withdraw User");
        UserJpaEntity guestOwner = saveUser("guest-owner@test.com", "password123!", "guest-owner", "Guest Owner");
        UserJpaEntity otherParticipant = saveUser("other-participant@test.com", "password123!", "other-participant", "Other Participant");

        String hostRoomResponse = mockMvc.perform(post("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(withdrawingUser.getId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Hosted Room"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long hostedRoomId = objectMapper.readTree(hostRoomResponse).get("roomId").asLong();
        RoomJpaEntity hostedRoom = roomJpaRepository.findById(hostedRoomId).orElseThrow();

        mockMvc.perform(post("/api/rooms/{roomCode}/join", hostedRoom.getRoomCode())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(otherParticipant.getId())))
                .andExpect(status().isOk());

        RoomJpaEntity joinedRoom = saveRoom("Joined Room", guestOwner);
        mockMvc.perform(post("/api/rooms/{roomCode}/join", joinedRoom.getRoomCode())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(withdrawingUser.getId())))
                .andExpect(status().isOk());

        String setCookieHeader = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "withdraw@test.com",
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getHeader(HttpHeaders.SET_COOKIE);

        String refreshToken = extractCookieValue(setCookieHeader, "refreshToken");
        assertThat(refreshTokenPortOut.find(withdrawingUser.getId())).contains(refreshToken);

        mockMvc.perform(delete("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(withdrawingUser.getId()))
                        .cookie(new Cookie("refreshToken", refreshToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("refreshToken=")))
                .andExpect(cookie().maxAge("refreshToken", 0));

        UserJpaEntity deletedUser = userJpaRepository.findById(withdrawingUser.getId()).orElseThrow();
        RoomJpaEntity endedHostedRoom = roomJpaRepository.findById(hostedRoomId).orElseThrow();
        RoomParticipantJpaEntity joinedRoomParticipant = roomParticipantJpaRepository
                .findByRoomJpaEntity_IdAndUserJpaEntity_Id(joinedRoom.getId(), withdrawingUser.getId())
                .orElseThrow();
        RoomParticipantJpaEntity hostedRoomOwnerParticipant = roomParticipantJpaRepository
                .findByRoomJpaEntity_IdAndUserJpaEntity_Id(hostedRoomId, withdrawingUser.getId())
                .orElseThrow();
        RoomParticipantJpaEntity hostedRoomOtherParticipant = roomParticipantJpaRepository
                .findByRoomJpaEntity_IdAndUserJpaEntity_Id(hostedRoomId, otherParticipant.getId())
                .orElseThrow();

        assertThat(deletedUser.isDeleted()).isTrue();
        assertThat(endedHostedRoom.getStatus()).isEqualTo(RoomStatus.ENDED);
        assertThat(endedHostedRoom.getEndedTime()).isNotNull();
        assertThat(joinedRoomParticipant.isActive()).isFalse();
        assertThat(joinedRoomParticipant.getDurationTime()).isGreaterThanOrEqualTo(0L);
        assertThat(hostedRoomOwnerParticipant.isActive()).isFalse();
        assertThat(hostedRoomOtherParticipant.isActive()).isFalse();
        assertThat(refreshTokenPortOut.find(withdrawingUser.getId())).isEmpty();
    }

    @Test
    void withdrawUser_requires_authentication() throws Exception {
        mockMvc.perform(delete("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }
}
