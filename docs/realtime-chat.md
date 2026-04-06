# Realtime Chat Overview

## 목적

이 문서는 현재 프로젝트에 적용된 실시간 채팅 구조를 정리한다.

- WebSocket/STOMP 연결 방식
- 인증과 구독 권한 처리 방식
- 채팅 송신 및 발행 흐름
- 테스트 페이지 사용 방법

---

## 핵심 규약

### WebSocket endpoint

- `/ws`

클라이언트는 이 endpoint로 WebSocket 연결을 맺는다.

### STOMP destination

- 송신: `/pub/rooms/{roomId}/messages`
- 구독: `/sub/rooms/{roomId}`

의미는 다음과 같다.

- `/pub/...`
  - 클라이언트가 서버로 메시지를 보내는 주소
- `/sub/...`
  - 서버가 구독자들에게 메시지를 뿌리는 주소

---

## 전체 흐름

실시간 채팅은 아래 순서로 동작한다.

1. 사용자가 로그인해서 access token을 받는다.
2. 사용자가 `roomCode`로 방 입장 REST API를 호출한다.
3. 서버는 해당 사용자를 room participant active 상태로 저장한다.
4. 클라이언트는 `/ws` 로 WebSocket 연결을 맺는다.
5. STOMP `CONNECT` 에 `Authorization: Bearer {accessToken}` 을 보낸다.
6. 서버는 토큰을 검증하고 세션에 `userId`를 저장한다.
7. 클라이언트는 `/sub/rooms/{roomId}` 를 구독한다.
8. 서버는 해당 사용자가 active participant인지 확인한다.
9. 클라이언트가 `/pub/rooms/{roomId}/messages` 로 메시지를 보낸다.
10. 서버는 메시지를 저장하고 `/sub/rooms/{roomId}` 로 발행한다.
11. 같은 방을 구독 중인 모든 사용자가 메시지를 받는다.

---

## 계층별 책임

### 1. WebSocket 설정

파일:

- `src/main/java/com/ssafy/ssafy_project/global/infrastructure/config/WebSocketConfig.java`

역할:

- `/ws` endpoint 등록
- `/pub` application prefix 설정
- `/sub` simple broker 설정
- inbound channel interceptor 등록

현재 설정 요약:

```java
registry.enableSimpleBroker("/sub");
registry.setApplicationDestinationPrefixes("/pub");
registry.addEndpoint("/ws").setAllowedOriginPatterns("*");
registration.interceptors(webSocketAuthChannelInterceptor);
```

### 2. WebSocket 인증 및 구독 권한 체크

파일:

- `src/main/java/com/ssafy/ssafy_project/global/infrastructure/websocket/WebSocketAuthChannelInterceptor.java`
- `src/main/java/com/ssafy/ssafy_project/global/infrastructure/websocket/StompPrincipal.java`

역할:

- `CONNECT` 시 JWT 검증
- 검증 성공 시 `userId`를 `Principal`과 session attribute에 저장
- `SUBSCRIBE /sub/rooms/{roomId}` 시 active participant 여부 확인

핵심 정책:

- `/ws` handshake 자체는 열어둔다.
- 실제 인증은 STOMP `CONNECT` 에서 처리한다.
- 실제 방 접근 권한은 `SUBSCRIBE` 에서 처리한다.

### 3. STOMP inbound adapter

파일:

- `src/main/java/com/ssafy/ssafy_project/chat/adapter/in/websocket/ChatStompController.java`
- `src/main/java/com/ssafy/ssafy_project/chat/adapter/in/websocket/dto/request/CreateMessageSocketRequest.java`

역할:

- `/pub/rooms/{roomId}/messages` 로 들어온 메시지를 받는다.
- session attribute에 저장된 `userId`를 읽는다.
- `CreateMessageCommand`를 만들어 application service로 전달한다.

### 4. Application service

파일:

- `src/main/java/com/ssafy/ssafy_project/chat/application/service/ChatMessageService.java`

역할:

- 송신자가 방 참가자인지 검증
- 메시지 저장
- 저장된 메시지를 실시간 발행 포트로 전달
- 기존 REST 조회 로직과 삭제 로직도 함께 담당

### 5. Persistence adapter

파일:

- `src/main/java/com/ssafy/ssafy_project/chat/adapter/out/persistence/ChatMessageJpaAdapter.java`

역할:

- 채팅 메시지 저장
- 메시지 조회
- 삭제 반영

주의:

- 저장 후 생성된 `messageId`, `createdTime` 등을 포함한 `ChatMessage`를 반환한다.
- 조회는 정렬 보장을 위해 `createdTime`, `id` 기준 오름차순으로 가져오도록 맞춰야 한다.

### 6. Messaging outbound adapter

파일:

- `src/main/java/com/ssafy/ssafy_project/chat/application/port/out/PublishChatMessagePortOut.java`
- `src/main/java/com/ssafy/ssafy_project/chat/application/port/out/ChatMessagePublishedData.java`
- `src/main/java/com/ssafy/ssafy_project/chat/adapter/out/messaging/StompChatPublisherAdapter.java`
- `src/main/java/com/ssafy/ssafy_project/chat/adapter/out/messaging/dto/response/ChatMessagePayload.java`

역할:

- application service가 넘긴 발행 데이터 수신
- `/sub/rooms/{roomId}` 로 실시간 broadcast

핵심 포인트:

- application service는 `SimpMessagingTemplate`를 직접 모른다.
- 실시간 발행 구현은 outbound adapter가 담당한다.

이 구조 덕분에 헥사고날 아키텍처를 유지할 수 있다.

---

## 현재 인증/인가 정책

### 인증

- WebSocket 연결 이후 STOMP `CONNECT` 에서 access token을 검증한다.
- 헤더 형식:

```text
Authorization: Bearer {accessToken}
```

### 인가

- 방 입장 권한은 기존 REST 입장 로직에서 `roomCode`로 검증한다.
- 실시간 채팅 구독 권한은 room participant active 상태로 판단한다.

즉 정책은 아래와 같다.

- `roomCode`는 입장 자격 확인용
- WebSocket 구독과 송신 권한은 `room participant` 상태로 판단

---

## 왜 roomCode를 WebSocket에서 직접 쓰지 않는가

WebSocket 구독 주소는 `roomId` 기반이다.

예:

- `/sub/rooms/1`

여기에는 `roomCode`가 포함되지 않는다.

따라서 권한 체크는 아래 2단계로 나눈다.

1. REST 입장 시 `roomCode` 확인
2. 이후 WebSocket에서는 active participant 여부 확인

이 방식이 가장 단순하고 안정적이다.

---

## 테스트용 페이지

파일:

- `src/main/resources/static/ws-test/index.html`

접속 주소:

- `http://localhost:8081/ws-test/`

기능:

- WebSocket 연결
- STOMP CONNECT
- 방 구독
- 채팅 메시지 전송
- 수신 로그 확인

### 사용 방법

1. 사용자 A로 로그인하고 access token 확보
2. 사용자 A로 방 입장 완료
3. 테스트 페이지 열기
4. `roomId`, `accessToken` 입력
5. `Connect`
6. `Subscribe`
7. 메시지 입력 후 `Send`

여러 사용자 동시 테스트:

1. 브라우저 창을 2개 이상 연다.
2. 서로 다른 사용자 토큰을 각각 입력한다.
3. 같은 `roomId`를 넣고 모두 `Connect`, `Subscribe` 한다.
4. 한 창에서 메시지를 보내면 다른 창에서도 수신되는지 확인한다.

---

## 테스트 시 주의사항

### 1. 입장을 먼저 해야 한다

방 참가(active participant) 상태가 없으면 구독이 거절된다.

즉 아래 순서가 필수다.

1. 로그인
2. 방 입장
3. WebSocket 연결
4. 구독
5. 송신

### 2. roomCode와 roomId를 혼동하지 않는다

- 입장: `roomCode`
- 구독/송신: `roomId`

### 3. 토큰은 access token을 사용한다

refresh token 쿠키는 WebSocket 인증에 사용하지 않는다.

---

## 현재 구현 상태

현재 확인된 내용:

- WebSocket 연결 가능
- STOMP `CONNECT` 성공
- STOMP `SUBSCRIBE` 성공
- STOMP `SEND` 성공
- 메시지 저장 성공
- 같은 방 구독자에게 실시간 broadcast 성공

즉 기본적인 1:1, N:1, N:N 실시간 채팅 파이프라인은 동작한다.

---

## 남아 있는 개선 포인트

아직 보강 가능한 부분은 아래와 같다.

- WebSocket/STOMP 관련 통합 테스트 추가
- 예외를 STOMP 에러 응답으로 더 명확하게 변환
- 구독 실패/송신 실패 로그 체계화
- 필요 시 `/ws` CORS 및 origin 정책 구체화
- 필요 시 구독 취소, 연결 종료 이벤트 처리

---

## 정리

현재 실시간 채팅 구조는 아래 한 문장으로 요약할 수 있다.

> REST 입장으로 참가 상태를 만든 뒤, STOMP `CONNECT`에서 인증하고, `SUBSCRIBE`에서 구독 권한을 검증하며, `SEND`로 저장된 메시지를 `/sub/rooms/{roomId}`에 broadcast 하는 구조다.

이 구조를 기준으로 프론트는 다음 규약만 맞추면 된다.

- WebSocket endpoint: `/ws`
- STOMP CONNECT header: `Authorization: Bearer {accessToken}`
- SUBSCRIBE: `/sub/rooms/{roomId}`
- SEND: `/pub/rooms/{roomId}/messages`

