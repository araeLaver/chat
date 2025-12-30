# CHANGELOG

## [1.2.0] - 2025-12-30

### Security Hardening (CRITICAL/HIGH)
- SQL Injection 방지: JPA Named Parameters 적용
- XSS 방지: 입력값 sanitization 추가
- 파일 업로드 보안: `FileSecurityValidator` 추가
  - MIME 타입 검증
  - 파일 확장자 화이트리스트
  - 파일명 sanitization
  - Path Traversal 방지
- JWT 토큰 보안 강화

### Code Quality Improvements (MEDIUM)
- System.out.println → SLF4J Logger 교체
- God Class 리팩토링: `ChatWebSocketHandler` 분리
  - `WebSocketSessionManager` - 세션 관리
  - `ChatRoomManager` - 채팅방 관리
  - `WebSocketMessageSender` - 메시지 전송
  - `RoomMessageHandler` - 방 관련 처리
  - `ChatMessageHandler` - 채팅 메시지 처리
- 검증 로직 DTO 분리

### Refactoring (LOW)
- 매직 넘버 상수화
  - `FileStorageService.THUMBNAIL_WIDTH`
  - `AuthController.MAX_GUEST_ID`
  - `AuthController.DEFAULT_ROOM_MAX_MEMBERS`
- 유틸리티 클래스 추가
  - `ResponseHelper` - API 응답 생성
  - `AuthUtil` - JWT 토큰 추출
- 설정 외부화
  - `${guest.default-room-name}` - 기본 채팅방 이름
  - `${monitoring.update-interval-ms}` - 모니터링 간격
- `ThreadLocalRandom` 적용 (스레드 안전)

### Production Configuration
- Actuator 엔드포인트 활성화
  - `/actuator/prometheus`
  - `/actuator/metrics`
- CORS 설정 추가
  - `cors.allowed-origins` for Koyeb domain
- WebSocket 설정 개선
  - `TokenHandshakeInterceptor` 추가
  - `setAllowedOriginPatterns("*")` 적용

### Maintenance
- `.gitignore` 정리
  - Eclipse 파일 (`.classpath`, `.project`)
  - AI 도구 설정 (`.ai/`, `.agent/`, `.assistant/`, `.claude/`)

---

## Files Changed

### New Files
- `src/main/java/com/beam/util/ResponseHelper.java`
- `src/main/java/com/beam/util/AuthUtil.java`
- `src/main/java/com/beam/websocket/TokenHandshakeInterceptor.java`
- `src/main/java/com/beam/websocket/WebSocketSessionManager.java`
- `src/main/java/com/beam/websocket/ChatRoomManager.java`
- `src/main/java/com/beam/websocket/WebSocketMessageSender.java`
- `src/main/java/com/beam/websocket/handler/RoomMessageHandler.java`
- `src/main/java/com/beam/websocket/handler/ChatMessageHandler.java`
- `src/main/java/com/beam/FileSecurityValidator.java`

### Modified Files
- `src/main/java/com/beam/AuthController.java`
- `src/main/java/com/beam/FileStorageService.java`
- `src/main/java/com/beam/MonitoringService.java`
- `src/main/java/com/beam/ChatWebSocketHandler.java`
- `src/main/java/com/beam/WebSocketConfig.java`
- `src/main/resources/application.properties`
- `src/main/resources/application-prod.properties`
- `.gitignore`

---

## Production Verification

| Endpoint | Status |
|----------|--------|
| `/actuator/health` | UP |
| `/actuator/prometheus` | OK |
| `/actuator/metrics` | OK |
| `/swagger-ui/index.html` | OK (56 APIs) |
| `/ws` (WebSocket) | Connected |

### Metrics Summary
- `beam_active_users`: 30
- `beam_active_rooms`: 1
- `beam_total_messages`: 0
- JVM: OpenJDK 17.0.17
- DB Pool: HikariCP (10 connections)
