package com.osdiag;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Step 2 of Optimize: what could be done. Nothing here changes anything. */
@RestController
@RequestMapping("/api")
public class PlanApi {

    private static final String[][] CLEANABLE = {
            { "temp", "Old temporary files", "Left behind by installers and programs. Safe to remove." },
            { "thumbnails", "Picture preview cache", "Previews are rebuilt automatically when needed." },
            { "browser", "Browser caches", "Saved web pages and images. Websites load a little slower once, then it is rebuilt. "
                    + "Close the browser first for the best result." },
            { "reports", "Old crash reports", "Reports of programs that crashed earlier. Safe to remove." },
    };

    private static final Path HOME = Path.of(System.getProperty("user.home"));

    private final CleanApi clean;
    private final Monitor monitor;
    private final Demo demo;
    private final LimitsRepo limits;
    private final Recorder recorder;
    private final OsAdapter os = OsAdapter.current();

    public PlanApi(CleanApi clean, Monitor monitor, Demo demo, LimitsRepo limits, Recorder recorder) {
        this.clean = clean;
        this.monitor = monitor;
        this.demo = demo;
        this.limits = limits;
        this.recorder = recorder;
    }

    @GetMapping("/plan")
    public List<Plan.Action> plan(@RequestParam(defaultValue = "24") int hours, @RequestParam(required = false) Long scanId) {
        List<Plan.Action> actions = new ArrayList<>();
        for (String[] c : CLEANABLE) {
            Map<String, Object> found = clean.preview(c[0], hours);
            actions.add(Plan.clean(c[0], c[1], c[2], (int) found.get("files"), (long) found.get("bytes"),
                    (String) found.get("folder")));
        }
        List<Sample> recent = monitor.recent();
        Sample last = recent.isEmpty() ? null : recent.get(recent.size() - 1);
        List<OsAdapter.Startup> startup = demo.on() ? demo.startup() : os.startupItems();
        String power = demo.on() ? demo.power() : os.powerPlan();
        List<Large.Entry> big = demo.on() ? demoBig() : Large.find(List.of(HOME.resolve("Downloads"), HOME.resolve("Desktop")), 100L << 20, 5);
        actions.addAll(Plan.advice(last, monitor.procs(), limits.get(), startup, power, big));
        recorder.recordPlan(scanId, actions);
        return actions;
    }

    private static List<Large.Entry> demoBig() {
        return List.of(new Large.Entry("C:\\Users\\demo\\Downloads\\old-movie.mkv", 4L << 30, java.time.Instant.parse("2025-03-02T10:00:00Z")),
                new Large.Entry("C:\\Users\\demo\\Downloads\\installer-backup.iso", 2L << 30, java.time.Instant.parse("2025-06-10T10:00:00Z")));
    }
}
