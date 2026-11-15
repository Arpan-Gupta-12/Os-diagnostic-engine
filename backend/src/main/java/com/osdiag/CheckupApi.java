package com.osdiag;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CheckupApi {

    private final Monitor monitor;
    private final Demo demo;
    private final ScanRepo scans;
    private final LimitsRepo limits;
    private final Recorder recorder;

    public CheckupApi(Monitor monitor, Demo demo, ScanRepo scans, LimitsRepo limits, Recorder recorder) {
        this.monitor = monitor;
        this.demo = demo;
        this.scans = scans;
        this.limits = limits;
        this.recorder = recorder;
    }

    @PostMapping("/scan")
    public Scan scan() throws InterruptedException {
        Sample now = monitor.fresh();
        List<Disk> disks = monitor.disks();
        Scan scan = Checkup.check(now, monitor.recent(), monitor.procs(), disks, limits.get());
        scan.setDemo(demo.on());
        Scan saved = scans.save(scan);
        recorder.recordDisks(saved, disks);
        return saved;
    }

    @GetMapping("/scans")
    public List<Scan> scans(@RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "0") int days) {
        Instant since = days > 0 ? Instant.now().minus(Duration.ofDays(days)) : Instant.EPOCH;
        PageRequest latest = PageRequest.of(0, 200);
        return status.isEmpty() ? scans.findByDemoAndTimeAfterOrderByTimeDesc(demo.on(), since, latest)
                : scans.findByDemoAndStatusAndTimeAfterOrderByTimeDesc(demo.on(), status, since, latest);
    }

    @GetMapping("/scans.csv")
    public ResponseEntity<String> csv(@RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "0") int days) {
        StringBuilder out = new StringBuilder("time,status,score,cpu,memory,summary,problems\n");
        for (Scan s : scans(status, days)) {
            out.append(String.join(",", s.getTime().toString(), s.getStatus(), String.valueOf(s.getScore()),
                    String.valueOf(s.getCpu()), String.valueOf(s.getMem()), quote(s.getSummary()),
                    quote(String.join(" | ", s.getProblems())))).append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=checkups.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(out.toString());
    }

    static String quote(String text) {
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
