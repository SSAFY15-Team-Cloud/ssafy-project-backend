package com.ssafy.ssafy_project.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.ssafy_project.global.application.port.out.RefreshTokenPortOut;
import com.ssafy.ssafy_project.global.infrastructure.security.JwtTokenProvider;
import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.room.adapter.out.persistence.repository.RoomJpaRepository;
import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.repository.RoomParticipantJpaRepository;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import com.ssafy.ssafy_project.user.domain.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(classes = {
        com.ssafy.ssafy_project.SsafyProjectApplication.class,
        ControllerIntegrationTestSupport.TestInfraConfig.class
})
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
    protected InMemoryRefreshTokenPortOut refreshTokenPortOut;

    protected void clearPersistence() {
        roomParticipantJpaRepository.deleteAll();
        roomJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        refreshTokenPortOut.clear();
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

    @TestConfiguration
    static class TestInfraConfig {

        @Bean
        @ServiceConnection
        @ConditionalOnProperty(name = "test.use-testcontainers", havingValue = "true", matchIfMissing = true)
        PostgreSQLContainer<?> postgresContainer() {
            return new PostgreSQLContainer<>("postgres:16-alpine");
        }

        @Bean
        @Primary
        InMemoryRefreshTokenPortOut refreshTokenPortOut() {
            return new InMemoryRefreshTokenPortOut();
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    public static class InMemoryRefreshTokenPortOut implements RefreshTokenPortOut {
        private final Map<Long, String> storage = new ConcurrentHashMap<>();

        @Override
        public void save(Long userId, String refreshToken, long expirationMillis) {
            storage.put(userId, refreshToken);
        }

        @Override
        public Optional<String> find(Long userId) {
            return Optional.ofNullable(storage.get(userId));
        }

        @Override
        public void delete(Long userId) {
            storage.remove(userId);
        }

        public void clear() {
            storage.clear();
        }
    }
}
