-- BEAM 메신저 chat 스키마 초기화 스크립트
-- 스키마 생성
CREATE SCHEMA IF NOT EXISTS chat;
SET search_path TO chat;

-- 기존 테이블 삭제 (FK 관계 역순)
DROP TABLE IF EXISTS read_receipts CASCADE;
DROP TABLE IF EXISTS file_metadata CASCADE;
DROP TABLE IF EXISTS group_messages CASCADE;
DROP TABLE IF EXISTS room_members CASCADE;
DROP TABLE IF EXISTS rooms CASCADE;
DROP TABLE IF EXISTS direct_messages CASCADE;
DROP TABLE IF EXISTS conversations CASCADE;
DROP TABLE IF EXISTS friends CASCADE;
DROP TABLE IF EXISTS user_sessions CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 1. Users 테이블
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100),
    display_name VARCHAR(100),
    profile_image VARCHAR(500),
    status_message VARCHAR(200),
    is_online BOOLEAN NOT NULL DEFAULT false,
    last_seen TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_phone_verified BOOLEAN NOT NULL DEFAULT false,
    verification_code VARCHAR(6),
    verification_code_expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_users_phone_number ON users(phone_number);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_is_online ON users(is_online);

-- 2. Friends 테이블
CREATE TABLE friends (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    friend_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP,
    accepted_at TIMESTAMP,
    blocked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_friend_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'BLOCKED')),
    CONSTRAINT uk_friends_user_friend UNIQUE (user_id, friend_id)
);

CREATE INDEX idx_friends_user_friend ON friends(user_id, friend_id);
CREATE INDEX idx_friends_status ON friends(status);

-- 3. Conversations 테이블 (1:1 DM 대화)
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(100) NOT NULL UNIQUE,
    user1_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user2_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    last_message TEXT,
    last_message_time TIMESTAMP,
    last_message_sender_id BIGINT,
    unread_count_user1 INTEGER NOT NULL DEFAULT 0,
    unread_count_user2 INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_conversations_user1_user2 ON conversations(user1_id, user2_id);

-- 4. Direct Messages 테이블
CREATE TABLE direct_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id VARCHAR(100) NOT NULL,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    receiver_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN NOT NULL DEFAULT false,
    read_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    message_type VARCHAR(20),
    file_url VARCHAR(500),
    file_name VARCHAR(200),
    file_size BIGINT
);

CREATE INDEX idx_dm_sender_receiver ON direct_messages(sender_id, receiver_id);
CREATE INDEX idx_dm_conversation ON direct_messages(conversation_id);
CREATE INDEX idx_dm_timestamp ON direct_messages(timestamp);

-- 5. Rooms 테이블 (그룹 채팅방)
CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    room_name VARCHAR(100) NOT NULL,
    description TEXT,
    room_type VARCHAR(20) NOT NULL DEFAULT 'PUBLIC',
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    max_members INTEGER NOT NULL DEFAULT 100,
    current_members INTEGER NOT NULL DEFAULT 0,
    room_image_url VARCHAR(500),
    is_active BOOLEAN NOT NULL DEFAULT true,
    last_message TEXT,
    last_message_time TIMESTAMP,
    last_message_sender_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT chk_room_type CHECK (room_type IN ('PUBLIC', 'PRIVATE', 'SECRET'))
);

CREATE INDEX idx_rooms_room_type ON rooms(room_type);
CREATE INDEX idx_rooms_created_by ON rooms(created_by);

-- 6. Room Members 테이블
CREATE TABLE room_members (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    is_muted BOOLEAN NOT NULL DEFAULT false,
    muted_until TIMESTAMP,
    unread_count INTEGER NOT NULL DEFAULT 0,
    last_read_time TIMESTAMP,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    left_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT chk_member_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
);

CREATE INDEX idx_room_members_room_user ON room_members(room_id, user_id);
CREATE INDEX idx_room_members_user_rooms ON room_members(user_id);

-- 7. Group Messages 테이블
CREATE TABLE group_messages (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    file_url VARCHAR(1000),
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_count INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    CONSTRAINT chk_gm_message_type CHECK (message_type IN ('TEXT', 'IMAGE', 'FILE', 'VOICE', 'VIDEO', 'SYSTEM'))
);

CREATE INDEX idx_gm_room_timestamp ON group_messages(room_id, timestamp);
CREATE INDEX idx_gm_sender ON group_messages(sender_id);

-- 8. File Metadata 테이블
CREATE TABLE file_metadata (
    id BIGSERIAL PRIMARY KEY,
    file_name VARCHAR(500) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploader_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    conversation_id VARCHAR(100),
    room_id BIGINT REFERENCES rooms(id) ON DELETE SET NULL,
    category VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    thumbnail_path VARCHAR(500),
    download_count INTEGER NOT NULL DEFAULT 0,
    expires_at TIMESTAMP,
    is_deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMP,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_file_category CHECK (category IN ('IMAGE', 'VIDEO', 'AUDIO', 'DOCUMENT', 'OTHER'))
);

CREATE INDEX idx_file_uploader ON file_metadata(uploader_id);
CREATE INDEX idx_file_conversation ON file_metadata(conversation_id);
CREATE INDEX idx_file_room ON file_metadata(room_id);

-- 9. Read Receipts 테이블
CREATE TABLE read_receipts (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT,
    group_message_id BIGINT,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_rr_message_user ON read_receipts(message_id, user_id);
CREATE INDEX idx_rr_group_message_user ON read_receipts(group_message_id, user_id);

-- 10. User Sessions 테이블
CREATE TABLE user_sessions (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(100) NOT NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    username VARCHAR(50) NOT NULL,
    room_id VARCHAR(50),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    is_online BOOLEAN NOT NULL DEFAULT true,
    last_activity TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    connected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    disconnected_at TIMESTAMP
);

CREATE INDEX idx_sessions_session_id ON user_sessions(session_id);
CREATE INDEX idx_sessions_user_id ON user_sessions(user_id);
CREATE INDEX idx_sessions_is_online ON user_sessions(is_online);

-- 기본 채팅방 생성 (시스템 사용자 없이)
-- INSERT INTO rooms (room_name, description, room_type, created_by, max_members) VALUES
-- ('일반 채팅방', '모든 사용자가 참여할 수 있는 일반 채팅방입니다.', 'PUBLIC', 1, 1000),
-- ('개발 이야기', '개발과 기술에 관한 이야기를 나누는 채팅방입니다.', 'PUBLIC', 1, 500);

-- 완료 메시지
SELECT 'BEAM chat schema created successfully' as status;
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'chat'
ORDER BY table_name;
