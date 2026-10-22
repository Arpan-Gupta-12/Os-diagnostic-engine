# Viva guide

## 10 minute demo

| Step | Do | Say |
|------|----|-----|
| 1 | Run `scripts\run.bat` | "One Spring Boot jar serves the React screen and the REST API." |
| 2 | Watch the intro | "Pure SVG and CSS keyframes, about 3 seconds, skippable, can be switched off in Settings." |
| 3 | Overview | "A checkup measures three quick samples and runs 4 simple rules. The score is 100 minus points per problem." |
| 4 | Live Diagnostics | "OSHI gives counters that only go up. We keep the last reading and show the rate. If a number is missing the chart says Unavailable." |
| 5 | Processes | "Read only on purpose. Closing a program could lose someone's unsaved work." |
| 6 | Optimize PC | "Check, plan, clean, verify. Nothing is deleted until I press Clean now." |
| 7 | Report | "We measure the folder again after cleaning. We only say it worked if it really got smaller." |
| 8 | History | "Saved with Spring Data JPA, so it survives a restart." Run `SELECT * FROM scans ORDER BY id DESC LIMIT 3;` |
| 9 | Tests | `mvn test` (13 tests) and `npm test` (6 tests). |

## Architecture in one breath

`React page -> api.js -> Api -> Monitor / Checkup / Cleaner -> database`.
`Monitor` reads numbers, `Checkup` decides (pure Java, easy to test), `Cleaner` deletes safely, `Api` only passes calls on.

## Likely questions

**Why OSHI?** One Java library reads CPU, memory and processes on Windows and Linux.

**How is CPU % calculated?** The OS counts time spent busy and idle (ticks). We read it twice: busy difference / total difference.

**What is paging and why does it matter?** When RAM is full the OS moves memory to the disk, which is thousands of times slower. So the memory rule needs high RAM use and paging.

**Why not trust one reading?** One reading can be a spike. Rules look at the last ~30 seconds and need 80% of samples over the limit.

**How is cleanup safe?** It only visits the temp folder (never home or a drive root), removes regular files only, never follows links, only your own files, only files older than 24 hours. Locked files are skipped and counted.

**Why don't you kill heavy programs?** The user could lose unsaved work. We name the program and explain what to do.

**What if a number is missing?** The backend sends `null`, the screen shows Unavailable. Without CPU and memory the PC is never called healthy.

**How do you know the cleanup worked?** `Api.cleanup` measures the old files before and after. `verified` is true only if files were removed and the folder got smaller.

**Why a monolith?** One user, one PC, one process. Microservices would only add complexity.

**How do I add a rule?** Add a small function to `Checkup`, call it from `check()`, add a test in `CheckupTest`.

**What is not in this version?** See "Part 2" in the README.
