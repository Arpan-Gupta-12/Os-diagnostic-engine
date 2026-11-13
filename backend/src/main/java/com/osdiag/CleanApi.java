package com.osdiag;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Cleaning targets: temp (temp folder), thumbnails (picture previews), browser (browser caches), reports (crash reports). */
@RestController
@RequestMapping("/api")
public class CleanApi {

    record Target(String name, List<Path> dirs, String prefix) {
    }

    private final Demo demo;
    private final CleanupRepo cleanups;
    private final OsAdapter os = OsAdapter.current();

    @Value("${app.temp-dir:#{systemProperties['java.io.tmpdir']}}")
    private String tempDir;

    @Value("${app.thumbnail-dir:}")
    private String thumbnailDir;

    public CleanApi(Demo demo, CleanupRepo cleanups) {
        this.demo = demo;
        this.cleanups = cleanups;
    }

    @GetMapping("/cleanup/preview")
    public Map<String, Object> preview(@RequestParam(defaultValue = "temp") String what,
                                       @RequestParam(defaultValue = "24") int hours) {
        if (demo.on()) {
            return Map.of("folder", "(demo)", "hours", hours, "files", demoFiles(what), "bytes", demoBytes(what));
        }
        Target t = target(what);
        Cleaner.Result r = preview(t, Duration.ofHours(hours));
        return Map.of("folder", t.dirs().isEmpty() ? "(not found on this PC)" : t.dirs().get(0).toString(), "hours", hours,
                "files", r.files(), "bytes", r.bytes());
    }

    @PostMapping("/cleanup")
    public Cleanup cleanup(@RequestParam(defaultValue = "temp") String what, @RequestParam(defaultValue = "24") int hours) {
        Cleanup c = demo.on() ? pretend(what) : clean(target(what), Duration.ofHours(hours));
        c.setTime(Instant.now());
        c.setWhat(what);
        c.setDemo(demo.on());
        c.setSummary(summary(c));
        return cleanups.save(c);
    }

    @GetMapping("/cleanups")
    public List<Cleanup> cleanups(@RequestParam(defaultValue = "0") int days) {
        Instant since = days > 0 ? Instant.now().minus(Duration.ofDays(days)) : Instant.MIN;
        return cleanups.findByDemoOrderByTimeDesc(demo.on()).stream()
                .filter(c -> c.getTime().isAfter(since)).limit(100).toList();
    }

    /** Removes the files, then measures the folders again, because "the delete worked" is never taken on trust. */
    private Cleanup clean(Target t, Duration age) {
        Cleanup c = new Cleanup();
        c.setFreeBefore(free(t.dirs()));
        c.setBytesBefore(preview(t, age).bytes());

        Cleaner.Result r = new Cleaner.Result(0, 0, 0);
        for (Path dir : t.dirs()) {
            r = r.plus(Cleaner.clean(dir, age, t.prefix()));
        }
        c.setFiles(r.files());
        c.setBytes(r.bytes());
        c.setFailed(r.failed());

        c.setBytesAfter(preview(t, age).bytes());
        c.setFreeAfter(free(t.dirs()));
        c.setVerified(r.files() > 0 && c.getBytesAfter() < c.getBytesBefore());
        return c;
    }

    private static Cleaner.Result preview(Target t, Duration age) {
        Cleaner.Result r = new Cleaner.Result(0, 0, 0);
        for (Path dir : t.dirs()) {
            r = r.plus(Cleaner.preview(dir, age, t.prefix()));
        }
        return r;
    }

    /** Demo mode: invented numbers, nothing on the disk is touched. */
    private Cleanup pretend(String what) {
        Cleanup c = new Cleanup();
        c.setFiles(demoFiles(what));
        c.setBytes(demoBytes(what));
        c.setBytesBefore(demoBytes(what));
        c.setBytesAfter(0);
        c.setFreeBefore(40L << 30);
        c.setFreeAfter((40L << 30) + demoBytes(what));
        c.setVerified(true);
        return c;
    }

    private Target target(String what) {
        return switch (what) {
            case "temp" -> new Target(what, List.of(Path.of(tempDir).toAbsolutePath()), "");
            case "thumbnails" -> thumbnails();
            case "browser" -> new Target(what, os.browserCaches(), "");
            case "reports" -> new Target(what, os.reportDirs(), "");
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown cleanup: " + what);
        };
    }

    private Target thumbnails() {
        if (!thumbnailDir.isEmpty()) {
            return new Target("thumbnails", List.of(Path.of(thumbnailDir).toAbsolutePath()), "");
        }
        Path dir = os.thumbnailDir();
        return new Target("thumbnails", dir == null ? List.of() : List.of(dir), os.thumbnailPrefix());
    }

    private static int demoFiles(String what) {
        return switch (what) {
            case "temp" -> 214;
            case "thumbnails" -> 40;
            case "browser" -> 520;
            default -> 6;
        };
    }

    private static long demoBytes(String what) {
        return switch (what) {
            case "temp" -> 188L << 20;
            case "thumbnails" -> 35L << 20;
            case "browser" -> 310L << 20;
            default -> 12L << 20;
        };
    }

    private static String summary(Cleanup c) {
        String kind = switch (c.getWhat()) {
            case "temp" -> "old temporary files";
            case "thumbnails" -> "thumbnail cache files";
            case "browser" -> "browser cache files";
            default -> "old crash reports";
        };
        if (c.isDemo()) {
            return String.format("Demo: %d %s would be removed (%.1f MB). Nothing was really removed.",
                    c.getFiles(), kind, c.getBytes() / 1048576.0);
        }
        if (c.getBytesBefore() == 0) {
            return "There were no " + kind + " to remove.";
        }
        if (c.isVerified()) {
            return String.format("Done! %d %s removed (%.1f MB freed). We checked the folder again and it is smaller.",
                    c.getFiles(), kind, c.getBytes() / 1048576.0);
        }
        return c.getFailed() > 0
                ? "Some " + kind + " could not be removed because they are in use or protected."
                : "The cleanup ran, but the folder did not get smaller, so we cannot confirm it worked.";
    }

    private static Long free(List<Path> dirs) {
        try {
            return Files.getFileStore(dirs.get(0)).getUsableSpace();
        } catch (Exception e) {
            return null;
        }
    }
}
