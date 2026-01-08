-- V3: Add encryption support for DirectMessage and GroupMessage tables
-- Migration for server-side encryption at rest

-- Add encryption columns to direct_messages table
ALTER TABLE direct_messages
ADD COLUMN IF NOT EXISTS is_encrypted BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE direct_messages
ADD COLUMN IF NOT EXISTS security_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

-- Add encryption columns to group_messages table
ALTER TABLE group_messages
ADD COLUMN IF NOT EXISTS is_encrypted BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE group_messages
ADD COLUMN IF NOT EXISTS security_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL';

-- Add index for efficient filtering of encrypted messages
CREATE INDEX IF NOT EXISTS idx_direct_messages_encrypted ON direct_messages(is_encrypted);
CREATE INDEX IF NOT EXISTS idx_group_messages_encrypted ON group_messages(is_encrypted);
CREATE INDEX IF NOT EXISTS idx_direct_messages_security_type ON direct_messages(security_type);
CREATE INDEX IF NOT EXISTS idx_group_messages_security_type ON group_messages(security_type);
