-- V6: Add reply/quote support to messages

-- Add reply columns to messages table
ALTER TABLE messages ADD COLUMN IF NOT EXISTS reply_to_id BIGINT;
ALTER TABLE messages ADD COLUMN IF NOT EXISTS reply_to_sender VARCHAR(50);
ALTER TABLE messages ADD COLUMN IF NOT EXISTS reply_to_content VARCHAR(200);

-- Add reply columns to direct_messages table
ALTER TABLE direct_messages ADD COLUMN IF NOT EXISTS reply_to_id BIGINT;
ALTER TABLE direct_messages ADD COLUMN IF NOT EXISTS reply_to_sender VARCHAR(50);
ALTER TABLE direct_messages ADD COLUMN IF NOT EXISTS reply_to_content VARCHAR(200);

-- Add indexes for reply lookups (partial indexes for non-null values)
CREATE INDEX IF NOT EXISTS idx_messages_reply_to ON messages(reply_to_id) WHERE reply_to_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_direct_messages_reply_to ON direct_messages(reply_to_id) WHERE reply_to_id IS NOT NULL;

COMMENT ON COLUMN messages.reply_to_id IS 'ID of the original message being replied to';
COMMENT ON COLUMN messages.reply_to_sender IS 'Sender name of the original message (denormalized)';
COMMENT ON COLUMN messages.reply_to_content IS 'Preview of the original message content (max 200 chars)';

COMMENT ON COLUMN direct_messages.reply_to_id IS 'ID of the original message being replied to';
COMMENT ON COLUMN direct_messages.reply_to_sender IS 'Sender name of the original message (denormalized)';
COMMENT ON COLUMN direct_messages.reply_to_content IS 'Preview of the original message content (max 200 chars)';
