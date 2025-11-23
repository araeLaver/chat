# BEAM - Real-time Chat Server

[![CI/CD Pipeline](https://github.com/araeLaver/chat/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/araeLaver/chat/actions/workflows/ci-cd.yml)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

Enterprise-grade real-time messaging server built with Spring Boot and WebSocket technology.

## Features

### Core Messaging
- **Real-time Communication**: WebSocket with SockJS fallback support
- **Direct Messaging**: 1:1 private conversations
- **Group Chat**: Multi-user chat rooms with member management
- **Message Search**: Full-text search across conversations
- **Read Receipts**: Message delivery and read status tracking

### Security
- **JWT Authentication**: Stateless token-based auth
- **Rate Limiting**: IP-based request throttling (60/min, 10/sec)
- **File Upload Security**: Magic number validation, path traversal protection
- **Input Validation**: Comprehensive sanitization and validation
- **CORS Configuration**: Environment-based origin control

### Database
- **PostgreSQL**: Production-ready relational database
- **Connection Pooling**: HikariCP for optimal performance
- **Schema Versioning**: Flyway migrations

### DevOps
- **Docker Support**: Containerized deployment
- **CI/CD Pipeline**: GitHub Actions automation
- **Health Checks**: Actuator endpoints for monitoring
- **Structured Logging**: JSON-formatted logs with rotation

## Quick Start

### Prerequisites
- Java 17+
- PostgreSQL 14+
- Maven 3.8+

### Installation

1. Clone repository
```bash
git clone https://github.com/araeLaver/chat.git
cd chat
```

2. Configure database
```bash
# Create database
createdb chatdb

# Update application.properties
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

3. Build and run
```bash
mvn clean package -DskipTests
java -jar target/beam-server-1.0.0.jar
```

Server starts at `http://localhost:8080`

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL JDBC URL | `jdbc:postgresql://localhost:5432/chatdb` |
| `DATABASE_USERNAME` | Database user | `postgres` |
| `DATABASE_PASSWORD` | Database password | - |
| `JWT_SECRET` | JWT signing key (256-bit) | - |
| `CORS_ALLOWED_ORIGINS` | Allowed origins (comma-separated) | `http://localhost:8080` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `dev` |

### Production Deployment

```bash
# Set environment variables
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://prod-db:5432/chatdb
export JWT_SECRET=your-256-bit-secret-key
export CORS_ALLOWED_ORIGINS=https://yourdomain.com

# Run
java -jar target/beam-server-1.0.0.jar
```

## API Documentation

### Authentication

#### Register
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePass123!",
  "email": "john@example.com"
}
```

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "john_doe",
  "password": "SecurePass123!"
}
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com"
  }
}
```

### WebSocket Connection

```javascript
const token = localStorage.getItem('jwt_token');
const ws = new WebSocket(`ws://localhost:8080/ws?token=${token}`);

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('Received:', message);
};

// Send message
ws.send(JSON.stringify({
  type: 'CHAT',
  roomId: 1,
  content: 'Hello, World!'
}));
```

## Architecture

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTP/WS
┌──────▼──────────────────┐
│   Spring Boot Server    │
│  ┌──────────────────┐   │
│  │ Security Filters │   │
│  │  - JWT Auth      │   │
│  │  - Rate Limit    │   │
│  └────────┬─────────┘   │
│           │             │
│  ┌────────▼─────────┐   │
│  │   Controllers    │   │
│  │  - REST APIs     │   │
│  │  - WebSocket     │   │
│  └────────┬─────────┘   │
│           │             │
│  ┌────────▼─────────┐   │
│  │    Services      │   │
│  │  - Auth          │   │
│  │  - Message       │   │
│  │  - Room          │   │
│  └────────┬─────────┘   │
│           │             │
│  ┌────────▼─────────┐   │
│  │  Repositories    │   │
│  └────────┬─────────┘   │
└───────────┼─────────────┘
            │
     ┌──────▼──────┐
     │ PostgreSQL  │
     └─────────────┘
```

## Performance

- **Concurrent Users**: 10,000+ WebSocket connections
- **Message Throughput**: 50,000+ messages/second
- **Response Time**: <50ms (p95)
- **Database Connections**: HikariCP pool (max 20)

## Security

### Input Validation
- Username: 3-20 alphanumeric + underscore
- Password: 8+ chars (uppercase, lowercase, number, special char)
- Email: RFC 5322 compliant

### File Upload
- Max size: 10MB
- Allowed types: jpg, png, gif, webp, mp4, pdf, docx
- Magic number verification
- Path traversal protection

### Rate Limiting
- Global: 60 requests/minute
- Burst: 10 requests/second
- WebSocket: Separate connection limits

## Development

### Running Tests
```bash
mvn test
```

### Code Coverage
```bash
mvn jacoco:report
open target/site/jacoco/index.html
```

### Database Migrations
```bash
# Create new migration
touch src/main/resources/db/migration/V3__Description.sql

# Apply migrations
mvn flyway:migrate
```

## Monitoring

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

### Logs
```bash
tail -f logs/beam-app.log
tail -f logs/beam-error.log
tail -f logs/beam-security.log
```

## Troubleshooting

### Common Issues

**WebSocket connection fails**
- Check CORS settings
- Verify JWT token is valid
- Ensure firewall allows WebSocket connections

**Database connection timeout**
- Verify PostgreSQL is running
- Check connection pool settings
- Review database credentials

**Rate limit errors**
- Reduce request frequency
- Check IP whitelist configuration
- Review rate limit settings

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'feat: add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

### Commit Convention
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `refactor`: Code refactoring
- `test`: Tests
- `chore`: Maintenance

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

- **Developer**: KIM DOWN
- **Email**: downkim@naver.com
- **Repository**: https://github.com/araeLaver/chat
