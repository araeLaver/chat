package com.beam;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class DatabaseSetup {
    
    private static final String DB_URL = "jdbc:postgresql://ep-blue-unit-a2ev3s9x.eu-central-1.pg.koyeb.app/koyebdb?sslmode=require";
    private static final String USERNAME = "koyeb-adm";
    private static final String PASSWORD = "TRQuyavq9W5B";
    
    public static void main(String[] args) {
        System.out.println("🚀 PostgreSQL 데이터베이스 초기화 시작...");
        
        try {
            // 드라이버 로드
            Class.forName("org.postgresql.Driver");
            
            // 데이터베이스 연결
            Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            Statement statement = connection.createStatement();
            
            // 1. 스키마 생성
            System.out.println("📁 스키마 생성 중...");
            statement.execute("CREATE SCHEMA IF NOT EXISTS chatapp_dev");
            statement.execute("CREATE SCHEMA IF NOT EXISTS chatapp_prod");
            System.out.println("✅ 스키마 생성 완료");
            
            // 2. chatapp_dev 스키마 초기화
            System.out.println("🔧 chatapp_dev 스키마 초기화 중...");
            setupDevSchema(statement);
            System.out.println("✅ chatapp_dev 초기화 완료");
            
            // 3. chatapp_prod 스키마 초기화
            System.out.println("🔧 chatapp_prod 스키마 초기화 중...");
            setupProdSchema(statement);
            System.out.println("✅ chatapp_prod 초기화 완료");
            
            // 4. 테이블 확인
            System.out.println("🔍 테이블 생성 확인 중...");
            verifyTables(statement);
            
            statement.close();
            connection.close();
            System.out.println("🎉 데이터베이스 초기화 완료!");
            
        } catch (Exception e) {
            System.err.println("❌ 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void setupDevSchema(Statement statement) throws SQLException {
        statement.execute("SET search_path TO chatapp_dev");
        
        // 기존 테이블 삭제
        statement.execute("DROP TABLE IF EXISTS user_sessions CASCADE");
        statement.execute("DROP TABLE IF EXISTS messages CASCADE");
        statement.execute("DROP TABLE IF EXISTS chat_rooms CASCADE");
        statement.execute("DROP TABLE IF EXISTS users CASCADE");
        statement.execute("DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE");
        
        // 테이블 생성
        createTables(statement);
        createInitialData(statement);
    }
    
    private static void setupProdSchema(Statement statement) throws SQLException {
        statement.execute("SET search_path TO chatapp_prod");
        
        // 기존 테이블 삭제
        statement.execute("DROP TABLE IF EXISTS user_sessions CASCADE");
        statement.execute("DROP TABLE IF EXISTS messages CASCADE");
        statement.execute("DROP TABLE IF EXISTS chat_rooms CASCADE");
        statement.execute("DROP TABLE IF EXISTS users CASCADE");
        statement.execute("DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE");
        
        // 테이블 생성
        createTables(statement);
        createInitialData(statement);
    }
    
    private static void createTables(Statement statement) throws SQLException {
        // Users Table
        statement.execute("""
            CREATE TABLE users (
                id BIGSERIAL PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                password VARCHAR(255) NOT NULL,
                email VARCHAR(255),
                last_login TIMESTAMP,
                is_active BOOLEAN NOT NULL DEFAULT true,
                profile_image VARCHAR(500),
                status_message VARCHAR(200),
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """);
        
        // Chat Rooms Table
        statement.execute("""
            CREATE TABLE chat_rooms (
                room_id VARCHAR(50) PRIMARY KEY,
                room_name VARCHAR(100) NOT NULL,
                room_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
                encryption_key VARCHAR(255),
                description VARCHAR(500),
                is_active BOOLEAN NOT NULL DEFAULT true,
                max_users INTEGER,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT chk_room_type CHECK (room_type IN ('NORMAL', 'SECRET', 'VOLATILE'))
            )
        """);
        
        // Messages Table
        statement.execute("""
            CREATE TABLE messages (
                id BIGSERIAL PRIMARY KEY,
                user_id BIGINT,
                sender VARCHAR(50) NOT NULL,
                content TEXT,
                room_id VARCHAR(50) NOT NULL,
                message_type VARCHAR(20) NOT NULL,
                timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                security_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
                expires_at TIMESTAMP,
                encryption_key VARCHAR(255),
                is_encrypted BOOLEAN NOT NULL DEFAULT false,
                file_url VARCHAR(500),
                file_name VARCHAR(255),
                file_size BIGINT,
                edited_at TIMESTAMP,
                is_deleted BOOLEAN NOT NULL DEFAULT false,
                CONSTRAINT chk_security_type CHECK (security_type IN ('NORMAL', 'SECRET', 'VOLATILE')),
                CONSTRAINT chk_message_type CHECK (message_type IN ('message', 'system', 'file', 'volatile')),
                CONSTRAINT fk_messages_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
                CONSTRAINT fk_messages_room_id FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id) ON DELETE CASCADE
            )
        """);
        
        // User Sessions Table
        statement.execute("""
            CREATE TABLE user_sessions (
                id BIGSERIAL PRIMARY KEY,
                session_id VARCHAR(100) NOT NULL,
                user_id BIGINT NOT NULL,
                username VARCHAR(50) NOT NULL,
                room_id VARCHAR(50),
                ip_address VARCHAR(45),
                user_agent VARCHAR(500),
                is_online BOOLEAN NOT NULL DEFAULT true,
                last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                connected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                disconnected_at TIMESTAMP,
                CONSTRAINT fk_sessions_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                CONSTRAINT fk_sessions_room_id FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id) ON DELETE SET NULL
            )
        """);
        
        // 인덱스 생성
        createIndexes(statement);
        
        // 트리거 함수 및 트리거 생성
        createTriggers(statement);
    }
    
    private static void createIndexes(Statement statement) throws SQLException {
        // Users 인덱스
        statement.execute("CREATE INDEX idx_users_username ON users(username)");
        statement.execute("CREATE INDEX idx_users_email ON users(email)");
        statement.execute("CREATE INDEX idx_users_is_active ON users(is_active)");
        statement.execute("CREATE INDEX idx_users_created_at ON users(created_at)");
        
        // Chat Rooms 인덱스
        statement.execute("CREATE INDEX idx_chat_rooms_room_type ON chat_rooms(room_type)");
        statement.execute("CREATE INDEX idx_chat_rooms_is_active ON chat_rooms(is_active)");
        
        // Messages 인덱스
        statement.execute("CREATE INDEX idx_messages_room_id ON messages(room_id)");
        statement.execute("CREATE INDEX idx_messages_sender ON messages(sender)");
        statement.execute("CREATE INDEX idx_messages_timestamp ON messages(timestamp)");
        statement.execute("CREATE INDEX idx_messages_user_id ON messages(user_id)");
        statement.execute("CREATE INDEX idx_messages_security_type ON messages(security_type)");
        statement.execute("CREATE INDEX idx_messages_expires_at ON messages(expires_at)");
        statement.execute("CREATE INDEX idx_messages_is_deleted ON messages(is_deleted)");
        
        // User Sessions 인덱스
        statement.execute("CREATE INDEX idx_sessions_session_id ON user_sessions(session_id)");
        statement.execute("CREATE INDEX idx_sessions_user_id ON user_sessions(user_id)");
        statement.execute("CREATE INDEX idx_sessions_room_id ON user_sessions(room_id)");
        statement.execute("CREATE INDEX idx_sessions_is_online ON user_sessions(is_online)");
        statement.execute("CREATE INDEX idx_sessions_last_activity ON user_sessions(last_activity)");
    }
    
    private static void createTriggers(Statement statement) throws SQLException {
        // 트리거 함수 생성
        statement.execute("""
            CREATE OR REPLACE FUNCTION update_updated_at_column()
            RETURNS TRIGGER AS $$
            BEGIN
                NEW.updated_at = CURRENT_TIMESTAMP;
                RETURN NEW;
            END;
            $$ language 'plpgsql'
        """);
        
        // 트리거 생성
        statement.execute("""
            CREATE TRIGGER update_users_updated_at 
                BEFORE UPDATE ON users 
                FOR EACH ROW 
                EXECUTE FUNCTION update_updated_at_column()
        """);
        
        statement.execute("""
            CREATE TRIGGER update_chat_rooms_updated_at 
                BEFORE UPDATE ON chat_rooms 
                FOR EACH ROW 
                EXECUTE FUNCTION update_updated_at_column()
        """);
    }
    
    private static void createInitialData(Statement statement) throws SQLException {
        // 기본 채팅방 생성
        statement.execute("""
            INSERT INTO chat_rooms (room_id, room_name, room_type, description, max_users) VALUES
            ('general', '일반 채팅방', 'NORMAL', '모든 사용자가 참여할 수 있는 일반 채팅방입니다.', NULL),
            ('tech', '개발 이야기', 'NORMAL', '개발과 기술에 관한 이야기를 나누는 채팅방입니다.', NULL),
            ('casual', '자유 토론', 'NORMAL', '자유롭게 대화를 나누는 채팅방입니다.', NULL),
            ('secret', '🔐 비밀 대화', 'SECRET', '암호화된 메시지로 안전한 대화를 나누는 채팅방입니다.', 10),
            ('volatile', '💥 휘발성 채팅', 'VOLATILE', '메시지가 자동으로 삭제되는 휘발성 채팅방입니다.', 20)
        """);
        
        // secret 방에 암호화 키 설정
        statement.execute("UPDATE chat_rooms SET encryption_key = 'secret123' WHERE room_id = 'secret'");
    }
    
    private static void verifyTables(Statement statement) throws SQLException {
        // dev 스키마 테이블 확인
        System.out.println("\n📊 chatapp_dev 스키마 테이블:");
        var devResult = statement.executeQuery("""
            SELECT table_name FROM information_schema.tables 
            WHERE table_schema = 'chatapp_dev' 
            ORDER BY table_name
        """);
        while (devResult.next()) {
            System.out.println("  ✓ " + devResult.getString("table_name"));
        }
        
        // prod 스키마 테이블 확인
        System.out.println("\n📊 chatapp_prod 스키마 테이블:");
        var prodResult = statement.executeQuery("""
            SELECT table_name FROM information_schema.tables 
            WHERE table_schema = 'chatapp_prod' 
            ORDER BY table_name
        """);
        while (prodResult.next()) {
            System.out.println("  ✓ " + prodResult.getString("table_name"));
        }
        
        // 채팅방 데이터 확인
        statement.execute("SET search_path TO chatapp_dev");
        var roomsResult = statement.executeQuery("SELECT room_id, room_name FROM chat_rooms ORDER BY room_id");
        System.out.println("\n🏠 생성된 채팅방:");
        while (roomsResult.next()) {
            System.out.println("  🏠 " + roomsResult.getString("room_id") + ": " + roomsResult.getString("room_name"));
        }
    }
}