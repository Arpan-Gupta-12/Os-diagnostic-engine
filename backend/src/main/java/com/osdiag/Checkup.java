package com.osdiag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The diagnosis rules. Each rule is one small function that adds a sentence to the problem list when it fires. */
public class Checkup {

    static final long GB = 1024L * 1024 * 1024;

    public static Scan check(Sample now, List<Sample> recent, List<Proc> procs, List<Disk> disks, Limits limits) {
        List<String> problems = new ArrayList<>();
        int lost = 0;

        lost += cpu(now, recent, procs, limits, problems);
        lost += memory(now, limits, problems);
        lost += bigProgram(now, procs, limits, problems);
        lost += diskSpace(disks, limits, problems);
        lost += diskBusy(now, recent, limits, problems);
        lost += heat(now, limits, problems);

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

    static int cpu(Sample now, List<Sample> recent, List<Proc> procs, Limits limits, List<String> problems) {
        if (!sustained(recent.stream().map(Sample::cpu).toList(), limits.getCpu())) {
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

    /** High memory use is normal. It only hurts when the PC also uses the slow disk as extra memory. */
    static int memory(Sample now, Limits limits, List<String> problems) {
        if (now.mem() == null || now.mem() < limits.getMem()) {
            return 0;
        }
        if (now.paging() == null || now.paging() < limits.getPaging()) {
            return 0;
        }
        problems.add(String.format("Memory is almost full (%.0f%% used) and the PC is using the slow disk as extra "
                + "memory (%.0f pages/s). Tip: close programs and browser tabs you do not use.", now.mem(), now.paging()));
        return now.mem() >= 95 ? 35 : 20;
    }

    /** All processes with the same name are added up, e.g. every "chrome". */
    static int bigProgram(Sample now, List<Proc> procs, Limits limits, List<String> problems) {
        if (now.memTotal() == null) {
            return 0;
        }
        Map<String, Long> byName = new LinkedHashMap<>();
        for (Proc p : procs) {
            byName.merge(p.name(), p.mem(), Long::sum);
        }
        for (Map.Entry<String, Long> e : byName.entrySet()) {
            double pct = 100.0 * e.getValue() / now.memTotal();
            if (pct >= limits.getProgram()) {
                problems.add(String.format("'%s' uses %.0f%% of your memory. Tip: if you do not need it right now, "
                        + "save your work and close it. This app never closes programs for you.", e.getKey(), pct));
                return 10;
            }
        }
        return 0;
    }

    static int diskSpace(List<Disk> disks, Limits limits, List<String> problems) {
        int lost = 0;
        for (Disk d : disks) {
            double free = 100.0 * d.free() / d.total();
            if (d.total() > GB && free < limits.getDisk()) {
                problems.add(String.format("Drive %s is almost full (%.0f%% free). Tip: run Optimize to remove old "
                        + "temporary files, and move big files you rarely use to another drive.", d.name(), free));
                lost += free < 5 ? 35 : 20;
            }
        }
        return lost;
    }

    static int diskBusy(Sample now, List<Sample> recent, Limits limits, List<String> problems) {
        if (!sustained(recent.stream().map(Sample::disk).toList(), limits.getDiskBusy())) {
            return 0;
        }
        problems.add(String.format("The disk has been busy for a long time (now %.0f%%), so programs wait for it. "
                + "Tip: wait for downloads, updates or virus scans to finish.", now.disk()));
        return 15;
    }

    static int heat(Sample now, Limits limits, List<String> problems) {
        if (now.temp() == null || now.temp() < limits.getTemp()) {
            return 0;
        }
        problems.add(String.format("The processor is hot (%.0f °C). Tip: check that the air vents are free of dust "
                + "and the PC stands on a hard, flat surface.", now.temp()));
        return 10;
    }

    /** At least 3 samples, and at least 80% of them at or above the limit. One spike is not enough. */
    static boolean sustained(List<Double> values, double limit) {
        List<Double> known = values.stream().filter(v -> v != null).toList();
        long high = known.stream().filter(v -> v >= limit).count();
        return known.size() >= 3 && high >= Math.ceil(known.size() * 0.8);
    }
}
