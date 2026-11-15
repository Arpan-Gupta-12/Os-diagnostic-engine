# OS Diagnostic Engine

A friendly app that watches your PC (Windows or Linux), explains in plain English why it may be slow,
and has one button, **Optimize My PC**, that safely removes old temp files and then checks that it worked.

| Part | Technology |
|------|-----------|
| Frontend | React, Tailwind CSS, Recharts, CSS animations (black and orange theme, with a light mode) |
| Backend | Java 17+, Spring Boot (Web, JPA, Validation), Maven |
| Live data | OSHI (CPU, memory, disk, programs) |
| Database | MySQL (H2 for tests, or as a no-install option) |

## What it does

- **Overview**: health score, what was found, live numbers.
- **Live Diagnostics**: six charts updated every 3 seconds: CPU, memory, paging, disk busy, I/O wait, temperature.
- **Processes**: search and sort running programs (read only, it never closes anything).
- **Optimize PC**: Check, Plan, Clean (after you click *Clean now*), Verify, then a before / after report. Cleans old temp files, thumbnails, browser caches and crash reports. The plan also gives advice on startup programs, memory and hardware, lists your biggest files (never deleted), and says clearly what this app does not do. Two real speed-ups need your confirmation: **closing a heavy program** (the app tells you the memory that came back) and **switching the power plan** to Balanced or High performance.
- **Security**: looks for warning signs of a compromised PC (indicators of compromise): programs running from temp or Downloads folders, look-alike system program names, hidden or encoded startup commands, scheduled tasks and cron jobs, a changed hosts file, open remote-access ports, connections to ports that botnets use, antivirus switched off. Read only, and clear that it is not a virus scan.
- **Startup & Power**: programs that start with the PC and the power plan, with advice. Read only.
- **History & Reports**: every checkup and cleanup from the database, with filters and a CSV download.
- **Settings**: welcome animation, minimum age of files, the limits used by the rules, demo mode, about this PC.
- **Demo mode**: made-up numbers (healthy or slow PC) for trying the app on a PC without problems. Nothing is deleted.
- **Dark and light mode**: a button at the bottom of the menu. Dark is black and dense orange, light is warm white and orange. The choice is remembered.
- Intro animation (about 3 seconds, skippable, respects "reduce motion"): a small PC screen wakes up, smiles and zooms into the home screen.

## Code layout

```
backend/src/main/java/com/osdiag/       (one folder)
  App.java                       starts the app, opens the browser
  Monitor.java  Demo.java        live numbers (OSHI) / made-up numbers
  Checkup.java                   the diagnosis rules
  Cleaner.java  Plan.java  Large.java    safe cleanup / the optimization plan / biggest files
  Security.java                  the warning-sign rules (indicators of compromise)
  OsAdapter.java WindowsOs.java LinuxOs.java    what differs between Windows and Linux
  LiveApi  CheckupApi  CleanApi  PlanApi  CloseApi  StartupApi  SecurityApi  SettingsApi    the REST endpoints
  Scan.java  Cleanup.java  Limits.java  Repos.java          database tables
  PerformanceSample.java SecurityScan.java SecurityFinding.java DiskSnapshot.java StartupSnapshot.java
  StartupEntry.java SystemActionLog.java OptimizationRun.java OptimizationAction.java   history tables
  Recorder.java HistoryApi.java                                saves and reads the history (see docs/DATABASE_INTEGRATION.md)
  Sample.java  Proc.java  Disk.java                         plain data

frontend/src/
  App.jsx  api.js  format.js  theme.js  index.css   (all colors are in index.css, one set per theme)
  pages/       Home  Live  Processes  Optimize  Startup  Security  History  Settings
  components/  Logo  Mascot  Intro  ui
```

Flow: `React page -> api.js -> a *Api class -> Monitor / Checkup / Cleaner -> database`.

## Run it

Needs JDK 17+, Maven and Node 18+ (Node only to build the screen). MySQL is optional.

```bash
mysql -u root -p < docs/mysql-setup.sql           # once (Windows PowerShell: Get-Content docs\mysql-setup.sql | mysql -u root -p)
```

Then build and start, one command at a time:

```
Windows (PowerShell):   .\scripts\build-app.bat
                        .\scripts\run.bat
Linux:                  ./scripts/build-app.sh
                        ./scripts/run.sh
```

The dashboard opens at <http://localhost:8080>.

- No MySQL: add `--spring.profiles.active=h2` to the run command (history is kept in a file in your home folder).
- Port 8080 busy: add `--server.port=8090`.
- Development: `cd backend && mvn spring-boot:run` and `cd frontend && npm install && npm run dev` (open port 5173).

The app must run on the PC it watches, so it cannot be hosted on a static site such as Vercel.
Database settings are in `backend/src/main/resources/application.properties`
(or the `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` environment variables).

## Tests

```bash
cd backend && mvn test      # 44 tests: rules, security rules, closing and power safety, cleanup safety, Windows / Linux parsing, REST API + database
cd frontend && npm test     # 7 tests
```

## REST API

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/info` | OS, CPU, memory, drives, demo mode |
| GET | `/api/live` | the last 60 samples (3 minutes) |
| GET | `/api/procs?search=&sort=mem\|cpu` | running programs |
| POST | `/api/scan` | checkup, saved to the database |
| GET | `/api/scans?status=&days=` | saved checkups, filtered |
| GET | `/api/scans.csv?status=&days=` | the same as a CSV file |
| GET | `/api/plan?hours=24` | the optimization plan: what can be cleaned, plus advice (changes nothing) |
| GET | `/api/security` | warning signs of a compromised PC |
| POST | `/api/programs/close?name=` | close a program (only your own, never system programs) |
| POST | `/api/power?plan=balanced\|performance` | switch the power plan |
| POST | `/api/cleanup?what=temp\|thumbnails\|browser\|reports&hours=24` | remove old files, then measure again |
| GET | `/api/cleanups?days=` | saved cleanups |
| GET | `/api/startup` | startup programs, power plan, advice |
| GET PUT DELETE | `/api/limits` | read, change (validated) or reset the rule limits |
| POST | `/api/demo?mode=off\|healthy\|slow` | demo mode |

## Safety

- Needs no administrator rights. A program is only closed, or the power plan switched, when you press the button and confirm. System programs, other users' programs and this app are never closed. The startup list is never changed.
- Cleanup only touches **your own** files in temp, thumbnail, browser cache and crash report folders, older than 24 hours (setting), and never follows links. Documents, downloads and the recycle bin are never touched.
- The security check only reads. It reports warning signs, never claims a PC is hacked, and never closes or deletes anything.
- A missing number shows **Unavailable**, nothing is invented. Demo data is always labelled.
- Success is never assumed: the folder is measured again after cleaning.

More: [the rules](docs/RULES.md) and [a viva guide](docs/VIVA_GUIDE.md).
