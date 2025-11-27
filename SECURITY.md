# Security Guide for BEAM Messenger

## Security Checklist for Production

### Before Deployment:

- [ ] All sensitive data moved to environment variables
- [ ] `.env` file exists locally but NOT in Git (check `.gitignore`)
- [ ] Database password is strong and unique
- [ ] `JWT_SECRET` set to strong 256-bit key
- [ ] `CORS_ALLOWED_ORIGINS` set to actual production domain
- [ ] HTTPS/WSS enabled (no HTTP in production)
- [ ] Database connections use SSL (`sslmode=require`)
- [ ] Rate limiting configured
- [ ] Actuator endpoints protected (not public)
- [ ] Spring Security configured for production

### Generate Secure JWT Secret:

```bash
# Generate 256-bit secret (Linux/Mac)
openssl rand -base64 64

# Or use Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
```

---

## Environment Variables Setup

### Development (.env file):

```bash
# Copy .env.example to .env
cp .env.example .env

# Edit .env with your values
nano .env

# DO NOT commit .env to Git!
```

### Production (Platform-specific):

#### Koyeb:
```bash
# Set via Koyeb Dashboard > Service > Environment Variables
DATABASE_URL=jdbc:postgresql://your-host/your-db?sslmode=require
DATABASE_USERNAME=your-username
DATABASE_PASSWORD=your-secure-password
JWT_SECRET=your-256-bit-secret
CORS_ALLOWED_ORIGINS=https://your-domain.com
SPRING_PROFILES_ACTIVE=prod
```

#### Docker:
```bash
docker run -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://..." \
  -e DATABASE_USERNAME="username" \
  -e DATABASE_PASSWORD="password" \
  -e JWT_SECRET="your-256-bit-secret" \
  -e CORS_ALLOWED_ORIGINS="https://your-domain.com" \
  -e SPRING_PROFILES_ACTIVE=prod \
  beam-server
```

---

## Security Incident Response

If you suspect a security breach:

1. **Immediately revoke exposed credentials**
   - Change database passwords
   - Rotate JWT secrets
   - Invalidate all active sessions

2. **Assess the impact**
   - Check database access logs
   - Review application logs for suspicious activity
   - Identify affected users

3. **Notify stakeholders**
   - Security team
   - Affected users (if personal data compromised)
   - Compliance team (GDPR, etc.)

4. **Document the incident**
   - What was exposed
   - When it was exposed
   - How it was discovered
   - Actions taken

---

## Security Best Practices

### Code:
- Never hardcode credentials
- Use parameterized queries (prevent SQL injection)
- Validate all user input
- Use HTTPS/WSS in production
- Implement rate limiting
- Keep dependencies updated

### Configuration:
- Use environment variables for secrets
- Restrict CORS to known domains
- Enable CSRF protection
- Configure Content Security Policy (CSP)
- Use secure session cookies

### Database:
- Use SSL connections
- Principle of least privilege (limited user permissions)
- Regular backups
- Encrypt sensitive data at rest

### Deployment:
- Use secrets management (AWS Secrets Manager, HashiCorp Vault)
- Enable audit logging
- Monitor for security events
- Regular security updates

---

## Reporting Security Vulnerabilities

**DO NOT open public GitHub issues for security vulnerabilities!**

Instead:
1. Open a private security advisory on GitHub
2. Or contact maintainers directly

We will respond within 48 hours and provide a fix within 7 days for critical issues.

---

## Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [PostgreSQL Security Best Practices](https://www.postgresql.org/docs/current/security.html)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

---

**Remember: Security is an ongoing process, not a one-time task. Stay vigilant!**
