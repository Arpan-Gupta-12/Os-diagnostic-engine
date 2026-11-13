package com.osdiag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/** Made-up numbers for showing the app on a PC that has no problem. Never used unless the user turns it on. */
@Component
public class Demo {

    private static final long GB = 1024L * 1024 * 1024;

    private volatile String mode = "off"; // off, healthy or slow
    private int tick;

    public String mode() {
        return mode;
    }

    public void set(String mode) {
        this.mode = mode;
    }

    public boolean on() {
        return !mode.equals("off");
    }

    private boolean slow() {
        return mode.equals("slow");
    }

    public synchronized Sample sample() {
        double wave = Math.sin(tick++ / 3.0);
        boolean s = slow();
        double mem = s ? 91 + 2 * wave : 55 + 3 * wave;
        long total = 16 * GB;
        return new Sample(Instant.now(), s ? 92 + 4 * wave : 25 + 8 * wave, mem, (long) (total * (1 - mem / 100)), total,
                s ? 450 + 100 * wave : 3 + 2 * wave, s ? 320 : 180, s ? 95 + 3 * wave : 10 + 5 * wave,
                s ? 30 + 5 * wave : 2 + wave, s ? 93 + 2 * wave : 50 + 3 * wave);
    }

    public List<Proc> procs() {
        boolean s = slow();
        List<Proc> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            list.add(proc(1000 + i, "chrome", s ? 2.0 : 1.0, s ? GB : GB / 8));
        }
        list.add(proc(2000, "video-editor", s ? 60.0 : 3.0, 2 * GB));
        list.add(proc(2001, "code", 4.0, GB / 2));
        list.add(proc(2002, "music-player", 1.0, GB / 8));
        list.add(proc(2003, "explorer", 0.5, GB / 16));
        if (s) {
            list.add(new Proc(3000, "svch0st.exe", 5.0, GB / 16, "demo", "C:\\Users\\demo\\AppData\\Local\\Temp\\svch0st.exe"));
        }
        return list;
    }

    private static Proc proc(int pid, String name, double cpu, long mem) {
        String folder = name.equals("explorer") ? "C:\\Windows\\" : "C:\\Program Files\\" + name + "\\";
        return new Proc(pid, name, cpu, mem, System.getProperty("user.name"), folder + name + ".exe");
    }

    public List<Listener> listeners() {
        return slow() ? List.of(new Listener(3389, 800), new Listener(4444, 3000)) : List.of();
    }

    public List<Listener> outgoing() {
        return slow() ? List.of(new Listener(6667, 3000)) : List.of();
    }

    public List<OsAdapter.Startup> scheduledJobs() {
        return slow() ? List.of(new OsAdapter.Startup("Helper", "C:\\Users\\demo\\AppData\\Local\\Temp\\helper.exe", "Scheduled task")) : List.of();
    }

    public List<String> hosts() {
        return slow() ? List.of("127.0.0.1 localhost", "203.0.113.7 www.mybank.example") : List.of("127.0.0.1 localhost");
    }

    public Boolean antivirus() {
        return !slow();
    }

    public List<Disk> disks() {
        return List.of(new Disk("C:\\", 512 * GB, slow() ? 40 * GB : 230 * GB), new Disk("D:\\", 1024 * GB, 600 * GB));
    }

    public List<OsAdapter.Startup> startup() {
        String[] names = { "Cloud Drive", "Chat App", "Music Player", "Game Launcher", "Update Helper", "Printer Tool",
                "Browser Helper", "Video Editor Agent", "Backup Tool", "PDF Helper" };
        List<OsAdapter.Startup> list = new ArrayList<>();
        for (int i = 0; i < (slow() ? names.length : 3); i++) {
            list.add(new OsAdapter.Startup(names[i], "C:\\Program Files\\" + names[i] + "\\start.exe", "Current user"));
        }
        if (slow()) {
            list.add(new OsAdapter.Startup("Updater", "powershell -WindowStyle Hidden -EncodedCommand SQBFAFgA", "Current user"));
        }
        return list;
    }

    public String power() {
        return slow() ? "High performance" : "Balanced";
    }
}
