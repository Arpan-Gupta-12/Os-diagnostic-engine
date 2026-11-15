# MySQL integration report

## 1. Is there fake data in normal operation? No.

Every simulated value is in `Demo.java` and the `demo.on()` branches next to it. Demo mode is **off at start**, is switched
on only by `POST /api/demo` (the Settings page), is held in memory, and is gone after a restart. Scans and cleanups made in
demo mode are already stored with `demo = true` and are hidden when demo mode is off. In normal mode the numbers already come
from OSHI, `WindowsOs`, `LinuxOs`, `Cleaner` and `Large`. Nothing was removed, because removing demo mode would delete a
working feature. What was added is the guarantee that **demo data never reaches the new history tables**.

| Location | What it is | Normal mode | Action |
|---|---|---|---|
| `Demo.java` (all methods) | invented CPU/RAM/processes/disks/startup/ports/hosts/antivirus/power | not reachable | kept, isolated; new tables skip it |
| `Monitor.java` `sample()/recent()/fresh()/procs()/disks()` | `demo.on()` branch | OSHI | unchanged |
| `CleanApi.java` `pretend()`, `demoFiles()`, `demoBytes()` | invented cleanup counts and sizes | `Cleaner.preview/clean` | unchanged, stored with `demo=true` |
| `PlanApi.java` `demoBig()` | two invented large files | `Large.find` on Downloads and Desktop | unchanged |
| `CloseApi.java` demo `Map.of(...)` | invented "closed 5" result | real `ProcessHandle`, re-checked with `isAlive` | unchanged |
| `StartupApi.java` demo power message | message only | real `powerCfg`/`powerprofilesctl` and re-read | unchanged |
| `SecurityApi.java` demo branches | invented indicators | real procs, startup, hosts, ports | unchanged; demo scans are not saved |

Kept on purpose (logic, not data): diagnostic thresholds and defaults in `Limits`, `Security` rules, risky/bad port maps,
`CloseApi.PROTECTED`, the cleanup categories in `PlanApi.CLEANABLE`.

## 2. New tables (created by Hibernate `ddl-auto=update`; reference DDL in `docs/mysql-history-schema.sql`)

| Table | Entity | Purpose |
|---|---|---|
| `performance_samples` | `PerformanceSample` | one live sample every 30 s (configurable), 30-day retention |
| `security_scans` | `SecurityScan` | one saved security check |
| `security_findings` | `SecurityFinding` | its warning signs |
| `disk_snapshots` | `DiskSnapshot` | each drive measured during a checkup |
| `startup_snapshots` | `StartupSnapshot` | startup list and power plan, saved on change or on request |
| `startup_entries` | `StartupEntry` | the programs in a snapshot |
| `system_action_logs` | `SystemActionLog` | program closing and power plan changes with the verified outcome |
| `optimization_runs` | `OptimizationRun` | one Optimize session |
| `optimization_actions` | `OptimizationAction` | the cleanups offered in it |

Retained unchanged: `scans`, `cleanups`, `limits` (and the `scan_problems` collection table Hibernate already made).

### Keys
- `security_findings.security_scan_id → security_scans.id` (many-to-one, cascade delete with the scan)
- `disk_snapshots.scan_id → scans.id` (nullable, many drives per checkup)
- `startup_entries.snapshot_id → startup_snapshots.id`
- `optimization_runs.scan_id → scans.id` (the checkup the session started from)
- `optimization_actions.optimization_run_id → optimization_runs.id`
- `optimization_actions.cleanup_id → cleanups.id` (nullable until the user runs that cleanup)

Indexes: the timestamp column of every history table (`performance_samples.time`, `security_scans.scan_time`,
`disk_snapshots.captured_at`, `startup_snapshots.captured_at`, `system_action_logs.execution_time`,
`optimization_runs.started_at`) and, through MySQL, every foreign key.

Not created, on purpose: `SecurityFindingRepo`, `StartupEntryRepo`, `OptimizationActionRepo`. Findings, entries and actions
are always saved and read through their parent (`cascade = ALL`), so separate repositories would be unused code. No process
list is stored: it holds user names and paths and no feature needs it.

## 3. Behaviour

- **Performance**: the 3 s live loop is untouched. `Recorder.saveSample()` copies the newest in-memory sample to MySQL every
  `app.history.sample-interval-ms` (30 000). It runs on its own scheduler call, outside `Monitor`'s lock, so live reads are
  not delayed. `Recorder.deleteOldSamples()` runs hourly and deletes only `performance_samples` older than
  `app.history.retention-days` (30; `0` keeps everything).
- **Security**: `GET /api/security` is read-only as before. `POST /api/security/scan` runs the same check and saves it; the
  "Check again" button uses it.
- **Disks**: every `POST /api/scan` saves one `disk_snapshots` row per drive, linked to the new scan.
- **Startup**: `GET /api/startup` saves a snapshot only if the SHA-256 of (programs + power plan) differs from the latest
  snapshot; `POST /api/startup/snapshot` forces one. If the PC returned no programs and no power plan the read is treated as
  failed and nothing is saved.
- **Actions**: closing a program is logged with `success = (no process of that name still alive)`; a power change with
  `success = (plan really differs after re-reading it)`. Refused requests (400) are not logged because nothing was executed.
- **Optimize**: the page passes the checkup's `scanId` to `GET /api/plan` and `POST /api/cleanup`. Both parameters are
  optional, so old clients work. The plan call creates one run per checkup; each cleanup marks its action and links the
  `cleanups` row.
- Existing `/api/scans` and `/api/cleanups` now filter, sort and limit in SQL instead of loading every row.
- A database failure while saving history is logged and never fails the feature that triggered it.

## 4. REST API changes (all additive)

| Endpoint | Change |
|---|---|
| `GET /api/plan?scanId=` | optional `scanId` |
| `POST /api/cleanup?scanId=` | optional `scanId` |
| `POST /api/security/scan` | new; same body as `GET /api/security` plus `saved`, `scanId` |
| `POST /api/startup/snapshot` | new; `{ "saved": true }` |
| `GET /api/history/performance?hours=24&limit=500` | new |
| `GET /api/history/security?days=30&limit=50` | new |
| `GET /api/history/disks?days=30&limit=200` | new |
| `GET /api/history/startup?days=90&limit=20` | new |
| `GET /api/history/actions?days=90&limit=100` | new |
| `GET /api/history/optimizations?days=90&limit=50` | new |

All other endpoints and response bodies are unchanged.

## 5. Configuration

`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` as before. New, optional: `HISTORY_SAMPLE_INTERVAL_MS` (default 30000),
`HISTORY_RETENTION_DAYS` (default 30). Tests still use in-memory H2.
