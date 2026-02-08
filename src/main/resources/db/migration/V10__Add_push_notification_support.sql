-- Push Notification Support
-- User device tokens for FCM/APNs push notifications

CREATE TABLE user_device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_token VARCHAR(500) NOT NULL UNIQUE,
    device_type VARCHAR(20) NOT NULL, -- 'IOS', 'ANDROID', 'WEB'
    device_name VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Notification preferences per user
CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    push_enabled BOOLEAN DEFAULT true,
    message_preview BOOLEAN DEFAULT true,
    sound_enabled BOOLEAN DEFAULT true,
    vibration_enabled BOOLEAN DEFAULT true,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_device_tokens_user ON user_device_tokens(user_id);
CREATE INDEX idx_device_tokens_active ON user_device_tokens(user_id, is_active) WHERE is_active = true;
CREATE INDEX idx_notification_preferences_user ON notification_preferences(user_id);
