package com.osdiag;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class LinuxOs implements OsAdapter {

    private static final Path HOME = Path.of(System.getProperty("user.home"));

    @Override
    public List<Startup> startupItems() {
        List<Startup> items = new ArrayList<>();
        try (Stream<Path> files = Files.list(HOME.resolve(".config/autostart"))) {
            for (Path f : files.filter(f -> f.toString().endsWith(".desktop")).toList()) {
                Startup item = parseDesktopFile(f.getFileName().toString(), Files.readAllLines(f));
                if (item != null) {
                    items.add(item);
                }
            }
        } catch (IOException e) {
            // no autostart folder: nothing starts from there
        }
        return items;
    }

    @Override
    public List<Startup> scheduledJobs() {
        List<Startup> jobs = new ArrayList<>();
        for (String line : OsAdapter.run("crontab", "-l")) {
            if (!line.isBlank() && !line.startsWith("#") && !line.startsWith("no crontab")) {
                jobs.add(new Startup(line.length() > 40 ? line.substring(0, 40) + "..." : line, line, "Cron job"));
            }
        }
        return jobs;
    }

    @Override
    public boolean setPowerPlan(String plan) {
        return !OsAdapter.run("powerprofilesctl", "set", plan).stream().anyMatch(l -> l.contains("rror"))
                && powerPlan() != null;
    }

    @Override
    public String powerPlan() {
        List<String> lines = OsAdapter.run("powerprofilesctl", "get");
        if (!lines.isEmpty()) {
            return lines.get(0).trim();
        }
        try {
            return Files.readString(Path.of("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor")).trim();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Path thumbnailDir() {
        return HOME.resolve(".cache/thumbnails");
    }

    @Override
    public String thumbnailPrefix() {
        return "";
    }

    @Override
    public List<Path> browserCaches() {
        return Stream.of(".cache/google-chrome", ".cache/chromium", ".cache/microsoft-edge", ".cache/mozilla/firefox")
                .map(HOME::resolve).filter(Files::isDirectory).toList();
    }

    @Override
    public List<Path> reportDirs() {
        return List.of();
    }

    @Override
    public Path hostsFile() {
        return Path.of("/etc/hosts");
    }

    @Override
    public Boolean antivirusOn() {
        return null;
    }

    /** Reads Name= and Exec= from an autostart file. Disabled entries are skipped. */
    static Startup parseDesktopFile(String file, List<String> lines) {
        String name = file;
        String command = "";
        for (String line : lines) {
            if (line.startsWith("Name=")) {
                name = line.substring(5);
            } else if (line.startsWith("Exec=")) {
                command = line.substring(5);
            } else if (line.equals("Hidden=true")) {
                return null;
            }
        }
        return new Startup(name, command, "Autostart folder");
    }
}
