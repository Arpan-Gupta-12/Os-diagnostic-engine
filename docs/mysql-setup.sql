-- One-time MySQL setup for OS Diagnostic Engine.
-- Run it as the MySQL root user, for example:
--   mysql -u root -p < docs/mysql-setup.sql
-- The tables themselves are created automatically by Spring Data JPA on first start.

CREATE DATABASE IF NOT EXISTS os_diagnostic_engine
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'osdiag'@'localhost' IDENTIFIED BY 'osdiag_password';
GRANT ALL PRIVILEGES ON os_diagnostic_engine.* TO 'osdiag'@'localhost';
FLUSH PRIVILEGES;

-- Useful queries during a demo:
--   USE os_diagnostic_engine;
--   SHOW TABLES;
--   SELECT id, scanned_at, overall_status, health_score, primary_title FROM diagnostic_scans ORDER BY id DESC LIMIT 5;
--   SELECT session_id, title, status, bytes_freed FROM optimization_actions ORDER BY id DESC LIMIT 10;
