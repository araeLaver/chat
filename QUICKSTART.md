# Quick Start Guide

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL 14+ (or H2 for local development)

## Local Development

### 1. Clone Repository
```bash
git clone https://github.com/araeLaver/chat.git
cd chat
```

### 2. Configure Environment
```bash
# Copy example environment file
cp .env.example .env

# Edit with your values
nano .env
```

### 3. Build & Run

#### Option A: Maven Spring Boot Plugin
```bash
# Linux/Mac
export $(cat .env | xargs)
mvn spring-boot:run

# Windows (PowerShell)
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2])
    }
}
mvn spring-boot:run
```

#### Option B: JAR Build
```bash
# Build
mvn clean package -DskipTests

# Run
java -jar target/beam-server-1.0.0.jar
```

### 4. Verify
```
Web Browser: http://localhost:8080
Swagger API: http://localhost:8080/swagger-ui.html
Health Check: http://localhost:8080/actuator/health
```

---

## Docker Deployment

```bash
# Build image
docker build -t beam-server .

# Run with env file
docker run -p 8080:8080 --env-file .env beam-server

# Verify
curl http://localhost:8080/actuator/health
```

---

## Production Deployment

### Koyeb

1. **Connect GitHub Repository**
   - Koyeb Dashboard > Create Service
   - Select GitHub repository
   - Branch: `main`

2. **Configure Environment Variables**
   ```
   DATABASE_URL=your-postgresql-jdbc-url
   DATABASE_USERNAME=your-username
   DATABASE_PASSWORD=your-password
   JWT_SECRET=your-256-bit-secret-key
   CORS_ALLOWED_ORIGINS=https://your-domain.com
   SPRING_PROFILES_ACTIVE=prod
   PORT=8080
   ```

3. **Build Settings**
   - Build command: `mvn clean package -DskipTests`
   - Run command: `java -jar target/beam-server-1.0.0.jar`
   - Port: `8080`

4. **Deploy**

---

## Troubleshooting

### Maven Not Found
```bash
# Use Maven Wrapper
./mvnw spring-boot:run    # Linux/Mac
mvnw.cmd spring-boot:run  # Windows
```

### Port Already in Use
```bash
export PORT=8081
mvn spring-boot:run
```

### Database Connection Failed
```bash
# Verify environment variables
echo $DATABASE_URL
echo $DATABASE_PASSWORD
```

### Build Failed
```bash
mvn clean
rm -rf target/
mvn install
```

---

## Documentation

- [README.md](README.md) - Project overview
- [SECURITY.md](SECURITY.md) - Security guide
- [API Documentation](http://localhost:8080/swagger-ui.html) - REST API docs
