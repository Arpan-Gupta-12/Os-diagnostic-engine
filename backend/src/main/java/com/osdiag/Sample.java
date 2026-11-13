package com.osdiag;

import java.time.Instant;

/** One measurement. null means it could not be measured, and the screen shows Unavailable. */
public record Sample(Instant time, Double cpu, Double mem, Long memFree, Long memTotal, Double paging, Integer procs,
                     Double disk, Double ioWait, Double temp) {
}
