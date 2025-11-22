-- chatapp_dev 스키마 초기화 스크립트 (카카오톡 스타일 메신저)
SET search_path TO chatapp_dev;

-- 기존 테이블 삭제 (순서 중요 - FK 관계 고려)
DROP TABLE IF EXISTS message_read_receipts CASCADE;
DROP TABLE IF EXISTS friends CASCADE;
DROP TABLE IF EXISTS user_sessions CASCADE;
DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS chat_rooms CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 함수 삭제
DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;

-- Users Table (사용자 정보)
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
);

-- Friends Table (친구 관계)
CREATE TABLE friends (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    friend_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP,

    CONSTRAINT fk_friends_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_friends_friend_id FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_friend_status CHECK (status IN ('PENDING', 'ACCEPTED', 'BLOCKED')),
    CONSTRAINT uk_friends_user_friend UNIQUE (user_id, friend_id)
);

-- Chat Rooms Table (채팅방 - 그룹 채팅 및 1:1 채팅)
CREATE TABLE chat_rooms (
    room_id VARCHAR(50) PRIMARY KEY,
    room_name VARCHAR(100) NOT NULL,
    room_type VARCHAR(20) NOT NULL DEFAULT 'GROUP',
    creator VARCHAR(50),
    description VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    max_users INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_room_type CHECK (room_type IN ('DIRECT', 'GROUP'))
);

-- Messages Table (메시지)
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    sender VARCHAR(50) NOT NULL,
    content TEXT,
    room_id VARCHAR(50) NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    security_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    file_url VARCHAR(500),
    file_name VARCHAR(255),
    file_size BIGINT,
    edited_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,

    CONSTRAINT chk_message_type CHECK (message_type IN ('message', 'system', 'file')),
    CONSTRAINT fk_messages_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_messages_room_id FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id) ON DELETE CASCADE
);

-- Message Read Receipts Table (읽음 표시)
CREATE TABLE message_read_receipts (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_read_receipts_message_id FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    CONSTRAINT fk_read_receipts_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_read_receipts_message_user UNIQUE (message_id, user_id)
);

-- User Sessions Table (사용자 세션)
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
);

-- Indexes for Performance
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active);
CREATE INDEX idx_users_created_at ON users(created_at);

CREATE INDEX idx_friends_user_id ON friends(user_id);
CREATE INDEX idx_friends_friend_id ON friends(friend_id);
CREATE INDEX idx_friends_status ON friends(status);
CREATE INDEX idx_friends_created_at ON friends(created_at);

CREATE INDEX idx_chat_rooms_room_type ON chat_rooms(room_type);
CREATE INDEX idx_chat_rooms_is_active ON chat_rooms(is_active);
CREATE INDEX idx_chat_rooms_creator ON chat_rooms(creator);

CREATE INDEX idx_messages_room_id ON messages(room_id);
CREATE INDEX idx_messages_sender ON messages(sender);
CREATE INDEX idx_messages_timestamp ON messages(timestamp);
CREATE INDEX idx_messages_user_id ON messages(user_id);
CREATE INDEX idx_messages_is_deleted ON messages(is_deleted);

CREATE INDEX idx_read_receipts_message_id ON message_read_receipts(message_id);
CREATE INDEX idx_read_receipts_user_id ON message_read_receipts(user_id);
CREATE INDEX idx_read_receipts_read_at ON message_read_receipts(read_at);

CREATE INDEX idx_sessions_session_id ON user_sessions(session_id);
CREATE INDEX idx_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_sessions_room_id ON user_sessions(room_id);
CREATE INDEX idx_sessions_is_online ON user_sessions(is_online);
CREATE INDEX idx_sessions_last_activity ON user_sessions(last_activity);

-- Trigger Function for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_chat_rooms_updated_at
    BEFORE UPDATE ON chat_rooms
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Initial Data - Default Group Chat Rooms
INSERT INTO chat_rooms (room_id, room_name, room_type, creator, description) VALUES
('general', '일반 채팅방', 'GROUP', 'system', '모든 사용자가 참여할 수 있는 일반 채팅방입니다.'),
('tech', '개발 이야기', 'GROUP', 'system', '개발과 기술에 관한 이야기를 나누는 채팅방입니다.'),
('casual', '자유 토론', 'GROUP', 'system', '자유롭게 대화를 나누는 채팅방입니다.');

-- Test Users (optional - for development)
-- Password is '1234' hashed
INSERT INTO users (username, password, email, status_message) VALUES
('테스트유저1', '1509442', 'user1@test.com', '안녕하세요!'),
('테스트유저2', '1509442', 'user2@test.com', '반갑습니다!'),
('테스트유저3', '1509442', 'user3@test.com', '테스트 중입니다.');

-- Test Friendships
INSERT INTO friends (user_id, friend_id, status, accepted_at) VALUES
(1, 2, 'ACCEPTED', CURRENT_TIMESTAMP),
(1, 3, 'ACCEPTED', CURRENT_TIMESTAMP),
(2, 3, 'PENDING', NULL);

-- Verify tables created
SELECT 'Tables created successfully in chatapp_dev schema' as status;
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'chatapp_dev'
ORDER BY table_name;
