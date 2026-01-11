-- V8: Add pinned and starred messages tables

-- Pinned messages table
CREATE TABLE pinned_messages (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    context_type VARCHAR(20) NOT NULL,
    context_id VARCHAR(100) NOT NULL,
    pinned_by_user_id BIGINT NOT NULL,
    pin_order INTEGER,
    note VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pinned_messages_user FOREIGN KEY (pinned_by_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_pinned_message_type CHECK (message_type IN ('ROOM', 'DIRECT', 'GROUP')),
    CONSTRAINT chk_pinned_context_type CHECK (context_type IN ('ROOM', 'CONVERSATION')),
    CONSTRAINT uq_pinned_message_context UNIQUE (message_id, message_type, context_type, context_id)
);

-- Starred messages table
CREATE TABLE starred_messages (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    room_id VARCHAR(50),
    conversation_id VARCHAR(100),
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_starred_messages_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_starred_message_type CHECK (message_type IN ('ROOM', 'DIRECT', 'GROUP')),
    CONSTRAINT uq_starred_message_user UNIQUE (user_id, message_id, message_type)
);

-- Indexes for pinned_messages
CREATE INDEX idx_pinned_context ON pinned_messages(context_type, context_id);
CREATE INDEX idx_pinned_message ON pinned_messages(message_id, message_type);
CREATE INDEX idx_pinned_user ON pinned_messages(pinned_by_user_id);
CREATE INDEX idx_pinned_order ON pinned_messages(context_type, context_id, pin_order);

-- Indexes for starred_messages
CREATE INDEX idx_starred_user ON starred_messages(user_id);
CREATE INDEX idx_starred_message ON starred_messages(message_id, message_type);
CREATE INDEX idx_starred_room ON starred_messages(user_id, room_id) WHERE room_id IS NOT NULL;
CREATE INDEX idx_starred_conversation ON starred_messages(user_id, conversation_id) WHERE conversation_id IS NOT NULL;
CREATE INDEX idx_starred_created ON starred_messages(user_id, created_at DESC);

-- Comments
COMMENT ON TABLE pinned_messages IS 'Stores pinned messages in rooms and conversations';
COMMENT ON TABLE starred_messages IS 'Stores user starred/bookmarked messages';

COMMENT ON COLUMN pinned_messages.context_type IS 'Context type: ROOM or CONVERSATION';
COMMENT ON COLUMN pinned_messages.context_id IS 'Room ID or Conversation ID depending on context_type';
COMMENT ON COLUMN pinned_messages.pin_order IS 'Order of pinned message within context';
COMMENT ON COLUMN starred_messages.note IS 'User note for the starred message';
