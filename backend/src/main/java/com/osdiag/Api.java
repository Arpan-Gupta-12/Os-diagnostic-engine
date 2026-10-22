package com.osdiag;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** All the REST endpoints. Each one is a short function. */
@RestController
@RequestMapping("/api")
public class Api {

    private final Monitor monitor;
    private final ScanRepo scans;
    private final CleanupRepo cleanups;

    // The folder that gets cleaned: the OS temp folder (%TEMP% on Windows, /tmp on Linux).
    @Value("${app.temp-dir:#{systemProperties['java.io.tmpdir']}}")
    private String tempDir;

    public Api(Monitor monitor, ScanRepo scans, CleanupRepo cleanups) {
        this.monitor = monitor;
        this.scans = scans;
        this.cleanups = cleanups;
    }

    // ---- live data

    @GetMapping("/info")
    public Map<String, Object> info() {
        return monitor.info();
    }

    @GetMapping("/live")
    public List<Sample> live() {
        return monitor.history();
    }

    @GetMapping("/procs")
    public List<Proc> procs(@RequestParam(defaultValue = "") String search,
                            @RequestParam(defaultValue = "mem") String sort) {
        Comparator<Proc> order = sort.equals("cpu")
                ? Comparator.comparing((Proc p) -> p.cpu() == null ? -1 : p.cpu()).reversed()
                : Comparator.comparing(Proc::mem).reversed();
        return monitor.procs().stream()
                .filter(p -> p.name() != null && p.name().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))
                .sorted(order).limit(100).toList();
    }

    // ---- checkup

    @PostMapping("/scan")
    public Scan scan() throws InterruptedException {
        Sample now = monitor.fresh();
        List<Sample> recent = monitor.history();
        recent = recent.subList(Math.max(0, recent.size() - 10), recent.size());
        return scans.save(Checkup.check(now, recent, monitor.procs(), monitor.disks()));
    }

    @GetMapping("/scans")
    public List<Scan> scans() {
        return scans.findTop50ByOrderByTimeDesc();
    }

    // ---- cleanup

    /** Shows what a cleanup would remove. Changes nothing. */
    @GetMapping("/cleanup/preview")
    public Map<String, Object> preview(@RequestParam(defaultValue = "24") int hours) {
        Cleaner.Result r = Cleaner.preview(Path.of(tempDir).toAbsolutePath(), Duration.ofHours(hours));
        return Map.of("folder", tempDir, "hours", hours, "files", r.files(), "bytes", r.bytes());
    }

    /** Removes the old files, then measures again to check it really worked. */
    @PostMapping("/cleanup")
    public Cleanup cleanup(@RequestParam(defaultValue = "24") int hours) {
        Path dir = Path.of(tempDir).toAbsolutePath();
        Duration age = Duration.ofHours(hours);

        Cleanup c = new Cleanup();
        c.setTime(Instant.now());
        c.setFreeBefore(free(dir));
        c.setBytesBefore(Cleaner.preview(dir, age).bytes());

        Cleaner.Result r = Cleaner.clean(dir, age);
        c.setFiles(r.files());
        c.setBytes(r.bytes());
        c.setFailed(r.failed());

        // Never trust "the delete worked". Look at the folder again.
        c.setBytesAfter(Cleaner.preview(dir, age).bytes());
        c.setFreeAfter(free(dir));
        c.setVerified(r.files() > 0 && c.getBytesAfter() < c.getBytesBefore());
        c.setSummary(summary(c));
        return cleanups.save(c);
    }

    @GetMapping("/cleanups")
    public List<Cleanup> cleanups() {
        return cleanups.findTop20ByOrderByTimeDesc();
    }

    private static String summary(Cleanup c) {
        if (c.getBytesBefore() == 0) {
            return "There were no old temporary files, so nothing needed cleaning.";
        }
        if (c.isVerified()) {
            return String.format("Done! %d old files removed (%.1f MB freed). We checked the folder again and it is smaller.",
                    c.getFiles(), c.getBytes() / 1048576.0);
        }
        return c.getFailed() > 0
                ? "Some files could not be removed because they are in use or protected."
                : "The cleanup ran, but the folder did not get smaller, so we cannot confirm it worked.";
    }

    private static Long free(Path dir) {
        try {
            return Files.getFileStore(dir).getUsableSpace();
        } catch (Exception e) {
            return null;
        }
    }
}
