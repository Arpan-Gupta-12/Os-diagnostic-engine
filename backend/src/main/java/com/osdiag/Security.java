package com.osdiag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Warning signs of a compromised PC ("indicators of compromise"). They are reasons to look closer, never proof,
 * and this is not a virus scan. Each rule is one small function.
 */
public class Security {

    public record Indicator(String level, String title, String text) {
    }

    private static final List<String> SYSTEM_NAMES = List.of("svchost", "lsass", "csrss", "winlogon", "services", "explorer", "smss");
    private static final Map<Integer, String> RISKY_PORTS = Map.of(23, "Telnet", 3389, "Remote Desktop",
            5900, "VNC remote screen", 4444, "a port often used by attack tools");
    private static final Map<Integer, String> BAD_REMOTE_PORTS = Map.of(4444, "attack tools", 6667, "IRC chat, which botnets use",
            1337, "backdoors", 31337, "backdoors");
    private static final List<String> STRONG = List.of("-encodedcommand", " -enc ", "-windowstyle hidden", "-w hidden", "mshta");
    private static final List<String> WEAK = List.of("wscript", "cscript", "/appdata/local/temp/", "/tmp/");

    /** Everything the rules look at. startup also holds scheduled tasks and cron jobs. */
    public record Input(List<Proc> procs, List<OsAdapter.Startup> startup, List<String> hosts, List<Listener> listeners,
                        List<Listener> outgoing, Boolean antivirus) {
    }

    public static List<Indicator> check(Input in) {
        List<Indicator> found = new ArrayList<>();
        found.addAll(riskyFolders(in.procs()));
        found.addAll(lookAlikes(in.procs()));
        found.addAll(startupRisks(in.startup()));
        found.addAll(hostsRedirects(in.hosts()));
        found.addAll(openPorts(in.listeners(), in.procs()));
        found.addAll(badConnections(in.outgoing(), in.procs()));
        if (Boolean.FALSE.equals(in.antivirus())) {
            found.add(new Indicator("Warning", "Antivirus protection is off",
                    "Windows Defender real-time protection is switched off. That is normal if you use another antivirus. "
                            + "If not, turn it on in Windows Security."));
        }
        return found;
    }

    /** A program started from a temporary or Downloads folder has not been installed properly. */
    static List<Indicator> riskyFolders(List<Proc> procs) {
        List<Indicator> found = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (Proc p : procs) {
            String path = lower(p.path());
            boolean risky = path.contains("/appdata/local/temp/") || path.startsWith("/tmp/") || path.startsWith("/dev/shm/")
                    || path.contains("/downloads/") || path.contains("$recycle.bin");
            if (risky && !seen.contains(p.name()) && found.size() < 5) {
                seen.add(p.name());
                found.add(new Indicator("Check", "'" + p.name() + "' runs from a temporary or Downloads folder",
                        p.path() + " Many of these are harmless installers, but malware often runs from such folders. "
                                + "If you do not know this program, scan it with your antivirus."));
            }
        }
        return found;
    }

    /** Malware likes names that look like Windows programs. */
    static List<Indicator> lookAlikes(List<Proc> procs) {
        List<Indicator> found = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (Proc p : procs) {
            String name = lower(p.name()).replace(".exe", "");
            String path = lower(p.path());
            for (String known : SYSTEM_NAMES) {
                boolean wrongFolder = name.equals(known) && !path.isBlank() && !path.contains("/windows/");
                boolean copy = distance(name, known) == 1;
                if ((wrongFolder || copy) && !seen.contains(p.name()) && found.size() < 5) {
                    seen.add(p.name());
                    found.add(new Indicator("Warning", wrongFolder
                            ? "'" + p.name() + "' runs from outside the Windows folder"
                            : "'" + p.name() + "' has a name almost like the system program '" + known + "'",
                            (p.path() == null ? "" : p.path() + " ") + "Real system programs live in the Windows folder. "
                                    + "Scan this one with your antivirus."));
                }
            }
        }
        return found;
    }

    static List<Indicator> startupRisks(List<OsAdapter.Startup> startup) {
        List<Indicator> found = new ArrayList<>();
        for (OsAdapter.Startup s : startup) {
            String command = lower(s.command());
            boolean strong = STRONG.stream().anyMatch(command::contains);
            boolean weak = WEAK.stream().anyMatch(command::contains);
            if ((strong || weak) && found.size() < 5) {
                found.add(new Indicator(strong ? "Warning" : "Check", "Startup item '" + s.name() + "' looks unusual",
                        s.command() + " It starts hidden, runs a script, or starts from a temporary folder. "
                                + "Check that you know it."));
            }
        }
        return found;
    }

    /** A hosts file line that sends a website name to another address can send you to a fake site. */
    static List<Indicator> hostsRedirects(List<String> hosts) {
        List<Indicator> found = new ArrayList<>();
        for (String line : hosts == null ? List.<String>of() : hosts) {
            String[] parts = line.trim().split("\\s+");
            boolean local = List.of("127.0.0.1", "::1", "0.0.0.0", "::").contains(parts[0]);
            if (line.isBlank() || line.trim().startsWith("#") || parts.length < 2 || local || found.size() >= 5) {
                continue;
            }
            found.add(new Indicator("Warning", "The hosts file sends '" + parts[1] + "' to " + parts[0],
                    "A website name is pointed at another address. Unless you did this yourself, "
                            + "this can lead you to a fake website."));
        }
        return found;
    }

    static List<Indicator> openPorts(List<Listener> listeners, List<Proc> procs) {
        List<Indicator> found = new ArrayList<>();
        List<Integer> seen = new ArrayList<>();
        for (Listener l : listeners) {
            String what = RISKY_PORTS.get(l.port());
            if (what == null || seen.contains(l.port())) {
                continue;
            }
            seen.add(l.port());
            String program = procs.stream().filter(p -> p.pid() == l.pid()).map(Proc::name).findFirst().orElse("unknown program");
            found.add(new Indicator(l.port() == 4444 ? "Warning" : "Check", "Port " + l.port() + " (" + what + ") is open",
                    "'" + program + "' accepts connections from other computers on this port. Fine if you use it on purpose. "
                            + "If not, switch the feature off."));
        }
        return found;
    }

    /** An open connection to a port that attack tools and botnets are known for. */
    static List<Indicator> badConnections(List<Listener> outgoing, List<Proc> procs) {
        List<Indicator> found = new ArrayList<>();
        List<Integer> seen = new ArrayList<>();
        for (Listener c : outgoing) {
            String what = BAD_REMOTE_PORTS.get(c.port());
            if (what == null || seen.contains(c.port())) {
                continue;
            }
            seen.add(c.port());
            String program = procs.stream().filter(p -> p.pid() == c.pid()).map(Proc::name).findFirst().orElse("unknown program");
            found.add(new Indicator("Warning", "'" + program + "' is connected to port " + c.port() + " of another computer",
                    "That port is known for " + what + ". Unless you know why, scan the program with your antivirus."));
        }
        return found;
    }

    private static String lower(String text) {
        return text == null ? "" : text.toLowerCase().replace('\\', '/');
    }

    /** Number of single-letter changes needed to turn a into b. */
    static int distance(String a, String b) {
        int[] row = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            row[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            int diagonal = row[0];
            row[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int above = row[j];
                int change = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                row[j] = Math.min(Math.min(row[j] + 1, row[j - 1] + 1), diagonal + change);
                diagonal = above;
            }
        }
        return row[b.length()];
    }
}
