-- Reference schema for the history tables added by the MySQL integration.
-- You do NOT have to run this: Spring Data JPA (spring.jpa.hibernate.ddl-auto=update) creates the same tables on first
-- start and never drops or changes the existing scans, cleanups and limits tables or their rows.
-- Run it only if you prefer to create the tables yourself, before the first start:
--   mysql -u osdiag -p os_diagnostic_engine < docs/mysql-history-schema.sql

CREATE TABLE IF NOT EXISTS performance_samples (
  id         BIGINT NOT NULL AUTO_INCREMENT,
  time       DATETIME(6),
  cpu        DOUBLE,
  mem        DOUBLE,
  mem_free   BIGINT,
  mem_total  BIGINT,
  paging     DOUBLE,
  procs      INT,
  disk       DOUBLE,
  io_wait    DOUBLE,
  temp       DOUBLE,
  PRIMARY KEY (id),
  KEY idx_performance_samples_time (time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS security_scans (
  id              BIGINT NOT NULL AUTO_INCREMENT,
  scan_time       DATETIME(6),
  summary         VARCHAR(500),
  total_findings  INT NOT NULL,
  PRIMARY KEY (id),
  KEY idx_security_scans_scan_time (scan_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS security_findings (
  id                BIGINT NOT NULL AUTO_INCREMENT,
  security_scan_id  BIGINT NOT NULL,
  level             VARCHAR(20),
  title             VARCHAR(300),
  text              VARCHAR(2000),
  PRIMARY KEY (id),
  CONSTRAINT fk_security_findings_scan FOREIGN KEY (security_scan_id) REFERENCES security_scans (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS disk_snapshots (
  id           BIGINT NOT NULL AUTO_INCREMENT,
  scan_id      BIGINT,
  drive_name   VARCHAR(255),
  total_space  BIGINT NOT NULL,
  free_space   BIGINT NOT NULL,
  used_space   BIGINT NOT NULL,
  captured_at  DATETIME(6),
  PRIMARY KEY (id),
  KEY idx_disk_snapshots_captured_at (captured_at),
  CONSTRAINT fk_disk_snapshots_scan FOREIGN KEY (scan_id) REFERENCES scans (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS startup_snapshots (
  id            BIGINT NOT NULL AUTO_INCREMENT,
  captured_at   DATETIME(6),
  power_plan    VARCHAR(100),
  content_hash  VARCHAR(64) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_startup_snapshots_captured_at (captured_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS startup_entries (
  id           BIGINT NOT NULL AUTO_INCREMENT,
  snapshot_id  BIGINT NOT NULL,
  name         VARCHAR(255),
  command      VARCHAR(1000),
  location     VARCHAR(100),
  PRIMARY KEY (id),
  CONSTRAINT fk_startup_entries_snapshot FOREIGN KEY (snapshot_id) REFERENCES startup_snapshots (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS system_action_logs (
  id               BIGINT NOT NULL AUTO_INCREMENT,
  action_type      VARCHAR(30),
  target           VARCHAR(255),
  previous_value   VARCHAR(255),
  new_value        VARCHAR(255),
  execution_time   DATETIME(6),
  success          BIT(1) NOT NULL,
  result_message   VARCHAR(500),
  PRIMARY KEY (id),
  KEY idx_system_action_logs_execution_time (execution_time)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS optimization_runs (
  id           BIGINT NOT NULL AUTO_INCREMENT,
  scan_id      BIGINT NOT NULL,
  started_at   DATETIME(6),
  finished_at  DATETIME(6),
  PRIMARY KEY (id),
  KEY idx_optimization_runs_started_at (started_at),
  CONSTRAINT fk_optimization_runs_scan FOREIGN KEY (scan_id) REFERENCES scans (id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS optimization_actions (
  id                   BIGINT NOT NULL AUTO_INCREMENT,
  optimization_run_id  BIGINT NOT NULL,
  cleanup_id           BIGINT,
  action_key           VARCHAR(20),
  planned_files        INT NOT NULL,
  planned_bytes        BIGINT NOT NULL,
  selected             BIT(1) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_optimization_actions_run FOREIGN KEY (optimization_run_id) REFERENCES optimization_runs (id),
  CONSTRAINT fk_optimization_actions_cleanup FOREIGN KEY (cleanup_id) REFERENCES cleanups (id)
) ENGINE=InnoDB;
