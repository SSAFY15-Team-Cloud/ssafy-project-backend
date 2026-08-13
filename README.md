# Meetiny — AI 온라인 미팅 서비스

Zoom처럼 회의를 만들고 초대코드/딥링크로 참여하는 화상회의 서비스에, AI가 회의 내내 함께합니다.

- **화상회의**: LiveKit(SFU) 기반 다자간 영상/음성/화면공유
- **초대**: 8자리 초대코드 + 딥링크(`/join/{roomCode}`)
- **실시간 AI 자막**: 마이크 발화를 15초 청크로 Whisper STT → 회의방에 자막 브로드캐스트
- **실시간 AI 인사이트**: 발화가 쌓일 때마다 롤링 요약 · 액션아이템 · 미해결 쟁점 갱신
- **지식 위키 문서 추천**: 업로드한 문서를 임베딩(pgvector)해두고, 대화 맥락과 관련된 문서를 회의 중 실시간 추천 + 다운로드 링크 제공
- **자동 회의록**: 회의 종료 시 발언자별 요약/결정사항/액션아이템이 정리된 회의록 생성
- **AI 코파일럿**: 회의 중 "지금까지 결정된 게 뭐야?" 질문 → 트랜스크립트 + 위키 RAG 답변(출처 포함)
- **번역 자막**: 실시간 자막을 English/日本語로 토글 (원문 병기)
- **리액션 · 손들기**: LiveKit 데이터 채널 기반 플로팅 이모지 / 손들기 배너
- **발언 점유율**: 참가자별 발언 시간 실시간 통계 바
- **배경 블러**: 카메라 가상 배경 처리 (Chromium)
- **회의 분위기**: 인사이트에 무드 게이지(이모지+라벨)
- **자막 오버레이**: 영화 자막 스타일 CC를 비디오 위에 표시 (토글)
- **가상 배경**: 블러 + 배경 이미지 3종 (Chromium)
- **통합 시맨틱 검색**: 내가 참여한 회의의 발화 + 위키 문서를 의미 기반으로 한 번에 검색 (`/search`)
- **채팅**: STOMP 기반 회의방 채팅 (참가자 검증 포함)

## 아키텍처

```
frontend (React 19 + Vite + TS + Tailwind v4)
   │  REST /api/**        ─→  Spring Boot 4 (Java 21, 헥사고날)
   │  STOMP /ws           ─→  채팅 · 자막 · AI 인사이트 · 문서 추천 브로드캐스트
   │  WebRTC              ─→  LiveKit (docker, 토큰은 백엔드가 발급)
   │
backend ──→ PostgreSQL+pgvector · Redis(refresh token) · RabbitMQ(STT 큐)
        ──→ MinIO(S3 호환: 오디오/프로필/문서) · OpenAI(Whisper/GPT/임베딩)
```

STT 파이프라인: `마이크 청크 → presigned PUT(MinIO) → 메타데이터 등록 → RabbitMQ
→ Whisper 전사 → DB 저장 → STOMP 자막 + AI 인사이트 + 문서 추천 → (종료 시) 회의록`

## 로컬 실행

### 0. 준비물

- Docker Desktop, JDK 21, Node 20+
- OpenAI API 키

### 1. 시크릿 설정

`src/main/resources/secrets.properties` 파일 생성 (gitignore 대상):

```properties
openai.api.key=sk-...
```

### 2. 인프라 기동

```bash
docker compose up -d
```

| 서비스 | 포트 | 비고 |
|---|---|---|
| PostgreSQL(pgvector) | **5433** | 로컬 5432 점유 회피 |
| Redis | 6379 | |
| RabbitMQ | 5672 / 15672(관리 UI, ssafy/ssafy) | |
| MinIO | **19000** / 19001(콘솔, minioadmin/minioadmin) | `meeting-media` 버킷 자동 생성. 9000은 Windows WinNAT 예약과 충돌해 회피 |
| LiveKit | 7880(ws) / 7881(tcp) / 7882(udp) | 키: `docker/livekit/livekit.yaml` |

### 3. 백엔드

```bash
./mvnw spring-boot:run
```

→ http://localhost:8081 (스키마는 `ddl-auto=update`로 자동 생성)

### 4. 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

→ http://localhost:5173 (`/api`, `/ws`는 vite proxy로 백엔드에 연결)

### 5. 테스트

```bash
./mvnw test          # 통합 테스트 (Testcontainers — Docker 필요)
cd frontend && npm run build   # 타입체크 + 빌드
```

## 주요 API

| 기능 | 엔드포인트 |
|---|---|
| 회원가입/로그인/재발급 | `POST /api/auth/signup` `/login` `/reissue` |
| 방 생성/조회/종료 | `POST /api/rooms` · `GET /api/rooms/{roomCode}` · `DELETE /api/rooms/{roomId}` |
| 참여/나가기/참가자 | `POST /api/rooms/{roomCode}/join` · `POST /api/rooms/{roomId}/leave` · `GET /api/rooms/{roomId}/participants` |
| **LiveKit 토큰** | `POST /api/rooms/{roomId}/rtc-token` (활성 참가자만) |
| 오디오 업로드 | `GET /api/rooms/{roomId}/audios/upload-url?extension=webm` → S3 PUT → `POST /api/rooms/{roomId}/audios` |
| 회의록 | `GET /api/rooms/{roomId}/report/status` · `GET /api/rooms/{roomId}/report` |
| 지식 위키 | `POST/GET /api/knowledge/documents` · `GET .../{id}/download-url` · `DELETE .../{id}` |
| **AI 코파일럿** | `POST /api/rooms/{roomId}/copilot` `{question}` → 답변+출처 |
| 자막 번역 | `POST /api/ai/translate` `{texts[], targetLang}` |
| 발언 통계 | `GET /api/rooms/{roomId}/speaking-stats` |
| 내 회의 이력 | `GET /api/users/me/rooms` |
| **통합 검색** | `GET /api/search?q=` (위키 + 내 회의 발화, pgvector) |

STOMP (연결: `/ws`, CONNECT 헤더 `Authorization: Bearer {accessToken}`):

| destination | 용도 |
|---|---|
| `/pub/rooms/{roomId}/messages` | 채팅 발행 |
| `/sub/rooms/{roomId}` | 채팅 수신 |
| `/sub/rooms/{roomId}/deletions` | 채팅 삭제 알림 |
| `/sub/rooms/{roomId}/transcripts` | 실시간 자막 |
| `/sub/rooms/{roomId}/ai/insights` | 롤링 요약/액션아이템 |
| `/sub/rooms/{roomId}/ai/recommendations` | 관련 문서 추천 |

## 운영 전 챙길 것

- `CookieProvider`의 refresh 쿠키 `secure=true` 전환 (HTTPS 배포 시)
- LiveKit 키(`docker/livekit/livekit.yaml`)와 JWT `SECRET_KEY`를 운영 값으로 교체
- 과거 로컬 설정에 평문으로 있던 AWS/OpenAI/DB 자격증명은 이미 저장소에서 제거했지만, **키 로테이션 권장**
- STOMP SimpleBroker는 단일 인스턴스 전제 — 스케일아웃 시 RabbitMQ STOMP relay로 전환
