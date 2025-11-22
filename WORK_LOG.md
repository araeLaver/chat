# BEAM Chat Server - 작업 기록

## 완료된 작업

### 1. Lombok 제거 및 수동 코드 변환
**문제**: Docker 빌드 시 Lombok 어노테이션 프로세서가 작동하지 않음
- Windows 로컬, Docker 환경 모두에서 Lombok 컴파일 실패
- 다양한 Maven 설정 시도 실패 (annotationProcessorPaths, forceJavacCompilerUse 등)

**해결**: Lombok 완전 제거, 수동 getter/setter/builder 추가
- 변환된 파일 (18개):
  - Entity: UserEntity, RoomEntity, DirectMessageEntity, ConversationEntity, EmoticonEntity, FileMetadataEntity, FriendEntity, GroupMessageEntity, ReadReceiptEntity, RoomMemberEntity, MessageReadReceipt
  - DTO: AuthRequest, AuthResponse, CreateRoomRequest, AddMemberRequest, AcceptFriendRequestDto, BlockUserRequestDto, UpdateRoomRequest, FriendRequestDto, SendMessageRequest

### 2. 패키지명 수정
**문제**: MessageReadReceipt.java, MessageReadReceiptRepository.java가 잘못된 패키지 `com.chat` 사용

**해결**: `com.beam`으로 수정

### 3. Docker 빌드 환경 수정
**문제**: apt-get으로 Maven 설치 시 JDK 11 의존성 설치로 버전 충돌

**해결**: Dockerfile에서 Maven 직접 다운로드
```dockerfile
ARG MAVEN_VERSION=3.9.6
RUN curl -fsSL https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz | tar xzf - -C /opt
```

### 4. 로그 디렉토리 생성
**문제**: Logback이 `/app/logs` 디렉토리 생성 실패

**해결**: Dockerfile에 추가
```dockerfile
RUN mkdir -p /app/logs && chown -R chatapp:chatapp /app
```

### 5. H2 로컬 프로파일 설정
**문제**: Flyway 마이그레이션 스크립트가 PostgreSQL 전용 문법 사용 (BIGSERIAL, plpgsql)

**해결**: application-local.properties 생성
- Flyway 비활성화: `spring.flyway.enabled=false`
- JPA auto-ddl 사용: `spring.jpa.hibernate.ddl-auto=create-drop`
- H2 콘솔 활성화

### 6. SimpMessageSendingOperations 의존성 수정 (진행 중)
**문제**: 앱이 기본 WebSocket만 사용하는데, STOMP용 SimpMessageSendingOperations를 필수 의존성으로 주입

**해결**: `@Autowired(required = false)` 및 null 체크 추가
- ReadReceiptService.java - 완료
- WebSocketEventListener.java - 완료
- ChatWebSocketController.java - 완료

---

## 현재 에러 상태

### 남은 문제: SimpMessageSendingOperations Bean 없음
```
APPLICATION FAILED TO START
Field messagingTemplate in com.beam.WebSocketEventListener required a bean of type
'org.springframework.messaging.simp.SimpMessageSendingOperations' that could not be found.
```

**원인**:
- WebSocketConfig가 `@EnableWebSocket` (기본 WebSocket)만 사용
- `@EnableWebSocketMessageBroker` (STOMP)가 없어서 SimpMessageSendingOperations Bean이 생성되지 않음
- 3개 클래스에서 이 Bean을 필수로 주입받으려 함

**해결 완료**: 3개 파일 모두 `@Autowired(required = false)` 적용
- ReadReceiptService.java
- WebSocketEventListener.java
- ChatWebSocketController.java

**다음 단계**: Docker 재빌드 후 테스트 필요

---

## 내일 해야 할 작업

1. **Docker 이미지 재빌드**
   ```bash
   docker build --no-cache -t beam-server .
   ```

2. **컨테이너 실행 및 테스트**
   ```bash
   docker run -d --name beam-test -p 8080:8080 -e SPRING_PROFILES_ACTIVE=local -e PORT=8080 beam-server
   docker logs beam-test
   ```

3. **API 테스트**
   - 서버 응답 확인: `curl http://localhost:8080/`
   - H2 콘솔 접속: `http://localhost:8080/h2-console`

4. **Koyeb 배포 준비**
   - PostgreSQL 데이터베이스 설정
   - 환경변수 설정 (DATABASE_URL, JWT_SECRET 등)
   - production 프로파일 설정

---

## 파일 변경 요약

### 수정된 주요 파일
- `pom.xml` - Lombok 의존성 및 컴파일러 설정
- `Dockerfile` - Maven 설치 방식, logs 디렉토리
- `src/main/resources/application-local.properties` - 새 파일 (H2 설정)
- `src/main/java/com/beam/*.java` - 18개 Entity/DTO Lombok 제거
- `src/main/java/com/beam/MessageReadReceipt.java` - 패키지 수정
- `src/main/java/com/beam/MessageReadReceiptRepository.java` - 패키지 수정
- `src/main/java/com/beam/ReadReceiptService.java` - SimpMessageSendingOperations optional
- `src/main/java/com/beam/WebSocketEventListener.java` - SimpMessageSendingOperations optional
- `src/main/java/com/beam/ChatWebSocketController.java` - SimpMessageSendingOperations optional

---

## 기술 스택
- Spring Boot 3.2.0
- Java 17
- H2 (로컬) / PostgreSQL (프로덕션)
- WebSocket (기본, STOMP 아님)
- JWT 인증
- Docker 멀티스테이지 빌드
