package com.osdiag;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Saved history, newest first. Real measurements only: demo mode is never saved here. */
@RestController
@RequestMapping("/api/history")
public class HistoryApi {

    private final PerformanceSampleRepo samples;
    private final SecurityScanRepo securityScans;
    private final DiskSnapshotRepo diskSnapshots;
    private final StartupSnapshotRepo startupSnapshots;
    private final SystemActionLogRepo actions;
    private final OptimizationRunRepo runs;

    public HistoryApi(PerformanceSampleRepo samples, SecurityScanRepo securityScans, DiskSnapshotRepo diskSnapshots,
                      StartupSnapshotRepo startupSnapshots, SystemActionLogRepo actions, OptimizationRunRepo runs) {
        this.samples = samples;
        this.securityScans = securityScans;
        this.diskSnapshots = diskSnapshots;
        this.startupSnapshots = startupSnapshots;
        this.actions = actions;
        this.runs = runs;
    }

    @GetMapping("/performance")
    public List<PerformanceSample> performance(@RequestParam(defaultValue = "24") int hours,
                                               @RequestParam(defaultValue = "500") int limit) {
        return samples.findByTimeAfterOrderByTimeDesc(since(Duration.ofHours(hours)), page(limit, 5000));
    }

    @GetMapping("/security")
    public List<SecurityScan> security(@RequestParam(defaultValue = "30") int days, @RequestParam(defaultValue = "50") int limit) {
        return securityScans.findByScanTimeAfterOrderByScanTimeDesc(since(Duration.ofDays(days)), page(limit, 200));
    }

    @GetMapping("/disks")
    public List<DiskSnapshot> disks(@RequestParam(defaultValue = "30") int days, @RequestParam(defaultValue = "200") int limit) {
        return diskSnapshots.findByCapturedAtAfterOrderByCapturedAtDesc(since(Duration.ofDays(days)), page(limit, 1000));
    }

    @GetMapping("/startup")
    public List<StartupSnapshot> startup(@RequestParam(defaultValue = "90") int days, @RequestParam(defaultValue = "20") int limit) {
        return startupSnapshots.findByCapturedAtAfterOrderByCapturedAtDesc(since(Duration.ofDays(days)), page(limit, 100));
    }

    @GetMapping("/actions")
    public List<SystemActionLog> actions(@RequestParam(defaultValue = "90") int days, @RequestParam(defaultValue = "100") int limit) {
        return actions.findByExecutionTimeAfterOrderByExecutionTimeDesc(since(Duration.ofDays(days)), page(limit, 500));
    }

    @GetMapping("/optimizations")
    public List<OptimizationRun> optimizations(@RequestParam(defaultValue = "90") int days,
                                               @RequestParam(defaultValue = "50") int limit) {
        return runs.findByStartedAtAfterOrderByStartedAtDesc(since(Duration.ofDays(days)), page(limit, 200));
    }

    private static Instant since(Duration age) {
        return Instant.now().minus(age.isNegative() || age.isZero() ? Duration.ofHours(1) : age);
    }

    private static Pageable page(int limit, int max) {
        return PageRequest.of(0, Math.max(1, Math.min(limit, max)));
    }
}
