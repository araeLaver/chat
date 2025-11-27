# Deployment Guide

## Overview

Complete guide for deploying BEAM Messenger to production environments.

---

## Pre-Deployment Checklist

### Required Environment Variables

```bash
DATABASE_URL          # PostgreSQL connection URL
DATABASE_USERNAME     # Database username
DATABASE_PASSWORD     # Database password
JWT_SECRET           # 256-bit JWT signing key
CORS_ALLOWED_ORIGINS # Allowed domains (comma-separated)
SPRING_PROFILES_ACTIVE=prod
```

### Security Check

- [ ] JWT_SECRET is a strong 256-bit key
- [ ] CORS_ALLOWED_ORIGINS is set to actual domain
- [ ] DATABASE_PASSWORD is set via environment variable
- [ ] HTTPS/WSS certificate prepared (production)
- [ ] No sensitive data in Git

---

## Docker Deployment

### 1. Local Docker Test

```bash
# Build image
docker build -t beam-server:latest .

# Run with env file
docker run -p 8080:8080 --env-file .env beam-server:latest

# Verify
curl http://localhost:8080/actuator/health
```

### 2. Docker Hub Deployment

```bash
# Login to Docker Hub
docker login

# Tag and push
docker tag beam-server:latest your-username/beam-server:latest
docker push your-username/beam-server:latest

# Run on server
docker run -d -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://..." \
  -e DATABASE_USERNAME="username" \
  -e DATABASE_PASSWORD="password" \
  -e JWT_SECRET="your-secret" \
  -e CORS_ALLOWED_ORIGINS="https://your-domain.com" \
  -e SPRING_PROFILES_ACTIVE="prod" \
  --name beam-server \
  your-username/beam-server:latest
```

---

## Koyeb Deployment (Recommended)

### Method 1: GitHub Integration

1. **Koyeb Dashboard**
   - https://app.koyeb.com

2. **Create Service**
   - `Create Service` > Select GitHub repository
   - Branch: `main`

3. **Build Settings**
   ```
   Builder: Dockerfile
   Dockerfile path: Dockerfile
   ```

4. **Environment Variables**
   ```
   DATABASE_URL=jdbc:postgresql://your-host/your-db?currentSchema=chat&sslmode=require
   DATABASE_USERNAME=your-username
   DATABASE_PASSWORD=your-password
   JWT_SECRET=your-256-bit-secret
   CORS_ALLOWED_ORIGINS=https://your-app.koyeb.app
   SPRING_PROFILES_ACTIVE=prod
   ```

5. **Instance Settings**
   - Region: Frankfurt (eu-west) or nearest
   - Instance type: Nano (512MB) or Micro (1GB)
   - Port: 8080
   - Health check: `/actuator/health`

6. **Deploy**

---

## AWS Deployment

### AWS Elastic Beanstalk

```bash
# Install EB CLI
pip install awsebcli

# Initialize
eb init -p docker beam-server --region us-east-1

# Create environment
eb create beam-production

# Set environment variables
eb setenv \
  DATABASE_URL="jdbc:postgresql://..." \
  DATABASE_USERNAME="username" \
  DATABASE_PASSWORD="password" \
  JWT_SECRET="your-secret" \
  CORS_ALLOWED_ORIGINS="https://your-domain.com" \
  SPRING_PROFILES_ACTIVE="prod"

# Deploy
eb deploy
```

---

## Heroku Deployment

```bash
# Install Heroku CLI
# https://devcenter.heroku.com/articles/heroku-cli

# Login
heroku login

# Create app
heroku create beam-server

# Set environment variables
heroku config:set \
  DATABASE_URL="jdbc:postgresql://..." \
  DATABASE_USERNAME="username" \
  DATABASE_PASSWORD="password" \
  JWT_SECRET="your-secret" \
  CORS_ALLOWED_ORIGINS="https://beam-server.herokuapp.com" \
  SPRING_PROFILES_ACTIVE="prod"

# Deploy
git push heroku main

# Verify
heroku open
heroku logs --tail
```

---

## Post-Deployment Verification

### Health Check

```bash
curl https://your-domain.com/actuator/health

# Expected response
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping": { "status": "UP" }
  }
}
```

### API Test

```bash
# Swagger UI
https://your-domain.com/swagger-ui.html

# Registration test
curl -X POST https://your-domain.com/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "testpass123",
    "displayName": "Test User"
  }'
```

### Log Monitoring

```bash
# Koyeb
koyeb logs beam-server --follow

# Heroku
heroku logs --tail

# Docker
docker logs -f beam-server
```

---

## Troubleshooting

### Database Connection Failed
- Verify DATABASE_URL, USERNAME, PASSWORD
- Check SSL settings (sslmode=require)

### Memory Issues
```bash
JAVA_OPTS="-Xms128m -Xmx512m -XX:+UseG1GC"
```

### CORS Errors
- Verify CORS_ALLOWED_ORIGINS includes frontend domain

### Port Conflicts
```bash
PORT=8081
```

---

## Monitoring & Scaling

### Actuator Metrics

```bash
# Prometheus metrics
curl https://your-domain.com/actuator/prometheus

# Application info
curl https://your-domain.com/actuator/info
```

### Auto Scaling (Koyeb)

- Dashboard > Service > Autoscaling
- Min instances: 1
- Max instances: 5
- Target CPU: 70%
- Target Memory: 80%

---

## Production Security Checklist

- [ ] HTTPS/WSS enabled
- [ ] Strong JWT_SECRET
- [ ] CORS restricted to actual domains
- [ ] Database SSL connection
- [ ] Environment variables for secrets
- [ ] Rate limiting enabled
- [ ] Actuator endpoints protected
- [ ] Regular security updates
- [ ] Log monitoring
- [ ] Backup automation

---

## Additional Resources

- [Koyeb Documentation](https://www.koyeb.com/docs)
- [AWS Documentation](https://docs.aws.amazon.com/)
- [Heroku Documentation](https://devcenter.heroku.com/)
- [Docker Documentation](https://docs.docker.com/)
- [Spring Boot Deployment](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)
