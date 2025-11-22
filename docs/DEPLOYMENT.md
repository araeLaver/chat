# BEAM 배포 가이드

## 목차
1. [사전 준비](#사전-준비)
2. [환경 변수 설정](#환경-변수-설정)
3. [데이터베이스 설정](#데이터베이스-설정)
4. [이메일 설정](#이메일-설정)
5. [Docker 배포](#docker-배포)
6. [Koyeb 배포](#koyeb-배포)
7. [모니터링](#모니터링)

---

## 사전 준비

### 필수 요구사항
- Java 17+
- Maven 3.8+
- PostgreSQL 15+
- Docker (선택)

### 빌드
```bash
# 프로젝트 클론
git clone https://github.com/araeLaver/simple-chat-server.git
cd simple-chat-server

# Maven 빌드
mvn clean package -DskipTests -Pprod

# JAR 파일 확인
ls target/beam-server-1.0.0.jar
```

---

## 환경 변수 설정

### 필수 환경 변수

```bash
# 데이터베이스
DATABASE_URL=jdbc:postgresql://host/db?currentSchema=chat&sslmode=require
DATABASE_USERNAME=username
DATABASE_PASSWORD=password

# JWT 보안키 (256비트 이상)
JWT_SECRET=your-super-secret-key-at-least-256-bits-long

# 이메일 (Gmail SMTP)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# CORS 허용 도메인
CORS_ALLOWED_ORIGINS=https://your-domain.com

# Spring 프로파일
SPRING_PROFILES_ACTIVE=prod

# 서버 포트
PORT=8080
```

### JWT 시크릿 키 생성
```bash
# OpenSSL로 안전한 키 생성
openssl rand -base64 64
```

---

## 데이터베이스 설정

### PostgreSQL 스키마 생성

1. **PostgreSQL 접속**
```bash
psql -h your-host -U your-username -d your-database
```

2. **스키마 초기화**
```sql
-- init-chat.sql 파일 실행
\i src/main/resources/db/init-chat.sql
```

또는 SQL 클라이언트에서 `init-chat.sql` 내용을 직접 실행

### 테이블 목록
- `users` - 사용자
- `friends` - 친구 관계
- `conversations` - 1:1 대화
- `direct_messages` - DM 메시지
- `rooms` - 그룹 채팅방
- `room_members` - 채팅방 멤버
- `group_messages` - 그룹 메시지
- `file_metadata` - 파일 정보
- `read_receipts` - 읽음 표시
- `user_sessions` - 세션

---

## 이메일 설정

### Gmail SMTP 설정

1. **Google 계정 설정**
   - [Google 계정](https://myaccount.google.com) 접속
   - 보안 → 2단계 인증 활성화
   - 보안 → 앱 비밀번호 → 앱 선택: 메일, 기기: Windows
   - 생성된 16자리 비밀번호 복사

2. **환경 변수 설정**
```bash
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=xxxx-xxxx-xxxx-xxxx  # 앱 비밀번호
```

### 기타 SMTP 서비스

**Naver:**
```bash
MAIL_HOST=smtp.naver.com
MAIL_PORT=587
```

**AWS SES:**
```bash
MAIL_HOST=email-smtp.ap-northeast-2.amazonaws.com
MAIL_PORT=587
```

---

## Docker 배포

### Dockerfile
프로젝트에 포함된 `Dockerfile` 사용

### 이미지 빌드
```bash
docker build -t beam-server:latest .
```

### 컨테이너 실행
```bash
docker run -d \
  --name beam-server \
  -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://host/db?currentSchema=chat&sslmode=require" \
  -e DATABASE_USERNAME="username" \
  -e DATABASE_PASSWORD="password" \
  -e JWT_SECRET="your-secret-key" \
  -e MAIL_USERNAME="email@gmail.com" \
  -e MAIL_PASSWORD="app-password" \
  -e CORS_ALLOWED_ORIGINS="https://your-domain.com" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  beam-server:latest
```

### Docker Compose
```yaml
version: '3.8'
services:
  beam-server:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DATABASE_URL=jdbc:postgresql://db:5432/beam?currentSchema=chat
      - DATABASE_USERNAME=beam
      - DATABASE_PASSWORD=secret
      - JWT_SECRET=your-secret-key
      - MAIL_USERNAME=email@gmail.com
      - MAIL_PASSWORD=app-password
      - CORS_ALLOWED_ORIGINS=https://your-domain.com
      - SPRING_PROFILES_ACTIVE=prod
    depends_on:
      - db

  db:
    image: postgres:15
    environment:
      - POSTGRES_DB=beam
      - POSTGRES_USER=beam
      - POSTGRES_PASSWORD=secret
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./src/main/resources/db/init-chat.sql:/docker-entrypoint-initdb.d/init.sql

volumes:
  postgres_data:
```

---

## Koyeb 배포

### 1. GitHub 연동
1. [Koyeb](https://www.koyeb.com) 로그인
2. Create App → GitHub 선택
3. Repository 선택: `araeLaver/simple-chat-server`
4. Branch: `main`

### 2. 환경 변수 설정
Koyeb 대시보드에서 Environment Variables 추가:

| Name | Value |
|------|-------|
| DATABASE_URL | jdbc:postgresql://... |
| DATABASE_USERNAME | username |
| DATABASE_PASSWORD | password |
| JWT_SECRET | your-secret |
| MAIL_USERNAME | email@gmail.com |
| MAIL_PASSWORD | app-password |
| CORS_ALLOWED_ORIGINS | https://your-app.koyeb.app |
| SPRING_PROFILES_ACTIVE | prod |

### 3. 빌드 설정
- Builder: Dockerfile
- Port: 8080

### 4. 배포
Deploy 클릭 후 빌드 완료 대기

---

## 모니터링

### Health Check
```bash
curl https://your-domain.com/actuator/health
```

### Prometheus Metrics
```bash
curl https://your-domain.com/actuator/prometheus
```

### 주요 엔드포인트
- `/actuator/health` - 서버 상태
- `/actuator/info` - 앱 정보
- `/actuator/metrics` - 메트릭
- `/actuator/prometheus` - Prometheus 포맷

### 로그 확인

**Docker:**
```bash
docker logs -f beam-server
```

**Koyeb:**
- 대시보드 → Logs 탭

---

## 트러블슈팅

### 데이터베이스 연결 실패
```
HikariPool-1 - Connection is not available
```
→ DATABASE_URL, USERNAME, PASSWORD 확인

### JWT 토큰 에러
```
JWT signature does not match
```
→ JWT_SECRET이 배포 환경과 동일한지 확인

### 이메일 발송 실패
```
Authentication failed
```
→ Gmail 앱 비밀번호 확인, 2단계 인증 활성화 여부 확인

### CORS 에러
```
Access-Control-Allow-Origin
```
→ CORS_ALLOWED_ORIGINS에 프론트엔드 도메인 추가

---

## 성능 최적화

### JVM 옵션 (프로덕션)
```bash
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

### 커넥션 풀 설정
`application-prod.properties`에서 조정:
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

### 캐시 설정
```properties
spring.cache.caffeine.spec=expireAfterWrite=30m,maximumSize=5000
```
