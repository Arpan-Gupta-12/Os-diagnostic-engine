package com.osdiag;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OSProcess;

@Service
public class Monitor {

    private final SystemInfo si = new SystemInfo();
    private final List<Sample> history = new ArrayList<>(); // last 60 samples, for the charts

    private long[] lastTicks = si.getHardware().getProcessor().getSystemCpuLoadTicks();
    private long lastTime = System.currentTimeMillis();
    private long lastPages = pages();
    private Map<Integer, OSProcess> lastProcs = new HashMap<>();

    /** Every 3 seconds in the background. */
    @Scheduled(fixedDelay = 3000, initialDelay = 1000)
    public synchronized void tick() {
        history.add(sample());
        if (history.size() > 60) {
            history.remove(0);
        }
    }

    public synchronized List<Sample> history() {
        return new ArrayList<>(history);
    }

    public synchronized Sample sample() {
        long now = System.currentTimeMillis();
        double secs = Math.max((now - lastTime) / 1000.0, 0.001);

        CentralProcessor cpu = si.getHardware().getProcessor();
        long[] ticks = cpu.getSystemCpuLoadTicks();
        double load = cpu.getSystemCpuLoadBetweenTicks(lastTicks);
        lastTicks = ticks;

        GlobalMemory ram = si.getHardware().getMemory();
        long total = ram.getTotal();
        long free = ram.getAvailable();

        long pages = pages();
        Double paging = pages >= 0 && lastPages >= 0 ? Math.max(0, pages - lastPages) / secs : null;
        lastPages = pages;
        lastTime = now;

        return new Sample(Instant.ofEpochMilli(now), Double.isNaN(load) ? null : load * 100,
                total > 0 ? 100.0 * (total - free) / total : null, free, total, paging,
                si.getOperatingSystem().getProcessCount());
    }

    /** Three quick samples averaged, so one short spike does not decide a scan. */
    public Sample fresh() throws InterruptedException {
        List<Sample> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Thread.sleep(700);
            list.add(sample());
        }
        return new Sample(Instant.now(), avg(list, Sample::cpu), avg(list, Sample::mem), list.get(2).memFree(),
                list.get(2).memTotal(), avg(list, Sample::paging), list.get(2).procs());
    }

    private static Double avg(List<Sample> list, java.util.function.Function<Sample, Double> field) {
        double sum = 0;
        int n = 0;
        for (Sample s : list) {
            Double v = field.apply(s);
            if (v != null) {
                sum += v;
                n++;
            }
        }
        return n == 0 ? null : sum / n;
    }

    /** Pages moved between RAM and the swap file since boot, or -1 if the OS does not tell us. */
    private long pages() {
        var vm = si.getHardware().getMemory().getVirtualMemory();
        return vm.getSwapPagesIn() < 0 || vm.getSwapPagesOut() < 0 ? -1 : vm.getSwapPagesIn() + vm.getSwapPagesOut();
    }

    /** All running programs. A program's CPU % also needs two readings. */
    public synchronized List<Proc> procs() {
        int cores = si.getHardware().getProcessor().getLogicalProcessorCount();
        Map<Integer, OSProcess> now = new HashMap<>();
        List<Proc> list = new ArrayList<>();
        for (OSProcess p : si.getOperatingSystem().getProcesses()) {
            if (p.getProcessID() == 0) {
                continue; // "System Idle Process" on Windows
            }
            now.put(p.getProcessID(), p);
            OSProcess before = lastProcs.get(p.getProcessID());
            double load = before != null ? p.getProcessCpuLoadBetweenTicks(before) : p.getProcessCpuLoadCumulative();
            Double cpu = Double.isNaN(load) || cores == 0 ? null : 100.0 * load / cores;
            list.add(new Proc(p.getProcessID(), p.getName(), cpu, p.getResidentSetSize(), p.getUser()));
        }
        lastProcs = now;
        return list;
    }

    /** Drives of this PC. */
    public List<Checkup.Disk> disks() {
        List<Checkup.Disk> list = new ArrayList<>();
        for (File root : File.listRoots()) {
            if (root.getTotalSpace() > 0) {
                list.add(new Checkup.Disk(root.getPath(), root.getTotalSpace(), root.getUsableSpace()));
            }
        }
        return list;
    }

    /** "About this PC". */
    public Map<String, Object> info() {
        var os = si.getOperatingSystem();
        var cpu = si.getHardware().getProcessor();
        Map<String, Object> m = new HashMap<>();
        m.put("os", os.getFamily() + " " + os.getVersionInfo().getVersion());
        m.put("host", os.getNetworkParams().getHostName());
        m.put("cpu", cpu.getProcessorIdentifier().getName().trim());
        m.put("cores", cpu.getLogicalProcessorCount());
        m.put("ram", si.getHardware().getMemory().getTotal());
        m.put("disks", disks());
        return m;
    }
}
