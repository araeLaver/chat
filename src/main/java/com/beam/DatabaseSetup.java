package com.beam;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Database setup utility for initializing chat server schemas.
 *
 * Usage:
 *   Set environment variables before running:
 *   - DATABASE_URL: JDBC connection URL
 *   - DATABASE_USERNAME: Database username
 *   - DATABASE_PASSWORD: Database password
 *
 * Example:
 *   export DATABASE_URL="jdbc:postgresql://localhost:5432/chatdb?sslmode=require"
 *   export DATABASE_USERNAME="postgres"
 *   export DATABASE_PASSWORD="your-password"
 *   java -cp target/classes com.beam.DatabaseSetup
 */
public class DatabaseSetup {

    private static final String DB_URL = System.getenv("DATABASE_URL");
    private static final String USERNAME = System.getenv("DATABASE_USERNAME");
    private static final String PASSWORD = System.getenv("DATABASE_PASSWORD");

    public static void main(String[] args) {
        if (DB_URL == null || USERNAME == null || PASSWORD == null) {
            System.err.println("Error: Required environment variables not set.");
            System.err.println("Please set: DATABASE_URL, DATABASE_USERNAME, DATABASE_PASSWORD");
            System.exit(1);
        }

        System.out.println("Starting PostgreSQL database initialization...");

        try {
            Class.forName("org.postgresql.Driver");

            Connection connection = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            Statement statement = connection.createStatement();

            System.out.println("Creating schemas...");
            statement.execute("CREATE SCHEMA IF NOT EXISTS chat_dev");
            statement.execute("CREATE SCHEMA IF NOT EXISTS chat_prod");
            System.out.println("Schemas created successfully.");

            System.out.println("Initializing chat_dev schema...");
            setupSchema(statement, "chat_dev");
            System.out.println("chat_dev initialized.");

            System.out.println("Initializing chat_prod schema...");
            setupSchema(statement, "chat_prod");
            System.out.println("chat_prod initialized.");

            verifyTables(statement);

            statement.close();
            connection.close();
            System.out.println("Database initialization completed!");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void setupSchema(Statement statement, String schema) throws SQLException {
        statement.execute("SET search_path TO " + schema);

        statement.execute("DROP TABLE IF EXISTS user_sessions CASCADE");
        statement.execute("DROP TABLE IF EXISTS messages CASCADE");
        statement.execute("DROP TABLE IF EXISTS chat_rooms CASCADE");
        statement.execute("DROP TABLE IF EXISTS users CASCADE");
        statement.execute("DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE");

        createTables(statement);
        createInitialData(statement);
    }

    private static void createTables(Statement statement) throws SQLException {
        statement.execute("""
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
            )
        """);

        statement.execute("""
            CREATE TABLE chat_rooms (
                room_id VARCHAR(50) PRIMARY KEY,
                room_name VARCHAR(100) NOT NULL,
                room_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
                encryption_key VARCHAR(255),
                description VARCHAR(500),
                is_active BOOLEAN NOT NULL DEFAULT true,
                max_users INTEGER,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT chk_room_type CHECK (room_type IN ('NORMAL', 'SECRET', 'VOLATILE'))
            )
        """);

        statement.execute("""
            CREATE TABLE messages (
                id BIGSERIAL PRIMARY KEY,
                user_id BIGINT,
                sender VARCHAR(50) NOT NULL,
                content TEXT,
                room_id VARCHAR(50) NOT NULL,
                message_type VARCHAR(20) NOT NULL,
                timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                security_type VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
                expires_at TIMESTAMP,
                encryption_key VARCHAR(255),
                is_encrypted BOOLEAN NOT NULL DEFAULT false,
                file_url VARCHAR(500),
                file_name VARCHAR(255),
                file_size BIGINT,
                edited_at TIMESTAMP,
                is_deleted BOOLEAN NOT NULL DEFAULT false,
                CONSTRAINT chk_security_type CHECK (security_type IN ('NORMAL', 'SECRET', 'VOLATILE')),
                CONSTRAINT chk_message_type CHECK (message_type IN ('message', 'system', 'file', 'volatile')),
                CONSTRAINT fk_messages_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
                CONSTRAINT fk_messages_room_id FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id) ON DELETE CASCADE
            )
        """);

        statement.execute("""
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
            )
        """);

        createIndexes(statement);
        createTriggers(statement);
    }

    private static void createIndexes(Statement statement) throws SQLException {
        statement.execute("CREATE INDEX idx_users_username ON users(username)");
        statement.execute("CREATE INDEX idx_users_email ON users(email)");
        statement.execute("CREATE INDEX idx_users_is_active ON users(is_active)");
        statement.execute("CREATE INDEX idx_chat_rooms_room_type ON chat_rooms(room_type)");
        statement.execute("CREATE INDEX idx_chat_rooms_is_active ON chat_rooms(is_active)");
        statement.execute("CREATE INDEX idx_messages_room_id ON messages(room_id)");
        statement.execute("CREATE INDEX idx_messages_sender ON messages(sender)");
        statement.execute("CREATE INDEX idx_messages_timestamp ON messages(timestamp)");
        statement.execute("CREATE INDEX idx_messages_user_id ON messages(user_id)");
        statement.execute("CREATE INDEX idx_sessions_session_id ON user_sessions(session_id)");
        statement.execute("CREATE INDEX idx_sessions_user_id ON user_sessions(user_id)");
        statement.execute("CREATE INDEX idx_sessions_is_online ON user_sessions(is_online)");
    }

    private static void createTriggers(Statement statement) throws SQLException {
        statement.execute("""
            CREATE OR REPLACE FUNCTION update_updated_at_column()
            RETURNS TRIGGER AS $$
            BEGIN
                NEW.updated_at = CURRENT_TIMESTAMP;
                RETURN NEW;
            END;
            $$ language 'plpgsql'
        """);

        statement.execute("""
            CREATE TRIGGER update_users_updated_at
                BEFORE UPDATE ON users
                FOR EACH ROW
                EXECUTE FUNCTION update_updated_at_column()
        """);

        statement.execute("""
            CREATE TRIGGER update_chat_rooms_updated_at
                BEFORE UPDATE ON chat_rooms
                FOR EACH ROW
                EXECUTE FUNCTION update_updated_at_column()
        """);
    }

    private static void createInitialData(Statement statement) throws SQLException {
        statement.execute("""
            INSERT INTO chat_rooms (room_id, room_name, room_type, description, max_users) VALUES
            ('general', 'General Chat', 'NORMAL', 'Public chat room for all users.', NULL),
            ('tech', 'Tech Talk', 'NORMAL', 'Discuss technology and development.', NULL),
            ('casual', 'Casual', 'NORMAL', 'Casual conversations.', NULL)
        """);
    }

    private static void verifyTables(Statement statement) throws SQLException {
        System.out.println("\nchat_dev schema tables:");
        var devResult = statement.executeQuery("""
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = 'chat_dev'
            ORDER BY table_name
        """);
        while (devResult.next()) {
            System.out.println("  - " + devResult.getString("table_name"));
        }

        System.out.println("\nchat_prod schema tables:");
        var prodResult = statement.executeQuery("""
            SELECT table_name FROM information_schema.tables
            WHERE table_schema = 'chat_prod'
            ORDER BY table_name
        """);
        while (prodResult.next()) {
            System.out.println("  - " + prodResult.getString("table_name"));
        }
    }
}
