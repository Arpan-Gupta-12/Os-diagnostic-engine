package com.osdiag;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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

    public CheckupApi(Monitor monitor, Demo demo, ScanRepo scans, LimitsRepo limits) {
        this.monitor = monitor;
        this.demo = demo;
        this.scans = scans;
        this.limits = limits;
    }

    @PostMapping("/scan")
    public Scan scan() throws InterruptedException {
        Sample now = monitor.fresh();
        Scan scan = Checkup.check(now, monitor.recent(), monitor.procs(), monitor.disks(), limits.get());
        scan.setDemo(demo.on());
        return scans.save(scan);
    }

    @GetMapping("/scans")
    public List<Scan> scans(@RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "0") int days) {
        Instant since = days > 0 ? Instant.now().minus(Duration.ofDays(days)) : Instant.MIN;
        return scans.findByDemoOrderByTimeDesc(demo.on()).stream()
                .filter(s -> status.isEmpty() || s.getStatus().equals(status))
                .filter(s -> s.getTime().isAfter(since))
                .limit(200).toList();
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
