package com.ssafy.ssafy_project.chat.adapter.in.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.ssafy_project.chat.adapter.in.websocket.dto.request.CreateMessageSocketRequest;
import com.ssafy.ssafy_project.chat.adapter.out.messaging.dto.response.ChatMessageDeletedPayload;
import com.ssafy.ssafy_project.chat.adapter.out.persistence.entity.ChatMessageJpaEntity;
import com.ssafy.ssafy_project.chat.adapter.out.persistence.repository.ChatMessageJpaRepository;
import com.ssafy.ssafy_project.chat.application.port.out.PublishDeletedChatMessagePortOut;
import com.ssafy.ssafy_project.global.infrastructure.security.JwtTokenProvider;
import com.ssafy.ssafy_project.room.adapter.out.persistence.entity.RoomJpaEntity;
import com.ssafy.ssafy_project.room.adapter.out.persistence.repository.RoomJpaRepository;
import com.ssafy.ssafy_project.roomparticipant.adapter.out.persistence.repository.RoomParticipantJpaRepository;
import com.ssafy.ssafy_project.support.TestInfraConfig;
import com.ssafy.ssafy_project.user.adapter.out.persistence.entity.UserJpaEntity;
import com.ssafy.ssafy_project.user.adapter.out.persistence.repository.UserJpaRepository;
import com.ssafy.ssafy_project.user.domain.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestInfraConfig.class)
class ChatRealtimeIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private RoomParticipantJpaRepository roomParticipantJpaRepository;

    @Autowired
    private ChatMessageJpaRepository chatMessageJpaRepository;

    @Autowired
    private TestInfraConfig.InMemoryRefreshTokenPortOut refreshTokenPortOut;

    @MockitoSpyBean
    private PublishDeletedChatMessagePortOut publishDeletedChatMessagePortOut;

    private final List<WebSocketStompClient> stompClients = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clearPersistence();
    }

    @AfterEach
    void tearDown() {
        for (WebSocketStompClient stompClient : stompClients) {
            stompClient.stop();
        }
        stompClients.clear();
    }

    @Test
    void sendMessage_through_websocket_persists_message_for_active_participant() throws Exception {
        UserJpaEntity owner = saveUser("realtime-owner@test.com", "password123!", "rt-owner", "Realtime Owner");
        UserJpaEntity participant = saveUser("realtime-participant@test.com", "password123!", "rt-participant", "Realtime Participant");

        Long roomId = createRoom(owner.getId(), "Realtime Chat Room");
        RoomJpaEntity room = roomJpaRepository.findById(roomId).orElseThrow();

        joinRoom(room.getRoomCode(), participant.getId());
        assertThat(roomParticipantJpaRepository.existsByRoomJpaEntity_IdAndUserJpaEntity_IdAndIsActiveTrue(roomId, participant.getId()))
                .isTrue();

        TestStompSessionHandler participantHandler = new TestStompSessionHandler();
        StompSession participantSession = connect(createAccessToken(participant.getId()), participantHandler);

        participantSession.send(
                "/pub/rooms/" + roomId + "/messages",
                new CreateMessageSocketRequest("hello realtime")
        );

        Thread.sleep(1_000L);

        List<ChatMessageJpaEntity> savedMessages =
                chatMessageJpaRepository.findAllByRoomJpaEntity_IdAndIsDeletedFalseOrderByCreatedTimeAscIdAsc(roomId);

        assertThat(savedMessages).hasSize(1);
        ChatMessageJpaEntity savedMessage = savedMessages.getFirst();
        assertThat(savedMessage.getRoomJpaEntity().getId()).isEqualTo(roomId);
        assertThat(savedMessage.getUserJpaEntity().getId()).isEqualTo(participant.getId());
        assertThat(savedMessage.getSenderNickname()).isEqualTo(participant.getNickname());
        assertThat(savedMessage.getMessage()).isEqualTo("hello realtime");
        assertThat(savedMessage.getCreatedTime()).isNotNull();
        assertThat(savedMessage.isDeleted()).isFalse();
    }

    @Test
    void subscribe_fails_for_non_participant() throws Exception {
        UserJpaEntity owner = saveUser("realtime-owner2@test.com", "password123!", "rt-owner2", "Realtime Owner2");
        UserJpaEntity outsider = saveUser("realtime-outsider@test.com", "password123!", "rt-outsider", "Realtime Outsider");

        Long roomId = createRoom(owner.getId(), "Realtime Guard Room");

        TestStompSessionHandler outsiderHandler = new TestStompSessionHandler();
        StompSession outsiderSession = connect(createAccessToken(outsider.getId()), outsiderHandler);

        subscribe(outsiderSession, roomId);

        String errorMessage = outsiderHandler.errorMessage().get(5, TimeUnit.SECONDS);

        assertThat(errorMessage)
                .contains("Failed to send message to ExecutorSubscribableChannel[clientInboundChannel]");
    }

    @Test
    void leaveRoom_disconnects_websocket_session_after_commit() throws Exception {
        UserJpaEntity owner = saveUser("leave-ws-owner@test.com", "password123!", "leave-ws-owner", "Leave Ws Owner");
        UserJpaEntity participant = saveUser("leave-ws-participant@test.com", "password123!", "leave-ws-participant", "Leave Ws Participant");

        Long roomId = createRoom(owner.getId(), "Leave WebSocket Room");
        RoomJpaEntity room = roomJpaRepository.findById(roomId).orElseThrow();

        joinRoom(room.getRoomCode(), participant.getId());

        TestStompSessionHandler participantHandler = new TestStompSessionHandler();
        StompSession participantSession = connect(createAccessToken(participant.getId()), participantHandler);
        subscribe(participantSession, roomId);

        mockMvc.perform(post("/api/rooms/{roomId}/leave", roomId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(participant.getId())))
                .andExpect(status().isOk());

        boolean disconnected = false;
        for (int attempt = 0; attempt < 20; attempt++) {
            if (!participantSession.isConnected()) {
                disconnected = true;
                break;
            }
            Thread.sleep(100L);
        }

        assertThat(disconnected).isTrue();
    }

    @Test
    void deleteMessage_broadcasts_deletion_and_marks_message_deleted() throws Exception {
        UserJpaEntity owner = saveUser("delete-owner@test.com", "password123!", "delete-owner", "Delete Owner");
        Long roomId = createRoom(owner.getId(), "Delete Realtime Room");
        RoomJpaEntity room = roomJpaRepository.findById(roomId).orElseThrow();

        ChatMessageJpaEntity savedMessage = chatMessageJpaRepository.save(
                new ChatMessageJpaEntity("delete-owner", "remove me", room, owner)
        );

        mockMvc.perform(delete("/api/messages/{messageId}", savedMessage.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(owner.getId())))
                .andExpect(status().isNoContent());

        ChatMessageJpaEntity deletedMessage = chatMessageJpaRepository.findById(savedMessage.getId()).orElseThrow();
        assertThat(deletedMessage.isDeleted()).isTrue();
        assertThat(deletedMessage.getMessage()).isEqualTo("remove me");

        verify(publishDeletedChatMessagePortOut).publish(argThat(data ->
                data.roomId().equals(roomId) && data.messageId().equals(savedMessage.getId())
        ));
    }

    private StompSession connect(String accessToken, TestStompSessionHandler sessionHandler) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.setThreadNamePrefix("stomp-receipt-");
        taskScheduler.initialize();
        stompClient.setTaskScheduler(taskScheduler);
        stompClient.setReceiptTimeLimit(5_000);
        stompClients.add(stompClient);

        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        CompletableFuture<StompSession> sessionFuture = stompClient.connectAsync(
                "ws://localhost:" + port + "/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                sessionHandler
        );

        StompSession session = sessionFuture.get(5, TimeUnit.SECONDS);
        session.setAutoReceipt(true);
        return session;
    }

    private void subscribe(StompSession session, Long roomId) {
        session.subscribe("/sub/rooms/" + roomId, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // This test focuses on end-to-end authorization and persistence.
            }
        });
    }

    private Long createRoom(Long ownerId, String title) throws Exception {
        String responseBody = mockMvc.perform(post("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(ownerId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s"
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode response = objectMapper.readTree(responseBody);
        return response.get("roomId").asLong();
    }

    private void joinRoom(String roomCode, Long userId) throws Exception {
        mockMvc.perform(post("/api/rooms/{roomCode}/join", roomCode)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createAccessToken(userId)))
                .andExpect(status().isOk());
    }

    private String createAccessToken(Long userId) {
        return jwtTokenProvider.generateAccessToken(userId);
    }

    private UserJpaEntity saveUser(String email, String rawPassword, String nickname, String name) {
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

    private void clearPersistence() {
        chatMessageJpaRepository.deleteAll();
        roomParticipantJpaRepository.deleteAll();
        roomJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        refreshTokenPortOut.clear();
    }

    private static class TestStompSessionHandler extends StompSessionHandlerAdapter {
        private final CompletableFuture<String> errorMessage = new CompletableFuture<>();

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            String message = headers.getFirst("message");
            if (message != null) {
                errorMessage.complete(message);
                return;
            }

            if (payload != null) {
                errorMessage.complete(String.valueOf(payload));
            }
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            if (!errorMessage.isDone()) {
                errorMessage.complete(exception.getMessage());
            }
        }

        CompletableFuture<String> errorMessage() {
            return errorMessage;
        }
    }
}
