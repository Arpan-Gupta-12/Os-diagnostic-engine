# Diagnosis rules

All rules are small functions in `backend/src/main/java/com/osdiag/Checkup.java`.
The limits are in `Limits.java` (defaults below) and can be changed in Settings.

A load is **sustained** when at least 3 recent samples exist and at least 80% of them are at or above the limit. One spike never decides.

| Rule | Fires when | Points lost | Why |
|------|-----------|-------------|-----|
| CPU busy | CPU is at least 85% for most of the last ~30 s | 20 | A busy processor makes everything wait. The busiest program is named if it uses 25% or more. |
| Memory | RAM use is at least 85% **and** paging is at least 100 pages/s | 20 (35 if RAM is 95%+) | High RAM alone is normal (the OS keeps spare RAM as cache). It only hurts when the PC starts using the slow disk as memory. |
| Big program | One program (all its processes added up) uses 25% of RAM or more | 10 | Points at the program worth looking at. The app never closes it. |
| Drive almost full | A drive over 1 GB has less than 10% free | 20 (35 if under 5%) | With no free space the OS cannot create temp files or install updates. |
| Disk busy | The busiest disk is at least 90% busy for most of the last ~30 s | 15 | Programs wait for the disk, so everything feels slow. |
| Hot processor | The processor is at least 90 °C | 10 | Hot processors slow themselves down. Often dust or blocked vents. |

A measurement that is missing (for example temperature on many PCs) never fires a rule and is shown as Unavailable.

## Result

- **Score** = 100 minus the points lost (never below 0). No score is shown if CPU or memory could not be measured.
- **Status**: Healthy (nothing lost), Needs attention (some points lost), Critical (30 or more lost), Unknown (no CPU or memory data).

# Security warning signs

Functions in `Security.java`. Each gives a level: **Warning** (more suspicious) or **Check** (often harmless). None of them is proof.

| Sign | Level | Why it matters |
|------|-------|----------------|
| A program runs from a temp, Downloads or recycle bin folder | Check | Properly installed programs live in Program Files. Malware often runs from temp folders. |
| A program has a system name (svchost, lsass, ...) but runs outside the Windows folder | Warning | Malware copies the names of system programs. |
| A program name is one letter away from a system name (svch0st) | Warning | The same trick, with a look-alike name. |
| A startup item runs an encoded or hidden command, or mshta | Warning | Used to start malware without being noticed. |
| A startup item runs a script or starts from a temp folder | Check | Unusual for normal programs. |
| A scheduled task or cron job looks like an unusual startup item | Warning / Check | A common way to start malware again and again. Windows' own tasks are skipped. |
| A connection to port 4444, 6667, 1337 or 31337 of another computer | Warning | Ports known for attack tools and botnets. |
| The hosts file sends a website name to another address | Warning | Can lead you to a fake website. A line sending to 0.0.0.0 only blocks a site and is ignored. |
| Telnet (23), Remote Desktop (3389), VNC (5900) or port 4444 is open to other computers | Check (4444: Warning) | Fine if you use it. Risky if you do not know about it. |
| Windows Defender real-time protection is off | Warning | Normal if another antivirus is installed. |

Programs whose path the OS does not reveal (protected system programs) are never accused.

# Optimization plan

`Plan.java` and `PlanApi.java`. Cleaning actions need your approval. Advice and "not done" items only explain.

| Item | Kind | What it covers |
|------|------|----------------|
| Old temporary files, thumbnails, browser caches, crash reports | Cleaning (approval) | Your own files older than the minimum age. Measured again after cleaning. |
| Startup programs | Advice | Lists them. Never changes them. |
| Close a heavy program | Button + confirm | Only your own programs, never system programs, this app or what started it. The result says how much memory came back and whether the program is really gone. |
| Switch power plan | Button + confirm | Balanced or High performance. The result says what it changed from and to. |
| Big files | Advice | The 5 biggest files over 100 MB in Downloads and Desktop. Never deleted. |
| Heavy programs | Advice | Only when CPU or memory is high. Names the top users. Never closes them. |
| Power plan | Advice | Explains a power-saving plan. |
| Forcing the system to free memory | Not done | Needs administrator rights and only helps for a moment. |
| Hardware checks | Advice | Only when the disk is very busy or the processor is hot. |
