package com.ssafy.ssafy_project.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.ssafy_project.global.application.port.out.RefreshTokenPortOut;
import com.ssafy.ssafy_project.user.application.port.out.ProfileImageStoragePortOut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@TestConfiguration(proxyBeanMethods = false)
public class TestInfraConfig {

    @Bean
    @ServiceConnection
    @ConditionalOnProperty(name = "test.use-testcontainers", havingValue = "true", matchIfMissing = true)
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }

    @Bean
    @Primary
    RefreshTokenPortOut refreshTokenPortOut() {
        return new InMemoryRefreshTokenPortOut();
    }

    @Bean
    @Primary
    ProfileImageStoragePortOut profileImageStoragePortOut() {
        return new InMemoryProfileImageStoragePortOut();
    }

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
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

    public static class InMemoryProfileImageStoragePortOut implements ProfileImageStoragePortOut {
        private final List<String> deletedKeys = new CopyOnWriteArrayList<>();

        @Override
        public String generateUploadUrl(String objectKey) {
            return "https://test-bucket.s3.amazonaws.com/" + objectKey;
        }

        @Override
        public void delete(String objectKey) {
            deletedKeys.add(objectKey);
        }

        public List<String> deletedKeys() {
            return deletedKeys;
        }

        public void clear() {
            deletedKeys.clear();
        }
    }
}
