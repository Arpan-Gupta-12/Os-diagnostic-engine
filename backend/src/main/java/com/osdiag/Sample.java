package com.osdiag;

import java.time.Instant;

public record Sample(Instant time, Double cpu, Double mem, Long memFree, Long memTotal, Double paging, Integer procs) {
}
