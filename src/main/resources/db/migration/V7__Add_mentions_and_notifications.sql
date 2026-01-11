-- V7: Add mentions and notifications tables

-- Mentions table
CREATE TABLE mentions (
    id BIGSERIAL PRIMARY KEY,
    message_id BIGINT NOT NULL,
    message_type VARCHAR(20) NOT NULL,
    mentioned_user_id BIGINT NOT NULL,
    mentioner_user_id BIGINT NOT NULL,
    room_id VARCHAR(50),
    conversation_id VARCHAR(100),
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,

    CONSTRAINT fk_mentions_mentioned_user FOREIGN KEY (mentioned_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_mentions_mentioner_user FOREIGN KEY (mentioner_user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_mention_message_type CHECK (message_type IN ('ROOM', 'DIRECT', 'GROUP'))
);

-- Notifications table
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    reference_id BIGINT NOT NULL,
    actor_user_id BIGINT,
    content VARCHAR(500),
    room_id VARCHAR(50),
    is_read BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP,

    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_notification_type CHECK (type IN ('MENTION', 'REACTION', 'REPLY', 'PIN', 'FRIEND_REQUEST', 'FRIEND_ACCEPTED', 'ROOM_INVITE'))
);

-- Indexes for mentions
CREATE INDEX idx_mentions_message ON mentions(message_id, message_type);
CREATE INDEX idx_mentions_mentioned_user ON mentions(mentioned_user_id);
CREATE INDEX idx_mentions_unread ON mentions(mentioned_user_id, is_read) WHERE is_read = false;
CREATE INDEX idx_mentions_room ON mentions(room_id) WHERE room_id IS NOT NULL;
CREATE INDEX idx_mentions_created ON mentions(created_at DESC);

-- Indexes for notifications
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read) WHERE is_read = false;
CREATE INDEX idx_notifications_type ON notifications(user_id, type);
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);

-- Comments
COMMENT ON TABLE mentions IS 'Stores @mentions in messages';
COMMENT ON TABLE notifications IS 'Stores user notifications for various events';

COMMENT ON COLUMN mentions.message_type IS 'Type of message: ROOM, DIRECT, GROUP';
COMMENT ON COLUMN notifications.type IS 'Notification type: MENTION, REACTION, REPLY, PIN, FRIEND_REQUEST, FRIEND_ACCEPTED, ROOM_INVITE';
COMMENT ON COLUMN notifications.reference_id IS 'ID of the related entity (message, friend request, etc.)';
COMMENT ON COLUMN notifications.actor_user_id IS 'User who triggered the notification';
