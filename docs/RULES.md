# Diagnosis rules

All rules are small functions in `backend/src/main/java/com/osdiag/Checkup.java`. The limits are constants at the top of that file.

A load is **sustained** when at least 3 recent samples exist and at least 80% of them are at or above the limit. One spike never decides.

| Rule | Fires when | Points lost | Why |
|------|-----------|-------------|-----|
| CPU busy | CPU is at least 85% for most of the last ~30 s | 20 | A busy processor makes everything wait. The busiest program is named if it uses 25% or more. |
| Memory | RAM use is at least 85% **and** paging is at least 100 pages/s | 20 (35 if RAM is 95%+) | High RAM alone is normal (the OS keeps spare RAM as cache). It only hurts when the PC starts using the slow disk as memory. |
| Big program | One program (all its processes added up) uses 25% of RAM or more | 10 | Points at the program worth looking at. The app never closes it. |
| Drive almost full | A drive over 1 GB has less than 10% free | 20 (35 if under 5%) | With no free space the OS cannot create temp files or install updates. |

## Result

- **Score** = 100 minus the points lost (never below 0). No score is shown if CPU or memory could not be measured.
- **Status**: Healthy (nothing lost), Needs attention (some points lost), Critical (30 or more lost), Unknown (no CPU or memory data).
