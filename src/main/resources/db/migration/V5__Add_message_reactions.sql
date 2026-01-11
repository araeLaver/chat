-- V5: Add message reactions table for emoji reactions on messages

CREATE TABLE message_reactions (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    emoji VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reactions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_message_type CHECK (message_type IN ('ROOM', 'DIRECT', 'GROUP')),
    CONSTRAINT uk_reaction_user_message UNIQUE (user_id, message_id, message_type, emoji)
);

-- Indexes for efficient querying
CREATE INDEX idx_reactions_message ON message_reactions(message_id, message_type);
CREATE INDEX idx_reactions_user ON message_reactions(user_id);
CREATE INDEX idx_reactions_emoji ON message_reactions(emoji);
CREATE INDEX idx_reactions_created ON message_reactions(created_at DESC);

COMMENT ON TABLE message_reactions IS 'Stores emoji reactions on messages';
COMMENT ON COLUMN message_reactions.message_type IS 'Type of message: ROOM (채팅방), DIRECT (1:1 DM), GROUP (그룹)';
COMMENT ON COLUMN message_reactions.emoji IS 'Unicode emoji or custom emoji code';
