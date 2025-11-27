-- PostgreSQL Schema Setup for BEAM Chat Server
-- Run this script to create schemas for dev and prod environments

-- Create schemas
CREATE SCHEMA IF NOT EXISTS chat_dev;
CREATE SCHEMA IF NOT EXISTS chat_prod;

-- Grant permissions (replace 'your_username' with actual database user)
-- GRANT ALL PRIVILEGES ON SCHEMA chat_dev TO your_username;
-- GRANT ALL PRIVILEGES ON SCHEMA chat_prod TO your_username;

-- Set search path for development
-- SET search_path TO chat_dev;

-- You can now run the schema creation DDL in each schema
-- by setting the currentSchema parameter in the connection URL
