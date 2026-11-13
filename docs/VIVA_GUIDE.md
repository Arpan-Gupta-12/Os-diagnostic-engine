# Viva guide

## 15 minute demo

| Step | Do | Say |
|------|----|-----|
| 1 | Run `scripts\run.bat` | "One Spring Boot jar serves the React screen and the REST API." |
| 2 | Watch the intro | "A PC screen drawn in SVG wakes up, smiles and zooms into the home screen. Pure CSS keyframes, about 3 seconds, skippable, can be switched off in Settings." |
| 3 | Overview | "A checkup measures three quick samples and runs 6 simple rules. The score is 100 minus points per problem." |
| 4 | Live Diagnostics | "OSHI gives counters that only go up. We keep the last reading and show the rate. If a number is missing the chart says Unavailable." |
| 5 | Processes | "Read only on purpose. Closing a program could lose someone's unsaved work." |
| 6 | Optimize PC | "Check, plan, clean, verify. Temp files and the thumbnail cache. Nothing is deleted until I press Clean now." |
| 7 | Report | "We measure the folder again after cleaning. We only say it worked if it really got smaller." |
| 8 | History | "Saved with Spring Data JPA, so it survives a restart." Run `SELECT * FROM scans ORDER BY id DESC LIMIT 3;` |
| 9 | Security | "Indicators of compromise: where programs run from, look-alike names, hidden startup commands, the hosts file, open remote-access ports. It reports warning signs and says clearly that it is not proof or a virus scan." |
| 9b | Startup & Power | "Read only advice. Changing startup items or power plans needs the user's own decision." |
| 10 | Settings | "The limits are saved in the database and validated. Demo mode shows made-up data, clearly labelled, and deletes nothing." |
| 11 | Tests | `mvn test` (44 tests) and `npm test` (7 tests). |

## Architecture in one breath

`React page -> api.js -> a *Api class -> Monitor / Checkup / Cleaner -> database`.
`Monitor` reads numbers, `Checkup` decides (pure Java, easy to test), `Cleaner` deletes safely, the `*Api` classes only pass calls on.
`OsAdapter` (with `WindowsOs` and `LinuxOs`) holds the few things that differ between the two systems.

## Likely questions

**Why OSHI?** One Java library reads CPU, memory and processes on Windows and Linux.

**How is CPU % calculated?** The OS counts time spent busy and idle (ticks). We read it twice: busy difference / total difference.

**What is paging and why does it matter?** When RAM is full the OS moves memory to the disk, which is thousands of times slower. So the memory rule needs high RAM use and paging.

**Why not trust one reading?** One reading can be a spike. Rules look at the last ~30 seconds and need 80% of samples over the limit.

**How is cleanup safe?** It only visits the temp folder (never home or a drive root), removes regular files only, never follows links, only your own files, only files older than 24 hours. Locked files are skipped and counted.

**Why don't you kill heavy programs?** The user could lose unsaved work. We name the program and explain what to do.

**What if a number is missing?** The backend sends `null`, the screen shows Unavailable. Without CPU and memory the PC is never called healthy.

**How do you know the cleanup worked?** `CleanApi.cleanup` measures the old files before and after. `verified` is true only if files were removed and the folder got smaller.

**Why a monolith?** One user, one PC, one process. Microservices would only add complexity.

**How do I add a rule?** Add a small function to `Checkup`, call it from `check()`, add a test in `CheckupTest`.

**How does it work on Windows and Linux?** OSHI hides most differences. The rest is in `WindowsOs` (registry, `powercfg`, Explorer thumbnail cache) and `LinuxOs` (autostart files, power profile, `~/.cache/thumbnails`).

**What is paging, disk busy and I/O wait?** Disk busy is the share of time the disk was working. I/O wait is the time the processor sat idle waiting for the disk. Windows does not report I/O wait, so it shows Unavailable there.

**Why can the limits be changed?** Every PC is different. The limits are one row in the database, validated with Bean Validation, and the rules read them on every checkup.

**What are indicators of compromise?** Signs that often appear on a hacked PC, for example a program called svch0st running from a temp folder. One sign is not proof, so each result says what it means and what to do.

**Is it safe to close programs from the app?** Only on a button press plus a confirmation that warns about unsaved work. Only the user's own programs can be closed, never system programs (a fixed list), this app or what started it. Afterwards the app checks whether the program is really gone and shows the memory before and after.

**What is demo mode for?** Showing the app on a PC without problems. Its numbers are made up, labelled in the banner, saved separately from real history, and it never touches a file.
