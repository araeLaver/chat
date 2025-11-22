-- PostgreSQL Schema Setup for Simple Chat Server
-- Run this script to create both dev and prod schemas

-- Connect as superuser or database owner
-- CREATE DATABASE koyebdb; -- If database doesn't exist

-- Create schemas
CREATE SCHEMA IF NOT EXISTS chatapp_dev;
CREATE SCHEMA IF NOT EXISTS chatapp_prod;

-- Grant permissions (adjust user as needed)
GRANT ALL PRIVILEGES ON SCHEMA chatapp_dev TO "koyeb-adm";
GRANT ALL PRIVILEGES ON SCHEMA chatapp_prod TO "koyeb-adm";

-- Set search path for development
-- SET search_path TO chatapp_dev;

-- You can now run the schema creation DDL in each schema by setting the currentSchema parameter in the connection URL