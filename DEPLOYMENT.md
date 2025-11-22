# 🚀 배포 가이드

## 개요

BEAM 메신저를 프로덕션 환경에 배포하는 완벽한 가이드입니다.

---

## 📋 배포 전 체크리스트

### 필수 환경변수 준비

```bash
✅ DATABASE_URL          # PostgreSQL 연결 URL
✅ DATABASE_USERNAME     # DB 사용자명
✅ DATABASE_PASSWORD     # DB 패스워드
✅ JWT_SECRET           # 256-bit JWT 시크릿
✅ CORS_ALLOWED_ORIGINS # 허용할 도메인 (쉼표 구분)
✅ SPRING_PROFILES_ACTIVE=prod
```

### 보안 체크

- [ ] JWT_SECRET이 강력한 256-bit 키인지 확인
- [ ] CORS_ALLOWED_ORIGINS가 실제 도메인으로 설정되었는지 확인
- [ ] DATABASE_PASSWORD가 환경변수로 설정되었는지 확인
- [ ] HTTPS/WSS 인증서 준비 (프로덕션)
- [ ] Git에 민감 정보가 없는지 최종 확인

---

## 🐳 Docker 배포

### 1. 로컬 Docker 테스트

```bash
# 1. 이미지 빌드
docker build -t beam-server:latest .

# 2. 로컬 실행 (.env 파일 사용)
docker run -p 8080:8080 --env-file .env beam-server:latest

# 3. 테스트
curl http://localhost:8080/actuator/health
```

### 2. Docker Hub 배포

```bash
# 1. Docker Hub 로그인
docker login

# 2. 태그 추가
docker tag beam-server:latest your-username/beam-server:latest
docker tag beam-server:latest your-username/beam-server:1.0.0

# 3. 푸시
docker push your-username/beam-server:latest
docker push your-username/beam-server:1.0.0

# 4. 서버에서 실행
docker pull your-username/beam-server:latest
docker run -d -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://..." \
  -e DATABASE_USERNAME="username" \
  -e DATABASE_PASSWORD="password" \
  -e JWT_SECRET="your-secret" \
  -e CORS_ALLOWED_ORIGINS="https://beam.chat" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  --name beam-server \
  your-username/beam-server:latest
```

---

## ☁️ Koyeb 배포 (추천)

Koyeb은 Git 연동으로 자동 배포를 지원하는 PaaS입니다.

### 방법 1: GitHub 연동 (자동 배포)

1. **Koyeb 대시보드** 접속
   - https://app.koyeb.com

2. **서비스 생성**
   - `Create Service` 클릭
   - GitHub repository 선택: `araeLaver/simple-chat-server`
   - Branch: `main`

3. **빌드 설정**
   ```
   Builder: Dockerfile
   Dockerfile path: Dockerfile
   ```

4. **환경변수 설정**
   ```
   DATABASE_URL=jdbc:postgresql://ep-blue-unit-a2ev3s9x.eu-central-1.pg.koyeb.app/koyebdb?currentSchema=chatapp_prod&sslmode=require
   DATABASE_USERNAME=koyeb-adm
   DATABASE_PASSWORD=TRQuyavq9W5B
   JWT_SECRET=Yc5SfNZegvtvNJaLyvNtzoXwUyKi+MHhG4tv75N7PYKnKDWdFLLHaqFnrDNNHaRnxjlVEHFrKpK1KHJ2ZK+qNA==
   CORS_ALLOWED_ORIGINS=https://your-app.koyeb.app
   SPRING_PROFILES_ACTIVE=prod
   ```

5. **인스턴스 설정**
   - Region: Frankfurt (eu-west) 또는 가까운 지역
   - Instance type: Nano (512MB RAM) 또는 Micro (1GB RAM)
   - Port: 8080
   - Health check: `/actuator/health`

6. **배포**
   - `Deploy` 버튼 클릭
   - 자동으로 빌드 및 배포 시작
   - 완료 후 Public URL 제공

### 방법 2: Docker Image 배포

```bash
# 1. 로컬에서 이미지 빌드
docker build -t beam-server:prod .

# 2. Docker Hub 푸시
docker tag beam-server:prod your-username/beam-server:prod
docker push your-username/beam-server:prod

# 3. Koyeb에서 Docker 이미지 선택
# - Docker Registry: Docker Hub
# - Image: your-username/beam-server:prod
# - 환경변수 설정 (위와 동일)
```

### Koyeb CLI 사용

```bash
# 1. Koyeb CLI 설치
curl -fsSL https://raw.githubusercontent.com/koyeb/koyeb-cli/master/install.sh | bash

# 2. 로그인
koyeb login

# 3. 서비스 생성
koyeb service create beam-server \
  --git github.com/araeLaver/simple-chat-server \
  --git-branch main \
  --ports 8080:http \
  --routes /:8080 \
  --env DATABASE_URL="jdbc:postgresql://..." \
  --env DATABASE_USERNAME="koyeb-adm" \
  --env DATABASE_PASSWORD="TRQuyavq9W5B" \
  --env JWT_SECRET="Yc5SfNZegvtvNJaLyvNtzoXwUyKi+MHhG4tv75N7PYKnKDWdFLLHaqFnrDNNHaRnxjlVEHFrKpK1KHJ2ZK+qNA==" \
  --env CORS_ALLOWED_ORIGINS="https://your-app.koyeb.app" \
  --env SPRING_PROFILES_ACTIVE="prod" \
  --instance-type nano \
  --regions fra
```

---

## 🌩️ AWS 배포

### AWS Elastic Beanstalk

```bash
# 1. EB CLI 설치
pip install awsebcli

# 2. EB 초기화
eb init -p docker beam-server --region us-east-1

# 3. 환경 생성
eb create beam-production

# 4. 환경변수 설정
eb setenv \
  DATABASE_URL="jdbc:postgresql://..." \
  DATABASE_USERNAME="username" \
  DATABASE_PASSWORD="password" \
  JWT_SECRET="your-secret" \
  CORS_ALLOWED_ORIGINS="https://beam.example.com" \
  SPRING_PROFILES_ACTIVE="prod"

# 5. 배포
eb deploy

# 6. 상태 확인
eb status
eb open
```

### AWS ECS (Fargate)

1. **ECR에 이미지 푸시**
   ```bash
   aws ecr create-repository --repository-name beam-server
   docker tag beam-server:latest <account-id>.dkr.ecr.<region>.amazonaws.com/beam-server:latest
   aws ecr get-login-password | docker login --username AWS --password-stdin <account-id>.dkr.ecr.<region>.amazonaws.com
   docker push <account-id>.dkr.ecr.<region>.amazonaws.com/beam-server:latest
   ```

2. **ECS 태스크 정의 생성** (AWS Console)
   - Container: beam-server
   - Image: ECR URI
   - Port: 8080
   - Environment Variables: 위 환경변수 추가

3. **서비스 생성**
   - Cluster 생성
   - Service 생성 (Fargate)
   - Load Balancer 설정
   - Auto Scaling 설정

---

## 🎯 Heroku 배포

```bash
# 1. Heroku CLI 설치
# https://devcenter.heroku.com/articles/heroku-cli

# 2. 로그인
heroku login

# 3. 앱 생성
heroku create beam-server

# 4. 환경변수 설정
heroku config:set \
  DATABASE_URL="jdbc:postgresql://..." \
  DATABASE_USERNAME="username" \
  DATABASE_PASSWORD="password" \
  JWT_SECRET="your-secret" \
  CORS_ALLOWED_ORIGINS="https://beam-server.herokuapp.com" \
  SPRING_PROFILES_ACTIVE="prod"

# 5. 배포
git push heroku main

# 6. 확인
heroku open
heroku logs --tail
```

---

## 🔧 Render 배포

1. **Render 대시보드** 접속
   - https://dashboard.render.com

2. **새 Web Service 생성**
   - Connect GitHub repository: `araeLaver/simple-chat-server`
   - Branch: `main`

3. **설정**
   ```
   Name: beam-server
   Environment: Docker
   Region: Frankfurt
   Instance Type: Starter ($7/month)
   ```

4. **환경변수 추가**
   ```
   DATABASE_URL=jdbc:postgresql://...
   DATABASE_USERNAME=username
   DATABASE_PASSWORD=password
   JWT_SECRET=your-secret
   CORS_ALLOWED_ORIGINS=https://beam-server.onrender.com
   SPRING_PROFILES_ACTIVE=prod
   ```

5. **Deploy**
   - `Create Web Service` 클릭
   - 자동 배포 시작

---

## 📊 배포 후 확인

### Health Check

```bash
# 서버 상태 확인
curl https://your-domain.com/actuator/health

# 예상 응답
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### API 테스트

```bash
# Swagger UI 접속
https://your-domain.com/swagger-ui.html

# 회원가입 테스트
curl -X POST https://your-domain.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "testpass123",
    "displayName": "Test User",
    "phoneNumber": "01012345678"
  }'
```

### 로그 모니터링

```bash
# Koyeb
koyeb logs beam-server --follow

# Heroku
heroku logs --tail

# AWS
eb logs --follow

# Docker
docker logs -f beam-server
```

---

## 🔍 문제 해결

### 데이터베이스 연결 실패

```bash
# PostgreSQL 연결 테스트
psql -h ep-blue-unit-a2ev3s9x.eu-central-1.pg.koyeb.app -U koyeb-adm -d koyebdb

# 환경변수 확인
echo $DATABASE_URL
```

### 메모리 부족

```bash
# JVM 메모리 설정 (Dockerfile 또는 환경변수)
JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC"
```

### CORS 에러

```bash
# CORS_ALLOWED_ORIGINS 확인
# 프론트엔드 도메인이 정확히 포함되어 있는지 확인
CORS_ALLOWED_ORIGINS=https://beam.chat,https://www.beam.chat
```

### 포트 충돌

```bash
# 환경변수로 포트 변경
PORT=8081
```

---

## 📈 모니터링 & 스케일링

### Actuator 메트릭

```bash
# Prometheus 메트릭
curl https://your-domain.com/actuator/prometheus

# 애플리케이션 정보
curl https://your-domain.com/actuator/info
```

### Auto Scaling (Koyeb)

- Dashboard > Service > Autoscaling
- Min instances: 1
- Max instances: 5
- Target CPU: 70%
- Target Memory: 80%

---

## 🔒 프로덕션 보안 체크리스트

- [ ] HTTPS/WSS 활성화
- [ ] JWT_SECRET 강력한 키 사용
- [ ] CORS 실제 도메인만 허용
- [ ] 데이터베이스 SSL 연결
- [ ] 환경변수로 민감 정보 관리
- [ ] Rate Limiting 활성화
- [ ] Actuator 엔드포인트 보호
- [ ] 정기적인 보안 업데이트
- [ ] 로그 모니터링
- [ ] 백업 자동화

---

## 📚 추가 리소스

- [Koyeb Documentation](https://www.koyeb.com/docs)
- [AWS Documentation](https://docs.aws.amazon.com/)
- [Heroku Documentation](https://devcenter.heroku.com/)
- [Docker Documentation](https://docs.docker.com/)
- [Spring Boot Deployment](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)

---

**🎉 배포 완료! BEAM 메신저가 이제 전 세계에서 사용 가능합니다!**
