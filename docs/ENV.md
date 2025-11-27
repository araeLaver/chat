# Environment Variables Guide

## Required Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DATABASE_URL` | Yes | - | PostgreSQL JDBC URL |
| `DATABASE_USERNAME` | Yes | - | Database username |
| `DATABASE_PASSWORD` | Yes | - | Database password |
| `JWT_SECRET` | Yes | - | JWT signing key (256-bit minimum) |
| `CORS_ALLOWED_ORIGINS` | Yes | localhost | Allowed domains (comma-separated) |
| `SPRING_PROFILES_ACTIVE` | Yes | dev | Profile (dev/prod) |
| `PORT` | No | 8080 | Server port |

## Optional Variables (Email)

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `MAIL_HOST` | No | smtp.gmail.com | SMTP server |
| `MAIL_PORT` | No | 587 | SMTP port |
| `MAIL_USERNAME` | No | - | Email account |
| `MAIL_PASSWORD` | No | - | Email password/app password |

---

## Environment Examples

### Development
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/chatdb?currentSchema=chat
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your-password
JWT_SECRET=dev-secret-key-for-local-development-only
CORS_ALLOWED_ORIGINS=http://localhost:8080,http://localhost:3000
SPRING_PROFILES_ACTIVE=dev
```

### Production
```bash
DATABASE_URL=jdbc:postgresql://your-host/your-db?currentSchema=chat&sslmode=require
DATABASE_USERNAME=your-username
DATABASE_PASSWORD=your-secure-password
JWT_SECRET=your-production-256-bit-secret-key
CORS_ALLOWED_ORIGINS=https://your-domain.com
SPRING_PROFILES_ACTIVE=prod
PORT=8080
```

---

## Variable Details

### DATABASE_URL
PostgreSQL JDBC connection URL

**Format:**
```
jdbc:postgresql://[host]:[port]/[database]?currentSchema=[schema]&sslmode=require
```

### JWT_SECRET
Secret key for JWT token signing

**Requirements:**
- Minimum 256 bits (32+ characters)
- Random, unpredictable string
- Never expose in production

**Generation:**
```bash
# OpenSSL
openssl rand -base64 64

# Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"

# Python
python -c "import secrets; print(secrets.token_urlsafe(64))"
```

### MAIL_HOST & MAIL_PORT

| Service | Host | Port |
|---------|------|------|
| Gmail | smtp.gmail.com | 587 |
| Naver | smtp.naver.com | 587 |
| AWS SES | email-smtp.{region}.amazonaws.com | 587 |

### CORS_ALLOWED_ORIGINS
Domains allowed for cross-origin requests

**Format:** Comma-separated
```bash
# Single domain
CORS_ALLOWED_ORIGINS=https://example.com

# Multiple domains
CORS_ALLOWED_ORIGINS=https://example.com,https://www.example.com
```

### SPRING_PROFILES_ACTIVE

| Profile | Purpose | Features |
|---------|---------|----------|
| `dev` | Development | SQL logging, detailed errors |
| `prod` | Production | Optimized, enhanced security |

---

## Application Methods

### System Environment Variables
```bash
# Linux/Mac
export DATABASE_URL="jdbc:postgresql://..."
export JWT_SECRET="your-secret"

# Windows (PowerShell)
$env:DATABASE_URL="jdbc:postgresql://..."
$env:JWT_SECRET="your-secret"
```

### .env File
```bash
cp .env.example .env
nano .env
export $(cat .env | xargs)
```

### Docker
```bash
docker run -e DATABASE_URL="..." -e JWT_SECRET="..." beam-server
docker run --env-file .env beam-server
```

---

## Security Notes

1. **Never commit to Git**
   - `.env` is in `.gitignore`
   - Never hardcode passwords in code

2. **Production key management**
   - Use secret management services (Vault, AWS Secrets Manager)
   - Regular key rotation

3. **Separate dev/prod keys**
   - Use different keys for development and production
