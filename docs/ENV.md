# BEAM 환경 변수 가이드

## 전체 환경 변수 목록

| 변수명 | 필수 | 기본값 | 설명 |
|--------|------|--------|------|
| `DATABASE_URL` | O | - | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | O | - | DB 사용자명 |
| `DATABASE_PASSWORD` | O | - | DB 비밀번호 |
| `JWT_SECRET` | O | - | JWT 서명 키 (256비트 이상) |
| `MAIL_HOST` | O | smtp.gmail.com | SMTP 서버 |
| `MAIL_PORT` | O | 587 | SMTP 포트 |
| `MAIL_USERNAME` | O | - | 이메일 계정 |
| `MAIL_PASSWORD` | O | - | 이메일 비밀번호/앱 비밀번호 |
| `CORS_ALLOWED_ORIGINS` | O | localhost | 허용 도메인 (쉼표 구분) |
| `SPRING_PROFILES_ACTIVE` | O | dev | 프로파일 (dev/prod) |
| `PORT` | X | 8080 | 서버 포트 |

---

## 환경별 설정

### 개발 환경 (.env.local)
```bash
# 데이터베이스
DATABASE_URL=jdbc:postgresql://localhost:5432/beam?currentSchema=chat
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=postgres

# JWT (개발용 - 프로덕션에서 변경 필수)
JWT_SECRET=dev-secret-key-for-local-development-only

# 이메일 (개발 시 테스트용)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-dev-email@gmail.com
MAIL_PASSWORD=your-app-password

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:8080,http://localhost:3000

# 프로파일
SPRING_PROFILES_ACTIVE=dev
```

### 프로덕션 환경 (.env.prod)
```bash
# 데이터베이스
DATABASE_URL=jdbc:postgresql://ep-divine-bird-a1f4mly5.ap-southeast-1.pg.koyeb.app/unble?currentSchema=chat&sslmode=require
DATABASE_USERNAME=unble
DATABASE_PASSWORD=npg_1kjV0mhECxqs

# JWT (반드시 안전한 키 사용)
JWT_SECRET=your-production-256-bit-secret-key-here

# 이메일
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=noreply@beam.chat
MAIL_PASSWORD=xxxx-xxxx-xxxx-xxxx

# CORS
CORS_ALLOWED_ORIGINS=https://beam.chat,https://www.beam.chat

# 프로파일
SPRING_PROFILES_ACTIVE=prod

# 포트
PORT=8080
```

---

## 환경 변수 상세 설명

### 데이터베이스

#### DATABASE_URL
PostgreSQL JDBC 연결 URL

**형식:**
```
jdbc:postgresql://[host]:[port]/[database]?currentSchema=[schema]&sslmode=require
```

**예시:**
```bash
# 로컬
DATABASE_URL=jdbc:postgresql://localhost:5432/beam?currentSchema=chat

# Koyeb
DATABASE_URL=jdbc:postgresql://ep-xxx.ap-southeast-1.pg.koyeb.app/unble?currentSchema=chat&sslmode=require

# AWS RDS
DATABASE_URL=jdbc:postgresql://beam-db.xxx.ap-northeast-2.rds.amazonaws.com:5432/beam?currentSchema=chat&sslmode=require
```

---

### 보안

#### JWT_SECRET
JWT 토큰 서명에 사용되는 비밀 키

**요구사항:**
- 최소 256비트 (32자 이상)
- 랜덤하고 예측 불가능한 문자열
- 프로덕션에서 절대 노출 금지

**생성 방법:**
```bash
# OpenSSL
openssl rand -base64 64

# Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"

# Python
python -c "import secrets; print(secrets.token_urlsafe(64))"
```

---

### 이메일

#### MAIL_HOST & MAIL_PORT

| 서비스 | Host | Port |
|--------|------|------|
| Gmail | smtp.gmail.com | 587 |
| Naver | smtp.naver.com | 587 |
| Daum | smtp.daum.net | 465 |
| AWS SES | email-smtp.{region}.amazonaws.com | 587 |

#### MAIL_PASSWORD

**Gmail 앱 비밀번호 생성:**
1. Google 계정 → 보안
2. 2단계 인증 활성화
3. 앱 비밀번호 생성
4. 16자리 비밀번호 복사 (공백 제거)

---

### CORS

#### CORS_ALLOWED_ORIGINS
크로스 오리진 요청을 허용할 도메인 목록

**형식:** 쉼표로 구분
```bash
# 단일 도메인
CORS_ALLOWED_ORIGINS=https://beam.chat

# 다중 도메인
CORS_ALLOWED_ORIGINS=https://beam.chat,https://www.beam.chat,https://app.beam.chat

# 개발 환경
CORS_ALLOWED_ORIGINS=http://localhost:8080,http://localhost:3000
```

---

### Spring 프로파일

#### SPRING_PROFILES_ACTIVE

| 프로파일 | 용도 | 특징 |
|----------|------|------|
| `dev` | 개발 | SQL 로그 출력, 상세 에러 |
| `prod` | 운영 | 최적화, 보안 강화 |

---

## 환경 변수 적용 방법

### 1. 시스템 환경 변수
```bash
# Linux/Mac
export DATABASE_URL="jdbc:postgresql://..."
export JWT_SECRET="your-secret"

# Windows (PowerShell)
$env:DATABASE_URL="jdbc:postgresql://..."
$env:JWT_SECRET="your-secret"
```

### 2. .env 파일
```bash
# .env 파일 생성
cp .env.example .env

# 값 수정
nano .env

# 적용 (Linux/Mac)
export $(cat .env | xargs)
```

### 3. Docker
```bash
# 명령줄
docker run -e DATABASE_URL="..." -e JWT_SECRET="..." beam-server

# env-file
docker run --env-file .env beam-server
```

### 4. Koyeb
대시보드 → Service → Settings → Environment Variables

---

## 보안 주의사항

1. **절대 Git에 커밋하지 마세요**
   - `.env` 파일은 `.gitignore`에 포함
   - 실제 비밀번호를 코드에 하드코딩 금지

2. **프로덕션 키는 안전하게 관리**
   - 환경 변수 관리 서비스 사용 (Vault, AWS Secrets Manager)
   - 정기적인 키 로테이션

3. **개발/프로덕션 키 분리**
   - 개발과 프로덕션에서 다른 키 사용
   - 개발 키가 프로덕션에 노출되지 않도록 주의

---

## 문제 해결

### 환경 변수가 적용되지 않을 때
```bash
# 현재 값 확인
echo $DATABASE_URL

# Spring Boot에서 확인
curl http://localhost:8080/actuator/env/DATABASE_URL
```

### 특수문자 처리
비밀번호에 특수문자가 있을 경우:
```bash
# 따옴표로 감싸기
DATABASE_PASSWORD='p@ss!word#123'

# URL 인코딩 (JDBC URL 내)
DATABASE_URL=jdbc:postgresql://host/db?password=p%40ss%21word%23123
```
