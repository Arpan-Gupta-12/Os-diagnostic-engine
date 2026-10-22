package com.osdiag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Checkup {

    public record Disk(String name, long total, long free) {
    }

    static final double CPU_LIMIT = 85;
    static final double MEM_LIMIT = 85;
    static final double PAGING_LIMIT = 100;
    static final double PROGRAM_LIMIT = 25;
    static final double DISK_LIMIT = 10;
    static final long GB = 1024L * 1024 * 1024;

    public static Scan check(Sample now, List<Sample> recent, List<Proc> procs, List<Disk> disks) {
        List<String> problems = new ArrayList<>();
        int lost = 0; / points taken off the health score
        lost += cpu(now, recent, procs, problems);
        lost += memory(now, problems);
        lost += bigProgram(now, procs, problems);
        lost += diskSpace(disks, problems);

        boolean noData = now.cpu() == null || now.mem() == null;
        Scan scan = new Scan();
        scan.setTime(Instant.now());
        scan.setCpu(now.cpu());
        scan.setMem(now.mem());
        scan.setMemFree(now.memFree());
        scan.setProcs(now.procs());
        scan.setProblems(problems);
        scan.setScore(noData ? null : Math.max(0, 100 - lost));
        scan.setStatus(noData ? "Unknown" : lost >= 30 ? "Critical" : lost > 0 ? "Needs attention" : "Healthy");
        scan.setSummary(noData ? "We could not measure CPU and memory on this PC, so we cannot judge its health."
                : problems.isEmpty() ? "Your PC looks healthy. No problem was found by the rules."
                : "Your PC may be slowing down. " + problems.size() + " thing(s) found.");
        return scan;
    }


    static int cpu(Sample now, List<Sample> recent, List<Proc> procs, List<String> problems) {
        if (!sustained(recent.stream().map(Sample::cpu).toList(), CPU_LIMIT)) {
            return 0;
        }
        String text = String.format("The processor has been busy for a long time (now %.0f%%).", now.cpu());
        Proc top = procs.stream().filter(p -> p.cpu() != null).max(Comparator.comparing(Proc::cpu)).orElse(null);
        if (top != null && top.cpu() >= 25) {
            text += String.format(" '%s' uses %.0f%% of it.", top.name(), top.cpu());
        }
        problems.add(text + " Tip: save your work and close programs you do not need.");
        return 20;
    }

    static int memory(Sample now, List<String> problems) {
        if (now.mem() == null || now.mem() < MEM_LIMIT) {
            return 0;
        }
        Double paging = now.paging();
        if (paging == null || paging < PAGING_LIMIT) {
            return 0;
        }
        problems.add(String.format("Memory is almost full (%.0f%% used) and the PC is using the slow disk as extra "
                + "memory (%.0f pages/s). Tip: close programs and browser tabs you do not use.", now.mem(), paging));
        return now.mem() >= 95 ? 35 : 20;
    }

    static int bigProgram(Sample now, List<Proc> procs, List<String> problems) {
        if (now.memTotal() == null) {
            return 0;
        }
        Map<String, Long> byName = new LinkedHashMap<>();
        for (Proc p : procs) {
            byName.merge(p.name(), p.mem(), Long::sum);
        }
        for (Map.Entry<String, Long> e : byName.entrySet()) {
            double pct = 100.0 * e.getValue() / now.memTotal();
            if (pct >= PROGRAM_LIMIT) {
                problems.add(String.format("'%s' uses %.0f%% of your memory. Tip: if you do not need it right now, "
                        + "save your work and close it. This app never closes programs for you.", e.getKey(), pct));
                return 10;
            }
        }
        return 0;
    }

    /** A drive with very little free space. */
    static int diskSpace(List<Disk> disks, List<String> problems) {
        int lost = 0;
        for (Disk d : disks) {
            double free = 100.0 * d.free() / d.total();
            if (d.total() > GB && free < DISK_LIMIT) {
                problems.add(String.format("Drive %s is almost full (%.0f%% free). Tip: run Optimize to remove old "
                        + "temporary files, and move big files you rarely use to another drive.", d.name(), free));
                lost += free < 5 ? 35 : 20;
            }
        }
        return lost;
    }

    static boolean sustained(List<Double> values, double limit) {
        List<Double> known = values.stream().filter(v -> v != null).toList();
        long high = known.stream().filter(v -> v >= limit).count();
        return known.size() >= 3 && high >= Math.ceil(known.size() * 0.8);
    }
}
