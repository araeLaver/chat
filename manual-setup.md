# PostgreSQL 수동 스키마 설정 가이드

## 1. PostgreSQL 연결 정보
```
Host: ep-blue-unit-a2ev3s9x.eu-central-1.pg.koyeb.app
Database: koyebdb
Username: koyeb-adm
Password: TRQuyavq9W5B
SSL: Required
```

## 2. 스키마 생성 (필요한 경우)
```sql
-- 스키마 생성
CREATE SCHEMA IF NOT EXISTS chatapp_dev;
CREATE SCHEMA IF NOT EXISTS chatapp_prod;

-- 권한 부여
GRANT ALL PRIVILEGES ON SCHEMA chatapp_dev TO "koyeb-adm";
GRANT ALL PRIVILEGES ON SCHEMA chatapp_prod TO "koyeb-adm";
```

## 3. Development 스키마 초기화
PostgreSQL 클라이언트(pgAdmin, DBeaver 등)에서 다음 파일 실행:
```
src/main/resources/db/init-chatapp-dev.sql
```

또는 psql 명령어:
```bash
psql -h ep-blue-unit-a2ev3s9x.eu-central-1.pg.koyeb.app -U koyeb-adm -d koyebdb -f src/main/resources/db/init-chatapp-dev.sql
```

## 4. Production 스키마 초기화
PostgreSQL 클라이언트에서 다음 파일 실행:
```
src/main/resources/db/init-chatapp-prod.sql
```

또는 psql 명령어:
```bash
psql -h ep-blue-unit-a2ev3s9x.eu-central-1.pg.koyeb.app -U koyeb-adm -d koyebdb -f src/main/resources/db/init-chatapp-prod.sql
```

## 5. 확인 방법
```sql
-- 개발 스키마 테이블 확인
SET search_path TO chatapp_dev;
\dt

-- 프로덕션 스키마 테이블 확인  
SET search_path TO chatapp_prod;
\dt

-- 또는 SQL로 확인
SELECT table_name FROM information_schema.tables 
WHERE table_schema IN ('chatapp_dev', 'chatapp_prod') 
ORDER BY table_schema, table_name;
```

## 6. 예상 테이블 목록
- chat_rooms
- messages  
- user_sessions
- users

## 7. 애플리케이션 실행
```bash
# 개발 환경 (chatapp_dev 스키마 사용)
mvn spring-boot:run -Pdev

# 프로덕션 환경 (chatapp_prod 스키마 사용)
mvn spring-boot:run -Pprod

# 로컬 테스트 (H2 인메모리 DB)
mvn spring-boot:run -Plocal
```

## 문제 해결
1. **권한 오류**: 스키마 권한 확인
2. **연결 오류**: SSL 설정 및 방화벽 확인
3. **테이블 중복**: 기존 테이블 삭제 후 재생성
4. **FK 오류**: 테이블 생성 순서 확인 (users → chat_rooms → messages → user_sessions)