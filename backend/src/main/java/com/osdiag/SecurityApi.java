package com.osdiag;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Looks for warning signs of a compromised PC. Read only. */
@RestController
@RequestMapping("/api")
public class SecurityApi {

    private final Monitor monitor;
    private final Demo demo;
    private final Recorder recorder;
    private final OsAdapter os = OsAdapter.current();

    public SecurityApi(Monitor monitor, Demo demo, Recorder recorder) {
        this.monitor = monitor;
        this.demo = demo;
        this.recorder = recorder;
    }

    @GetMapping("/security")
    public Map<String, Object> security() {
        List<Proc> procs = monitor.procs();
        List<OsAdapter.Startup> startup = new ArrayList<>(demo.on() ? demo.startup() : os.startupItems());
        List<OsAdapter.Startup> jobs = demo.on() ? demo.scheduledJobs() : os.scheduledJobs();
        startup.addAll(jobs);
        List<String> hosts = demo.on() ? demo.hosts() : readHosts();
        List<Listener> listeners = demo.on() ? demo.listeners() : monitor.listeners();
        List<Listener> outgoing = demo.on() ? demo.outgoing() : monitor.outgoing();
        Boolean antivirus = demo.on() ? demo.antivirus() : os.antivirusOn();

        List<Security.Indicator> found = Security.check(new Security.Input(procs, startup, hosts, listeners, outgoing, antivirus));

        List<String> checked = new ArrayList<>();
        checked.add(procs.size() + " running programs (where they run from, and look-alike names)");
        checked.add((startup.size() - jobs.size()) + " startup items and " + jobs.size() + " scheduled tasks or cron jobs");
        checked.add(hosts == null ? "Hosts file: could not be read" : "Hosts file: " + hosts.size() + " lines");
        checked.add("Open network ports: " + listeners.size() + " waiting for connections, " + outgoing.size() + " connections to other computers");
        checked.add(antivirus == null ? "Antivirus: cannot be checked on this PC" : "Antivirus protection: " + (antivirus ? "on" : "off"));

        Map<String, Object> m = new HashMap<>();
        m.put("indicators", found);
        m.put("checked", checked);
        m.put("summary", found.isEmpty()
                ? "No warning signs were found in the places we can check. This is not a virus scan."
                : found.size() + " warning sign(s) found. They are reasons to look closer, not proof of an attack.");
        return m;
    }

    /** The same check as GET, and the result is saved in the security history. The screen's polling never saves. */
    @PostMapping("/security/scan")
    @SuppressWarnings("unchecked")
    public Map<String, Object> scan() {
        Map<String, Object> m = security();
        SecurityScan saved = recorder.recordSecurity((String) m.get("summary"), (List<Security.Indicator>) m.get("indicators"));
        m.put("saved", saved != null);
        m.put("scanId", saved == null ? null : saved.getId());
        return m;
    }

    private List<String> readHosts() {
        try {
            return Files.readAllLines(os.hostsFile());
        } catch (Exception e) {
            return null;
        }
    }
}
