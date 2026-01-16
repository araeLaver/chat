-- V9: Privacy and Translation Features Migration
-- Add anonymous user flag, privacy settings, TTL for messages, and translation metadata

-- Anonymous user flag
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_anonymous BOOLEAN DEFAULT FALSE NOT NULL;

-- Privacy settings
ALTER TABLE users ADD COLUMN IF NOT EXISTS ghost_mode BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS disable_ip_logging BOOLEAN DEFAULT FALSE NOT NULL;

-- Self-destructing messages - Messages table
ALTER TABLE messages ADD COLUMN IF NOT EXISTS ttl_seconds BIGINT DEFAULT NULL;
-- Note: expires_at column already exists from V2 migration for VOLATILE messages
-- We'll reuse it for TTL functionality

-- Self-destructing messages - Direct Messages table
ALTER TABLE direct_messages ADD COLUMN IF NOT EXISTS ttl_seconds BIGINT DEFAULT NULL;
ALTER TABLE direct_messages ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP DEFAULT NULL;

-- Self-destructing messages - Group Messages table
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS ttl_seconds BIGINT DEFAULT NULL;
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP DEFAULT NULL;

-- Indexes for efficient expired message queries
CREATE INDEX IF NOT EXISTS idx_dm_expires_at ON direct_messages(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_group_messages_expires_at ON group_messages(expires_at) WHERE expires_at IS NOT NULL;

-- Translation metadata for messages
ALTER TABLE messages ADD COLUMN IF NOT EXISTS source_language VARCHAR(10) DEFAULT NULL;
ALTER TABLE direct_messages ADD COLUMN IF NOT EXISTS source_language VARCHAR(10) DEFAULT NULL;
ALTER TABLE group_messages ADD COLUMN IF NOT EXISTS source_language VARCHAR(10) DEFAULT NULL;

-- Index for user privacy settings queries
CREATE INDEX IF NOT EXISTS idx_users_ghost_mode ON users(ghost_mode) WHERE ghost_mode = TRUE;
CREATE INDEX IF NOT EXISTS idx_users_anonymous ON users(is_anonymous) WHERE is_anonymous = TRUE;
