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

## Safety

- Needs no administrator rights. A program is only closed, or the power plan switched, when you press the button and confirm. System programs, other users' programs and this app are never closed. The startup list is never changed.
- Cleanup only touches **your own** files in temp, thumbnail, browser cache and crash report folders, older than 24 hours (setting), and never follows links. Documents, downloads and the recycle bin are never touched.
- The security check only reads. It reports warning signs, never claims a PC is hacked, and never closes or deletes anything.
- A missing number shows **Unavailable**, nothing is invented. Demo data is always labelled.
- Success is never assumed: the folder is measured again after cleaning.

More: [the rules](docs/RULES.md) and [a viva guide](docs/VIVA_GUIDE.md).
