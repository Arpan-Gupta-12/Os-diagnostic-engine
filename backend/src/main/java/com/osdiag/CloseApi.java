package com.osdiag;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Closes a program, but only when the user asks for it with a button and confirms it.
 * Unsaved work in that program is lost, so the screen says so first. System programs are never closed.
 */
@RestController
@RequestMapping("/api")
public class CloseApi {

    private static final List<String> PROTECTED = List.of("system", "smss", "csrss", "wininit", "winlogon", "services",
            "lsass", "svchost", "dwm", "explorer", "init", "systemd", "sshd", "xorg", "gnome-shell", "kthreadd", "java");

    private final Monitor monitor;
    private final Demo demo;

    public CloseApi(Monitor monitor, Demo demo) {
        this.monitor = monitor;
        this.demo = demo;
    }

    @PostMapping("/programs/close")
    public Map<String, Object> close(@RequestParam String name) throws InterruptedException {
        if (demo.on()) {
            return Map.of("closed", 5, "remaining", 0, "freeBefore", 3L << 30, "freeAfter", 4L << 30,
                    "message", "Demo: '" + name + "' would be closed and about 1 GB of memory freed. Nothing was really closed.");
        }
        List<Proc> mine = monitor.procs().stream().filter(p -> p.name().equalsIgnoreCase(name) && closable(p)).toList();
        if (mine.isEmpty()) {
            throw new IllegalArgumentException("'" + name + "' cannot be closed here. It is not running, it belongs to the "
                    + "system or another user, or this app runs inside it.");
        }
        long before = monitor.memFree();
        for (Proc p : mine) {
            ProcessHandle.of(p.pid()).ifPresent(ProcessHandle::destroy);
        }
        Thread.sleep(1500);
        long remaining = mine.stream().filter(p -> ProcessHandle.of(p.pid()).map(ProcessHandle::isAlive).orElse(false)).count();
        long after = monitor.memFree();
        String message = remaining == 0
                ? String.format("Closed %d '%s' process(es). Free memory went from %.1f GB to %.1f GB.", mine.size(), name,
                        before / 1073741824.0, after / 1073741824.0)
                : remaining + " of " + mine.size() + " '" + name + "' process(es) are still running. They may need to be closed from the program itself.";
        return Map.of("closed", mine.size() - remaining, "remaining", remaining, "freeBefore", before, "freeAfter", after,
                "message", message);
    }

    /** Only your own programs, never system programs, never this app or what started it. */
    static boolean closable(Proc p) {
        String name = p.name().toLowerCase(Locale.ROOT).replace(".exe", "");
        boolean mine = p.user() != null
                && p.user().substring(p.user().lastIndexOf('\\') + 1).equalsIgnoreCase(System.getProperty("user.name"));
        boolean self = ProcessHandle.current().pid() == p.pid()
                || ProcessHandle.current().parent().map(h -> h.pid() == p.pid()).orElse(false);
        return mine && !self && !PROTECTED.contains(name);
    }
}
