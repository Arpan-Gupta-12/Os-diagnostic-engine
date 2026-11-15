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
--   SELECT id, time, status, score, summary FROM scans ORDER BY id DESC LIMIT 5;
--   SELECT id, time, what, files, bytes, verified FROM cleanups ORDER BY id DESC LIMIT 5;
--   SELECT * FROM limits;
--   SELECT id, scan_time, total_findings, summary FROM security_scans ORDER BY id DESC LIMIT 5;
--   SELECT scan_id, drive_name, total_space, free_space, used_space FROM disk_snapshots ORDER BY id DESC LIMIT 5;
--   SELECT action_type, target, previous_value, new_value, success, result_message FROM system_action_logs ORDER BY id DESC LIMIT 5;
--   SELECT time, cpu, mem, disk FROM performance_samples ORDER BY id DESC LIMIT 5;
