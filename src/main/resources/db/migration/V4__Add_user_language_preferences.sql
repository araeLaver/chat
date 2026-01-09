-- Add language preference fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_language VARCHAR(10) DEFAULT 'ko';
ALTER TABLE users ADD COLUMN IF NOT EXISTS auto_translate BOOLEAN DEFAULT FALSE NOT NULL;

-- Update existing users with default values
UPDATE users SET preferred_language = 'ko' WHERE preferred_language IS NULL;
UPDATE users SET auto_translate = FALSE WHERE auto_translate IS NULL;
