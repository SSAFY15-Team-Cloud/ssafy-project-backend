# Room 생성/수정/삭제 PR 정리

## 개요

`Room` 도메인에 대해 방 생성, 수정, 삭제(soft delete) 로직을 추가했습니다.

- 생성: 방 제목과 호스트 사용자 정보를 기반으로 방 생성
- 수정: 방 생성자만 제목 수정 가능
- 삭제: 물리 삭제 대신 `status = ENDED`로 변경

## 주요 변경 사항

### 1. In Port / Out Port 분리

헥사고날 아키텍처 흐름에 맞게 유스케이스별 입력 포트와 출력 포트를 분리했습니다.

- In Port
  - `CreateRoomPortIn`
  - `UpdateRoomPortIn`
  - `DeleteRoomPortIn`

- Out Port
  - `SaveRoomPortOut`
  - `LoadRoomPortOut`
  - `UpdateRoomPortOut`
  - `DeleteRoomPortOut`

### 2. Application 계층 전용 모델 추가

웹 DTO와 애플리케이션 유스케이스 모델을 분리했습니다.

- 생성
  - `CreateRoomCommand`
  - `CreateRoomResult`

- 수정
  - `UpdateRoomCommand`
  - `UpdateRoomResult`

- 삭제
  - `DeleteRoomCommand`

컨트롤러는 Request DTO를 Command로 변환해서 서비스에 전달하고, 서비스의 Result를 Response DTO로 변환해서 응답합니다.

### 3. Room 생성 로직

- 요청값: `title`, `userId`
- 생성 시 `room_code`를 생성
- 생성 시 초기 상태를 `RUNNING`으로 설정

생성 결과로 다음 정보를 반환합니다.

- `roomId`
- `title`
- `hostId`
- `roomCode`
- `createdAt`

### 4. Room 수정 로직

수정 시 아래 조건을 검증합니다.

- 요청자가 방 생성자인지 확인
- 방 상태가 `RUNNING`인지 확인

조건을 만족하면 제목을 수정합니다.

### 5. Room 삭제 로직

삭제는 실제 row 삭제가 아니라 soft delete 방식으로 처리합니다.

- `status`를 `ENDED`로 변경
- `endedTime` 기록

삭제 시 아래 조건을 검증합니다.

- 요청자가 방 생성자인지 확인
- 이미 종료된 방인지 확인

## 계층별 흐름

### 생성

`CreateRoomRequest`
-> `RoomController`
-> `CreateRoomCommand`
-> `CreateRoomPortIn`
-> `RoomService`
-> `SaveRoomPortOut`
-> `RoomAdapter`
-> `CreateRoomResult`
-> `CreateRoomResponse`

### 수정

`UpdateRoomRequest`
-> `RoomController`
-> `UpdateRoomCommand`
-> `UpdateRoomPortIn`
-> `RoomService`
-> `LoadRoomPortOut`
-> 권한/상태 검증
-> `UpdateRoomPortOut`
-> `UpdateRoomResult`
-> `UpdateRoomResponse`

### 삭제

`DELETE /api/rooms/{roomId}`
-> `RoomController`
-> `DeleteRoomCommand`
-> `DeleteRoomPortIn`
-> `RoomService`
-> `LoadRoomPortOut`
-> 권한/상태 검증
-> `DeleteRoomPortOut`

## API 요약

### 방 생성

- `POST /api/rooms`

Request

```json
{
  "title": "테스트 방"
}
```

Response

```json
{
  "roomId": 1,
  "title": "테스트 방",
  "hostId": 10,
  "roomCode": "A1B2C3D4",
  "createdAt": "2026-03-24T20:00:00"
}
```

### 방 수정

- `PUT /api/rooms/{roomId}`

Request

```json
{
  "title": "수정된 방 제목"
}
```

Response

```json
{
  "roomId": 1,
  "title": "수정된 방 제목"
}
```

### 방 삭제

- `DELETE /api/rooms/{roomId}`

Response

- `204 No Content`

## 설계 관점

### 헥사고날 아키텍처

- web adapter의 Request/Response DTO와 application 계층의 Command/Result를 분리했습니다.
- application 계층은 web DTO나 JPA Entity를 직접 의존하지 않도록 구성했습니다.
- 서비스는 port를 통해서만 외부 자원에 접근합니다.

### SOLID

- SRP
  - Controller는 요청/응답 변환 담당
  - Service는 유스케이스와 정책 담당
  - Adapter는 persistence 변환과 저장 담당

- DIP
  - Service는 구현체가 아니라 port interface에 의존합니다.

- ISP
  - `CreateRoom`, `UpdateRoom`, `DeleteRoom`을 각각의 in port로 분리했습니다.

## 검증

- `./mvnw -q test` 실행 통과

## 참고

- 현재 수정 로직은 service에서 조회 후 adapter에서 다시 조회하는 구조라 update 시 조회가 2번 발생할 수 있습니다.
- 현재 테스트는 컨텍스트 로딩 중심이라, 이후에는 `RoomService` 단위 테스트를 추가하는 것이 좋습니다.
