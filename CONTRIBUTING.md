# Contributing to BEAM

Thank you for your interest in contributing to BEAM! This document provides guidelines and instructions for contributing.

## Branch Strategy

### Branch Structure

```
master              Production environment (auto-deploy)
  ↓
develop            Development/Staging environment
  ↓
feature/*          New feature development
fix/*              Bug fixes
hotfix/*           Production hotfixes
```

### Branch Naming Convention

#### Feature Branches
```bash
feature/user-authentication
feature/group-chat-rooms
feature/file-upload-optimization
feature/message-encryption
```

#### Fix Branches
```bash
fix/websocket-connection-timeout
fix/jwt-token-expiration
fix/database-migration-error
```

#### Hotfix Branches
```bash
hotfix/security-vulnerability-cve-2024-001
hotfix/database-connection-leak
hotfix/memory-overflow
```

## Workflow

### 1. Feature Development

```bash
# Start from develop
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/your-feature-name

# Work on your feature
git add .
git commit -m "feat: add amazing feature"

# Push to GitHub
git push -u origin feature/your-feature-name

# Create Pull Request: feature/* -> develop
```

### 2. Bug Fix

```bash
# Start from develop
git checkout develop
git pull origin develop

# Create fix branch
git checkout -b fix/bug-description

# Fix the bug
git add .
git commit -m "fix: resolve bug description"

# Push and create PR
git push -u origin fix/bug-description
```

### 3. Production Hotfix

```bash
# Start from master (critical bugs only)
git checkout master
git pull origin master

# Create hotfix branch
git checkout -b hotfix/critical-issue

# Fix the issue
git add .
git commit -m "hotfix: resolve critical security issue"

# Push and create PR to master
git push -u origin hotfix/critical-issue

# After merge to master, also merge to develop
git checkout develop
git merge master
git push origin develop
```

### 4. Release to Production

```bash
# Ensure develop is tested and stable
git checkout develop
git pull origin develop

# Create PR: develop -> master
# After review and approval, merge to master
# CI/CD will auto-deploy to production
```

## Commit Convention

We follow [Conventional Commits](https://www.conventionalcommits.org/) specification.

### Format
```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Code style changes (formatting, semicolons, etc.)
- **refactor**: Code refactoring (no functional changes)
- **perf**: Performance improvements
- **test**: Adding or updating tests
- **chore**: Maintenance tasks (dependencies, build, etc.)

### Examples

```bash
# Feature
git commit -m "feat: add WebSocket reconnection logic"

# Bug fix
git commit -m "fix: resolve JWT token expiration issue"

# Documentation
git commit -m "docs: update API documentation for authentication"

# Refactoring
git commit -m "refactor: optimize database query performance"

# Multiple lines
git commit -m "feat: implement message read receipts

- Add read_at timestamp to messages table
- Create MessageReadReceipt entity
- Update WebSocket handler to track reads
- Add API endpoint for fetching read status"
```

## Pull Request Guidelines

### Before Creating PR

1. **Update from base branch**
   ```bash
   git checkout develop
   git pull origin develop
   git checkout your-branch
   git rebase develop
   ```

2. **Run tests**
   ```bash
   mvn clean test
   ```

3. **Check code style**
   ```bash
   mvn checkstyle:check
   ```

### PR Requirements

- ✅ Descriptive title following commit convention
- ✅ Clear description of changes
- ✅ Link to related issue (if applicable)
- ✅ All tests passing
- ✅ No merge conflicts
- ✅ At least 1 reviewer approval
- ✅ Updated documentation (if needed)

### PR Template

```markdown
## Description
Brief description of what this PR does.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Changes Made
- Change 1
- Change 2
- Change 3

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Manual testing completed

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] No new warnings generated
```

## Code Review Process

### For Authors

1. Create PR with clear description
2. Request review from team members
3. Address all review comments
4. Update PR based on feedback
5. Wait for approval before merging

### For Reviewers

1. Review code for:
   - Correctness
   - Performance
   - Security
   - Code style
   - Test coverage
2. Leave constructive comments
3. Approve if all looks good
4. Request changes if needed

## Development Setup

### Prerequisites
- Java 17+
- PostgreSQL 14+
- Maven 3.8+
- Git

### Local Setup

```bash
# Clone repository
git clone https://github.com/araeLaver/chat.git
cd chat

# Checkout develop branch
git checkout develop

# Configure database
createdb chatdb

# Copy and configure properties
cp src/main/resources/application.properties.example \
   src/main/resources/application.properties

# Build and run tests
mvn clean install

# Run application
mvn spring-boot:run
```

## Testing Guidelines

### Unit Tests
```java
@Test
public void testUserAuthentication() {
    // Arrange
    User user = new User("testuser", "password123");

    // Act
    boolean result = authService.authenticate(user);

    // Assert
    assertTrue(result);
}
```

### Integration Tests
```java
@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testLoginEndpoint() throws Exception {
        mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"test\",\"password\":\"pass\"}"))
            .andExpect(status().isOk());
    }
}
```

## Documentation

### Code Comments
- Add JavaDoc for public methods
- Explain complex logic
- Document assumptions
- Include examples for public APIs

### API Documentation
- Update Swagger/OpenAPI specs
- Include request/response examples
- Document error codes
- Specify authentication requirements

## Security

### Reporting Vulnerabilities
- **DO NOT** create public issues for security vulnerabilities
- Email: downkim@naver.com
- Include detailed description and reproduction steps
- We will respond within 48 hours

### Security Checklist
- [ ] No hardcoded credentials
- [ ] Input validation implemented
- [ ] SQL injection prevention
- [ ] XSS protection
- [ ] CSRF tokens used
- [ ] Sensitive data encrypted
- [ ] Dependencies up to date

## Getting Help

### Resources
- [README](README.md) - Project overview
- [API Documentation](docs/API.md) - API reference
- [Deployment Guide](docs/DEPLOYMENT.md) - Deployment instructions

### Contact
- **Developer**: KIM DOWN
- **Email**: downkim@naver.com
- **GitHub**: [@araeLaver](https://github.com/araeLaver)

## License

By contributing to BEAM, you agree that your contributions will be licensed under the MIT License.
