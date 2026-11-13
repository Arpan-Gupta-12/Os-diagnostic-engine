package com.osdiag;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** The few things that are different on Windows and Linux. Everything else uses OSHI. */
public interface OsAdapter {

    record Startup(String name, String command, String where) {
    }

    /** Programs that start together with the PC. Read only. */
    List<Startup> startupItems();

    /** Scheduled tasks (Windows) or cron jobs (Linux) of the current user. Read only. */
    List<Startup> scheduledJobs();

    /** Switches the power plan to "balanced" or "performance". Returns false if the command failed. */
    boolean setPowerPlan(String plan);

    /** Name of the active power plan, or null if unknown. */
    String powerPlan();

    /** Folder of the thumbnail cache, or null if unknown. */
    Path thumbnailDir();

    /** Only files starting with this text are thumbnails. Empty means every file in the folder. */
    String thumbnailPrefix();

    /** Folders where browsers keep their cache. Only folders that exist are returned. */
    List<Path> browserCaches();

    /** Folders with old crash reports. */
    List<Path> reportDirs();

    Path hostsFile();

    /** Is the antivirus protection on? null if it cannot be checked. */
    Boolean antivirusOn();

    static OsAdapter current() {
        return System.getProperty("os.name").toLowerCase().contains("win") ? new WindowsOs() : new LinuxOs();
    }

    /** Runs a command and returns its output lines. Empty if the command is missing or fails. */
    static List<String> run(String... command) {
        try {
            Process p = new ProcessBuilder(command).redirectErrorStream(true).start();
            List<String> lines = new BufferedReader(new InputStreamReader(p.getInputStream())).lines().toList();
            p.waitFor(5, TimeUnit.SECONDS);
            return lines;
        } catch (Exception e) {
            return List.of();
        }
    }
}
