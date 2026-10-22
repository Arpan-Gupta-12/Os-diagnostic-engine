package com.osdiag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class CheckupTest {

    static final long GB = 1024L * 1024 * 1024;

    static Sample sample(Double cpu, Double mem, Double paging) {
        return new Sample(Instant.now(), cpu, mem, 1 * GB, 8 * GB, paging, 200);
    }

    /** Ten identical samples, so the load counts as "sustained". */
    static Scan check(Sample s, List<Proc> procs, List<Checkup.Disk> disks) {
        return Checkup.check(s, Collections.nCopies(10, s), procs, disks);
    }

    @Test
    void healthyPc() {
        Scan scan = check(sample(20.0, 50.0, 0.0), List.of(), List.of());
        assertEquals("Healthy", scan.getStatus());
        assertEquals(100, scan.getScore());
    }

    @Test
    void sustainedCpuNamesTheBusyProgram() {
        Proc busy = new Proc(1, "render", 60.0, GB / 10, "me");
        Scan scan = check(sample(90.0, 40.0, 0.0), List.of(busy), List.of());
        assertEquals("Needs attention", scan.getStatus());
        assertEquals(80, scan.getScore());
        assertTrue(scan.getProblems().get(0).contains("render"));
    }

    @Test
    void oneSpikeIsNotAProblem() {
        Sample calm = sample(20.0, 40.0, 0.0);
        List<Sample> recent = new java.util.ArrayList<>(Collections.nCopies(9, calm));
        Sample spike = sample(99.0, 40.0, 0.0);
        recent.add(spike);
        assertEquals("Healthy", Checkup.check(spike, recent, List.of(), List.of()).getStatus());
    }

    @Test
    void highMemoryNeedsPaging() {
        assertEquals("Healthy", check(sample(20.0, 90.0, 5.0), List.of(), List.of()).getStatus());
        Scan scan = check(sample(20.0, 90.0, 400.0), List.of(), List.of());
        assertEquals(1, scan.getProblems().size());
        assertTrue(scan.getProblems().get(0).startsWith("Memory is almost full"));
    }

    @Test
    void oneProgramWithManyProcessesIsAdded() {
        List<Proc> chrome = Collections.nCopies(10, new Proc(1, "chrome", 1.0, GB / 4, "me"));
        Scan scan = check(sample(20.0, 50.0, 0.0), chrome, List.of());
        assertTrue(scan.getProblems().get(0).contains("'chrome'"));
    }

    @Test
    void fullDriveIsCritical() {
        Scan scan = check(sample(20.0, 50.0, 0.0), List.of(), List.of(new Checkup.Disk("C:\\", 256 * GB, 4 * GB)));
        assertEquals("Critical", scan.getStatus());
    }

    @Test
    void neverHealthyWithoutCpuAndMemory() {
        Scan scan = check(sample(null, null, null), List.of(), List.of());
        assertEquals("Unknown", scan.getStatus());
        assertNull(scan.getScore());
    }
}
