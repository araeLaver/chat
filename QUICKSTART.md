# 🚀 빠른 시작 가이드

## ⚡ 5분 안에 실행하기

### 1️⃣ 환경 확인

.env 파일이 이미 생성되어 있습니다! 바로 실행 가능합니다.

```bash
# .env 파일 확인
cat .env

# 환경변수 로드 확인
export $(cat .env | xargs)
echo $DATABASE_URL  # 확인
```

### 2️⃣ Maven 설치 (없는 경우)

#### Windows:
```bash
# Chocolatey 사용
choco install maven

# 또는 수동 다운로드
# https://maven.apache.org/download.cgi
```

#### Mac:
```bash
brew install maven
```

#### Linux:
```bash
sudo apt-get install maven  # Ubuntu/Debian
sudo yum install maven      # CentOS/RHEL
```

### 3️⃣ 빌드 & 실행

#### 방법 A: Spring Boot Maven Plugin (권장)

```bash
# Windows (cmd)
set DATABASE_URL=jdbc:postgresql://ep-blue-unit-a2ev3s9x.eu-central-1.pg.koyeb.app/koyebdb?currentSchema=chatapp_dev^&sslmode=require
set DATABASE_USERNAME=koyeb-adm
set DATABASE_PASSWORD=TRQuyavq9W5B
set JWT_SECRET=Yc5SfNZegvtvNJaLyvNtzoXwUyKi+MHhG4tv75N7PYKnKDWdFLLHaqFnrDNNHaRnxjlVEHFrKpK1KHJ2ZK+qNA==
set CORS_ALLOWED_ORIGINS=http://localhost:8080,http://localhost:3000
set SPRING_PROFILES_ACTIVE=dev

mvn spring-boot:run

# Windows (PowerShell)
$env:DATABASE_URL="jdbc:postgresql://ep-blue-unit-a2ev3s9x.eu-central-1.pg.koyeb.app/koyebdb?currentSchema=chatapp_dev&sslmode=require"
$env:DATABASE_USERNAME="koyeb-adm"
$env:DATABASE_PASSWORD="TRQuyavq9W5B"
$env:JWT_SECRET="Yc5SfNZegvtvNJaLyvNtzoXwUyKi+MHhG4tv75N7PYKnKDWdFLLHaqFnrDNNHaRnxjlVEHFrKpK1KHJ2ZK+qNA=="
$env:CORS_ALLOWED_ORIGINS="http://localhost:8080,http://localhost:3000"
$env:SPRING_PROFILES_ACTIVE="dev"

mvn spring-boot:run

# Linux/Mac
export $(cat .env | xargs)
mvn spring-boot:run
```

#### 방법 B: JAR 파일 빌드

```bash
# 1. 빌드
mvn clean package -DskipTests

# 2. 실행 (Windows cmd)
java -jar target\simple-chat-server-1.0.0.jar ^
  --DATABASE_URL="jdbc:postgresql://ep-blue-unit-a2ev3s9x.eu-central-1.pg.koyeb.app/koyebdb?currentSchema=chatapp_dev&sslmode=require" ^
  --DATABASE_USERNAME=koyeb-adm ^
  --DATABASE_PASSWORD=TRQuyavq9W5B ^
  --JWT_SECRET=Yc5SfNZegvtvNJaLyvNtzoXwUyKi+MHhG4tv75N7PYKnKDWdFLLHaqFnrDNNHaRnxjlVEHFrKpK1KHJ2ZK+qNA== ^
  --CORS_ALLOWED_ORIGINS=http://localhost:8080,http://localhost:3000 ^
  --SPRING_PROFILES_ACTIVE=dev

# Linux/Mac
export $(cat .env | xargs)
java -jar target/simple-chat-server-1.0.0.jar
```

### 4️⃣ 접속 확인

서버가 시작되면:

```
📍 웹 브라우저: http://localhost:8080
📍 Swagger API: http://localhost:8080/swagger-ui.html
📍 Health Check: http://localhost:8080/actuator/health
```

---

## 🐳 Docker로 실행

```bash
# 1. 이미지 빌드
docker build -t beam-server .

# 2. 실행 (.env 파일 사용)
docker run -p 8080:8080 --env-file .env beam-server

# 또는 환경변수 직접 지정
docker run -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://ep-blue-unit-a2ev3s9x.eu-central-1.pg.koyeb.app/koyebdb?currentSchema=chatapp_dev&sslmode=require" \
  -e DATABASE_USERNAME="koyeb-adm" \
  -e DATABASE_PASSWORD="TRQuyavq9W5B" \
  -e JWT_SECRET="Yc5SfNZegvtvNJaLyvNtzoXwUyKi+MHhG4tv75N7PYKnKDWdFLLHaqFnrDNNHaRnxjlVEHFrKpK1KHJ2ZK+qNA==" \
  -e CORS_ALLOWED_ORIGINS="http://localhost:8080,http://localhost:3000" \
  -e SPRING_PROFILES_ACTIVE="dev" \
  beam-server

# 3. 접속
curl http://localhost:8080/actuator/health
```

---

## 🌐 프로덕션 배포

### Koyeb 배포 (추천)

1. **GitHub 연동**
   - Koyeb Dashboard > Create Service
   - GitHub repository 선택
   - Branch: `main`

2. **환경변수 설정**
   ```
   DATABASE_URL=jdbc:postgresql://ep-blue-unit-a2ev3s9x.eu-central-1.pg.koyeb.app/koyebdb?currentSchema=chatapp_prod&sslmode=require
   DATABASE_USERNAME=koyeb-adm
   DATABASE_PASSWORD=TRQuyavq9W5B
   JWT_SECRET=Yc5SfNZegvtvNJaLyvNtzoXwUyKi+MHhG4tv75N7PYKnKDWdFLLHaqFnrDNNHaRnxjlVEHFrKpK1KHJ2ZK+qNA==
   CORS_ALLOWED_ORIGINS=https://your-domain.com
   SPRING_PROFILES_ACTIVE=prod
   PORT=8080
   ```

3. **빌드 설정**
   - Build command: `mvn clean package -DskipTests`
   - Run command: `java -jar target/simple-chat-server-1.0.0.jar`
   - Port: `8080`

4. **배포**
   - Deploy 버튼 클릭
   - 자동 빌드 및 배포 시작

### AWS / Heroku / Render

플랫폼별 환경변수 설정 방법만 다르고, 위 환경변수를 동일하게 설정하면 됩니다.

---

## 🔍 문제 해결

### Maven 없음
```bash
# Maven Wrapper 사용
./mvnw spring-boot:run  # Linux/Mac
mvnw.cmd spring-boot:run  # Windows
```

### 포트 이미 사용 중
```bash
# 포트 변경
export PORT=8081
mvn spring-boot:run
```

### DB 연결 실패
```bash
# 환경변수 확인
echo $DATABASE_URL
echo $DATABASE_PASSWORD

# 네트워크 연결 확인
ping ep-blue-unit-a2ev3s9x.eu-central-1.pg.koyeb.app
```

### 빌드 실패
```bash
# 캐시 클리어
mvn clean
rm -rf target/
mvn install
```

---

## 📊 실행 확인

서버가 정상 실행되면 다음과 같은 로그가 출력됩니다:

```
  ____  _____    _    __  __
 | __ )| ____|  / \  |  \/  |
 |  _ \|  _|   / _ \ | |\/| |
 | |_) | |___ / ___ \| |  | |
 |____/|_____/_/   \_\_|  |_|

:: Spring Boot ::                (v3.2.0)

2025-01-11 14:00:00.000  INFO --- [main] com.beam.SimpleChatServerApplication
: Starting SimpleChatServerApplication
...
2025-01-11 14:00:05.000  INFO --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer
: Tomcat started on port(s): 8080 (http)
```

✅ **성공!** http://localhost:8080 접속 가능

---

## 📚 추가 문서

- [README.md](README.md) - 프로젝트 전체 개요
- [SECURITY.md](SECURITY.md) - 보안 가이드
- [API Documentation](http://localhost:8080/swagger-ui.html) - REST API 문서

---

**⚡ 이제 BEAM 메신저를 사용할 수 있습니다!**
