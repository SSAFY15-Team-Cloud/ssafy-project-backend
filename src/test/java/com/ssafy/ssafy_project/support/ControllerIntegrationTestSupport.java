package com.ssafy.ssafy_project.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.ssafy_project.global.infrastructure.config.ProfileImageProperties;
import com.ssafy.ssafy_project.global.infrastructure.security.JwtTokenProvider;
import com.ssafy.ssafy_project.chat.adapter.out.persistence.repository.ChatMessageJpaRepository;
import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.room.adapter.out.persistence.repository.RoomJpaRepository;
import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.repository.RoomParticipantJpaRepository;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import com.ssafy.ssafy_project.user.domain.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestInfraConfig.class)
public abstract class ControllerIntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @Autowired
    protected UserJpaRepository userJpaRepository;

    @Autowired
    protected RoomJpaRepository roomJpaRepository;

    @Autowired
    protected RoomParticipantJpaRepository roomParticipantJpaRepository;

    @Autowired
    protected ChatMessageJpaRepository chatMessageJpaRepository;

    @Autowired
    protected TestInfraConfig.InMemoryRefreshTokenPortOut refreshTokenPortOut;

    @Autowired
    protected TestInfraConfig.InMemoryProfileImageStoragePortOut profileImageStoragePortOut;

    @Autowired
    protected ProfileImageProperties profileImageProperties;

    protected void clearPersistence() {
        chatMessageJpaRepository.deleteAll();
        roomParticipantJpaRepository.deleteAll();
        roomJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        refreshTokenPortOut.clear();
        profileImageStoragePortOut.clear();
    }

    protected UserJpaEntity saveUser(String email, String rawPassword, String nickname, String name) {
        return userJpaRepository.save(
                new UserJpaEntity(
                        email,
                        passwordEncoder.encode(rawPassword),
                        UserRole.USER,
                        nickname,
                        name
                )
        );
    }

    protected RoomJpaEntity saveRoom(String title, UserJpaEntity owner) {
        return roomJpaRepository.save(new RoomJpaEntity(title, owner));
    }

    protected String createAccessToken(Long userId) {
        return jwtTokenProvider.generateAccessToken(userId);
    }

    protected String extractCookieValue(String setCookieHeader, String cookieName) {
        String prefix = cookieName + "=";
        int start = setCookieHeader.indexOf(prefix);
        if (start < 0) {
            throw new IllegalArgumentException("Cookie not found: " + cookieName);
        }

        int valueStart = start + prefix.length();
        int valueEnd = setCookieHeader.indexOf(';', valueStart);
        if (valueEnd < 0) {
            valueEnd = setCookieHeader.length();
        }
        return setCookieHeader.substring(valueStart, valueEnd);
    }

}
