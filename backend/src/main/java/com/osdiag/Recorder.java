package com.osdiag;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Saves history in MySQL. Every method skips demo mode, so invented numbers never reach the history tables, and a
 * database problem is logged instead of breaking the feature that called it. Each aggregate (a security scan with its
 * findings, a startup snapshot with its entries) is written with one save, which is one transaction.
 */
@Service
public class Recorder {

    private static final Logger log = LoggerFactory.getLogger(Recorder.class);

    private final Monitor monitor;
    private final Demo demo;
    private final ScanRepo scans;
    private final PerformanceSampleRepo samples;
    private final SecurityScanRepo securityScans;
    private final DiskSnapshotRepo diskSnapshots;
    private final StartupSnapshotRepo startupSnapshots;
    private final SystemActionLogRepo actions;
    private final OptimizationRunRepo runs;

    @Value("${app.history.retention-days:30}")
    private int retentionDays;

    private volatile Instant lastSampleSaved = Instant.EPOCH;

    public Recorder(Monitor monitor, Demo demo, ScanRepo scans, PerformanceSampleRepo samples, SecurityScanRepo securityScans,
                    DiskSnapshotRepo diskSnapshots, StartupSnapshotRepo startupSnapshots, SystemActionLogRepo actions,
                    OptimizationRunRepo runs) {
        this.monitor = monitor;
        this.demo = demo;
        this.scans = scans;
        this.samples = samples;
        this.securityScans = securityScans;
        this.diskSnapshots = diskSnapshots;
        this.startupSnapshots = startupSnapshots;
        this.actions = actions;
        this.runs = runs;
    }

    /** The live screen reads every 3 seconds. The database gets the newest sample only every app.history.sample-interval-ms. */
    @Scheduled(fixedDelayString = "${app.history.sample-interval-ms:30000}", initialDelay = 10000)
    public void saveSample() {
        List<Sample> recent = monitor.history();
        if (demo.on() || recent.isEmpty()) {
            return;
        }
        Sample last = recent.get(recent.size() - 1);
        if (!last.time().isAfter(lastSampleSaved)) {
            return;
        }
        try {
            samples.save(PerformanceSample.from(last));
            lastSampleSaved = last.time();
        } catch (DataAccessException e) {
            log.warn("Could not save the performance sample", e);
        }
    }

    /** Only the detailed measurements are removed. Scans, cleanups and security history are kept. */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)
    public void deleteOldSamples() {
        if (retentionDays <= 0) {
            return;
        }
        try {
            long removed = samples.deleteByTimeBefore(Instant.now().minus(Duration.ofDays(retentionDays)));
            if (removed > 0) {
                log.info("Removed {} performance samples older than {} days", removed, retentionDays);
            }
        } catch (DataAccessException e) {
            log.warn("Could not remove old performance samples", e);
        }
    }

    public void recordDisks(Scan scan, List<Disk> disks) {
        if (demo.on() || disks.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        try {
            diskSnapshots.saveAll(disks.stream().map(d -> DiskSnapshot.of(scan, d, now)).toList());
        } catch (DataAccessException e) {
            log.warn("Could not save the disk snapshots", e);
        }
    }

    /** Returns the saved scan, or null when nothing was saved. */
    public SecurityScan recordSecurity(String summary, List<Security.Indicator> found) {
        if (demo.on()) {
            return null;
        }
        SecurityScan scan = new SecurityScan();
        scan.setScanTime(Instant.now());
        scan.setSummary(cut(summary, 500));
        scan.setTotalFindings(found.size());
        for (Security.Indicator i : found) {
            SecurityFinding f = new SecurityFinding();
            f.setSecurityScan(scan);
            f.setLevel(cut(i.level(), 20));
            f.setTitle(cut(i.title(), 300));
            f.setText(cut(i.text(), 2000));
            scan.getFindings().add(f);
        }
        try {
            return securityScans.save(scan);
        } catch (DataAccessException e) {
            log.warn("Could not save the security scan", e);
            return null;
        }
    }

    /**
     * Saves the startup programs and power plan. Unless always is true this only happens when they differ from the latest
     * snapshot. Nothing is saved when the PC reported neither, because that means it could not be read, not that it is empty.
     */
    public boolean recordStartup(List<OsAdapter.Startup> items, String power, boolean always) {
        if (demo.on() || (items.isEmpty() && power == null)) {
            return false;
        }
        String hash = fingerprint(items, power);
        try {
            if (!always && startupSnapshots.findFirstByOrderByCapturedAtDesc().map(s -> s.getContentHash().equals(hash)).orElse(false)) {
                return false;
            }
            StartupSnapshot snapshot = new StartupSnapshot();
            snapshot.setCapturedAt(Instant.now());
            snapshot.setPowerPlan(cut(power, 100));
            snapshot.setContentHash(hash);
            for (OsAdapter.Startup s : items) {
                StartupEntry entry = new StartupEntry();
                entry.setSnapshot(snapshot);
                entry.setName(cut(s.name(), 255));
                entry.setCommand(cut(s.command(), 1000));
                entry.setLocation(cut(s.where(), 100));
                snapshot.getEntries().add(entry);
            }
            startupSnapshots.save(snapshot);
            return true;
        } catch (DataAccessException e) {
            log.warn("Could not save the startup snapshot", e);
            return false;
        }
    }

    /** Call this only for something that was really tried, and set success from what was measured afterwards. */
    public void recordAction(String actionType, String target, String previousValue, String newValue, boolean success,
                             String resultMessage) {
        if (demo.on()) {
            return;
        }
        SystemActionLog entry = new SystemActionLog();
        entry.setActionType(actionType);
        entry.setTarget(cut(target, 255));
        entry.setPreviousValue(cut(previousValue, 255));
        entry.setNewValue(cut(newValue, 255));
        entry.setExecutionTime(Instant.now());
        entry.setSuccess(success);
        entry.setResultMessage(cut(resultMessage, 500));
        try {
            actions.save(entry);
        } catch (DataAccessException e) {
            log.warn("Could not save the action log", e);
        }
    }

    /** Step 2 of Optimize: remembers which cleanups were offered after the checkup with this id. Once per checkup. */
    public void recordPlan(Long scanId, List<Plan.Action> plan) {
        if (demo.on() || scanId == null) {
            return;
        }
        try {
            Scan scan = scans.findById(scanId).orElse(null);
            if (scan == null || scan.isDemo() || runs.findFirstByScanIdOrderByStartedAtDesc(scanId).isPresent()) {
                return;
            }
            OptimizationRun run = new OptimizationRun();
            run.setScan(scan);
            run.setStartedAt(Instant.now());
            for (Plan.Action a : plan) {
                if (a.kind().equals("clean")) {
                    OptimizationAction action = new OptimizationAction();
                    action.setRun(run);
                    action.setActionKey(a.key());
                    action.setPlannedFiles(a.files());
                    action.setPlannedBytes(a.bytes());
                    run.getActions().add(action);
                }
            }
            runs.save(run);
        } catch (DataAccessException e) {
            log.warn("Could not save the optimization run", e);
        }
    }

    /** Step 3 of Optimize: connects a finished cleanup to the offer it came from. */
    public void recordCleanup(Long scanId, Cleanup cleanup) {
        if (demo.on() || scanId == null) {
            return;
        }
        try {
            runs.findFirstByScanIdOrderByStartedAtDesc(scanId).ifPresent(run -> {
                run.getActions().stream().filter(a -> a.getActionKey().equals(cleanup.getWhat())).findFirst().ifPresent(a -> {
                    a.setSelected(true);
                    a.setCleanup(cleanup);
                });
                run.setFinishedAt(Instant.now());
                runs.save(run);
            });
        } catch (DataAccessException e) {
            log.warn("Could not link the cleanup to its optimization run", e);
        }
    }

    private static String fingerprint(List<OsAdapter.Startup> items, String power) {
        List<String> lines = new ArrayList<>(items.stream().map(s -> s.name() + "|" + s.command() + "|" + s.where()).toList());
        lines.sort(null);
        lines.add("power=" + power);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String cut(String text, int max) {
        return text == null || text.length() <= max ? text : text.substring(0, max);
    }
}
