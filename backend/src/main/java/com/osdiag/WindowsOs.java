package com.osdiag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class WindowsOs implements OsAdapter {

    private static final String RUN_KEY = "\\Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static final Pattern REG_LINE = Pattern.compile("^\\s+(.+?)\\s+REG_(?:EXPAND_)?SZ\\s+(.*)$");
    private static final Pattern PLAN = Pattern.compile("\\((.+)\\)");

    @Override
    public List<Startup> startupItems() {
        List<Startup> items = new ArrayList<>();
        items.addAll(fromRegistry("HKCU", "Current user"));
        items.addAll(fromRegistry("HKLM", "All users"));
        items.addAll(fromStartupFolder());
        return items;
    }

    @Override
    public List<Startup> scheduledJobs() {
        List<Startup> jobs = new ArrayList<>();
        for (String line : OsAdapter.run("schtasks", "/query", "/fo", "csv", "/v", "/nh")) {
            Startup job = parseTaskLine(line);
            if (job != null) {
                jobs.add(job);
            }
        }
        return jobs;
    }

    @Override
    public boolean setPowerPlan(String plan) {
        String scheme = plan.equals("performance") ? "SCHEME_MIN" : "SCHEME_BALANCED";
        return OsAdapter.run("powercfg", "/setactive", scheme).isEmpty();
    }

    @Override
    public String powerPlan() {
        for (String line : OsAdapter.run("powercfg", "/getactivescheme")) {
            String plan = parsePlan(line);
            if (plan != null) {
                return plan;
            }
        }
        return null;
    }

    @Override
    public Path thumbnailDir() {
        String local = System.getenv("LOCALAPPDATA");
        return local == null ? null : Path.of(local, "Microsoft", "Windows", "Explorer");
    }

    @Override
    public String thumbnailPrefix() {
        return "thumbcache_";
    }

    @Override
    public List<Path> browserCaches() {
        String local = System.getenv("LOCALAPPDATA");
        List<Path> dirs = new ArrayList<>();
        if (local == null) {
            return dirs;
        }
        dirs.add(Path.of(local, "Google", "Chrome", "User Data", "Default", "Cache"));
        dirs.add(Path.of(local, "Microsoft", "Edge", "User Data", "Default", "Cache"));
        try (Stream<Path> profiles = Files.list(Path.of(local, "Mozilla", "Firefox", "Profiles"))) {
            profiles.forEach(p -> dirs.add(p.resolve("cache2")));
        } catch (IOException e) {
            // Firefox is not installed
        }
        return dirs.stream().filter(Files::isDirectory).toList();
    }

    @Override
    public List<Path> reportDirs() {
        String local = System.getenv("LOCALAPPDATA");
        if (local == null) {
            return List.of();
        }
        return Stream.of(Path.of(local, "CrashDumps"), Path.of(local, "Microsoft", "Windows", "WER", "ReportArchive"),
                Path.of(local, "Microsoft", "Windows", "WER", "ReportQueue")).filter(Files::isDirectory).toList();
    }

    @Override
    public Path hostsFile() {
        return Path.of(System.getenv().getOrDefault("SystemRoot", "C:\\Windows"), "System32", "drivers", "etc", "hosts");
    }

    /** Windows Defender only. Another antivirus makes Defender switch itself off, which is normal. */
    @Override
    public Boolean antivirusOn() {
        List<String> out = OsAdapter.run("powershell", "-NoProfile", "-Command",
                "(Get-MpComputerStatus).RealTimeProtectionEnabled");
        return out.isEmpty() ? null : out.get(0).trim().equalsIgnoreCase("True");
    }

    private List<Startup> fromRegistry(String hive, String where) {
        List<Startup> items = new ArrayList<>();
        for (String line : OsAdapter.run("reg", "query", hive + RUN_KEY)) {
            Startup item = parseRegistryLine(line, where);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private List<Startup> fromStartupFolder() {
        String appData = System.getenv("APPDATA");
        List<Startup> items = new ArrayList<>();
        if (appData == null) {
            return items;
        }
        Path folder = Path.of(appData, "Microsoft", "Windows", "Start Menu", "Programs", "Startup");
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(f -> !f.getFileName().toString().equalsIgnoreCase("desktop.ini"))
                    .forEach(f -> items.add(new Startup(f.getFileName().toString(), f.toString(), "Startup folder")));
        } catch (IOException e) {
            // no startup folder: nothing starts from there
        }
        return items;
    }

    /** A line of "reg query" looks like: "    OneDrive    REG_SZ    C:\...\OneDrive.exe /background". */
    static Startup parseRegistryLine(String line, String where) {
        Matcher m = REG_LINE.matcher(line);
        return m.matches() ? new Startup(m.group(1), m.group(2), where) : null;
    }

    /** A line of "schtasks /query /fo csv /v" has the task name in column 2 and what it runs in column 9. Windows' own tasks are skipped. */
    static Startup parseTaskLine(String line) {
        String[] cells = line.split("\"\\s*,\\s*\"", -1);
        if (cells.length < 9) {
            return null;
        }
        String name = cells[1];
        String command = cells[8];
        return name.startsWith("\\Microsoft\\") || command.isBlank() || command.equals("N/A") ? null
                : new Startup(name.substring(name.lastIndexOf('\\') + 1), command, "Scheduled task");
    }

    /** A line of "powercfg /getactivescheme" looks like: "Power Scheme GUID: 381b...  (Balanced)". */
    static String parsePlan(String line) {
        Matcher m = PLAN.matcher(line);
        return m.find() ? m.group(1) : null;
    }
}
