# 🔐 Security Guide for BEAM Messenger

## ⚠️ CRITICAL: Git History Contains Exposed Credentials

**As of the latest security audit, database credentials were found hardcoded in Git history.**

### Immediate Actions Required:

1. **Change Database Password IMMEDIATELY**
   - The exposed password: `TRQuyavq9W5B` in `application-dev.properties`
   - Log into your PostgreSQL instance and change the password
   - Update your environment variables with the new password

2. **Clean Git History** (see instructions below)

---

## 🧹 Removing Sensitive Data from Git History

### Option 1: BFG Repo-Cleaner (Recommended)

BFG is faster and easier than `git filter-branch`:

```bash
# 1. Install BFG
# Download from: https://rtyley.github.io/bfg-repo-cleaner/
# Or install via Homebrew: brew install bfg

# 2. Create a backup
cd ..
git clone --mirror https://github.com/araeLaver/simple-chat-server.git simple-chat-server-backup

# 3. Clean the repository
cd simple-chat-server
bfg --replace-text passwords.txt

# Create passwords.txt with:
# TRQuyavq9W5B

# 4. Expire and prune
git reflog expire --expire=now --all
git gc --prune=now --aggressive

# 5. Force push (WARNING: This rewrites history!)
git push --force
```

### Option 2: Git Filter-Branch

```bash
# Remove file from all commits
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch src/main/resources/application-dev.properties" \
  --prune-empty --tag-name-filter cat -- --all

# Force push
git push origin --force --all
git push origin --force --tags
```

### Option 3: Start Fresh (Nuclear Option)

If the repository is new and has minimal history:

```bash
# 1. Backup current code
cp -r simple-chat-server simple-chat-server-backup

# 2. Delete .git directory
rm -rf .git

# 3. Initialize new repository
git init
git add .
git commit -m "Initial commit - Cleaned sensitive data"

# 4. Force push to GitHub
git remote add origin https://github.com/araeLaver/simple-chat-server.git
git push -u --force origin main
```

---

## ✅ Security Checklist for Production

### Before Deployment:

- [ ] All sensitive data moved to environment variables
- [ ] `.env` file exists locally but NOT in Git (check `.gitignore`)
- [ ] Database password changed from exposed value
- [ ] Git history cleaned of sensitive data
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

# Or use online: https://generate-random.org/api-key-generator
```

---

## 🔒 Environment Variables Setup

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
DATABASE_URL=jdbc:postgresql://...
DATABASE_USERNAME=username
DATABASE_PASSWORD=new-secure-password
JWT_SECRET=your-256-bit-secret
CORS_ALLOWED_ORIGINS=https://beam.chat,https://www.beam.chat
SPRING_PROFILES_ACTIVE=prod
```

#### Docker:
```bash
docker run -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://..." \
  -e DATABASE_USERNAME="username" \
  -e DATABASE_PASSWORD="new-secure-password" \
  -e JWT_SECRET="your-256-bit-secret" \
  -e CORS_ALLOWED_ORIGINS="https://beam.chat" \
  -e SPRING_PROFILES_ACTIVE=prod \
  beam-server
```

#### AWS/Heroku/Other:
Follow platform-specific environment variable configuration.

---

## 🚨 Security Incident Response

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

## 📋 Security Best Practices

### Code:
- ✅ Never hardcode credentials
- ✅ Use parameterized queries (prevent SQL injection)
- ✅ Validate all user input
- ✅ Use HTTPS/WSS in production
- ✅ Implement rate limiting
- ✅ Keep dependencies updated

### Configuration:
- ✅ Use environment variables for secrets
- ✅ Restrict CORS to known domains
- ✅ Enable CSRF protection
- ✅ Configure Content Security Policy (CSP)
- ✅ Use secure session cookies

### Database:
- ✅ Use SSL connections
- ✅ Principle of least privilege (limited user permissions)
- ✅ Regular backups
- ✅ Encrypt sensitive data at rest

### Deployment:
- ✅ Use secrets management (AWS Secrets Manager, HashiCorp Vault)
- ✅ Enable audit logging
- ✅ Monitor for security events
- ✅ Regular security updates

---

## 🐛 Reporting Security Vulnerabilities

**DO NOT open public GitHub issues for security vulnerabilities!**

Instead:
1. Email: security@beam.chat (if available)
2. Or open a private security advisory on GitHub
3. Or contact maintainers directly

We will respond within 48 hours and provide a fix within 7 days for critical issues.

---

## 📚 Additional Resources

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [PostgreSQL Security Best Practices](https://www.postgresql.org/docs/current/security.html)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)

---

**Remember: Security is an ongoing process, not a one-time task. Stay vigilant!**
