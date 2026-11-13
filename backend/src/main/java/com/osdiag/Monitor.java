package com.osdiag;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HWDiskStore;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.OSProcess;

/** Reads live numbers with OSHI. Counters that only go up are turned into rates by remembering the last reading. */
@Service
public class Monitor {

    private final SystemInfo si = new SystemInfo();
    private final Demo demo;
    private final List<Sample> history = new ArrayList<>();

    private long[] lastTicks = si.getHardware().getProcessor().getSystemCpuLoadTicks();
    private long lastTime = System.currentTimeMillis();
    private long lastPages = pages();
    private Map<String, Long> lastTransfer = new HashMap<>();
    private Map<Integer, OSProcess> lastProcs = new HashMap<>();

    public Monitor(Demo demo) {
        this.demo = demo;
    }

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

    /** The last 10 samples (about 30 seconds), used to tell a long load from a short spike. */
    public synchronized List<Sample> recent() {
        if (demo.on()) {
            return List.of(demo.sample(), demo.sample(), demo.sample());
        }
        return new ArrayList<>(history.subList(Math.max(0, history.size() - 10), history.size()));
    }

    public synchronized void clearHistory() {
        history.clear();
    }

    public synchronized Sample sample() {
        if (demo.on()) {
            return demo.sample();
        }
        long now = System.currentTimeMillis();
        double secs = Math.max((now - lastTime) / 1000.0, 0.001);

        CentralProcessor cpu = si.getHardware().getProcessor();
        long[] ticks = cpu.getSystemCpuLoadTicks();
        double load = cpu.getSystemCpuLoadBetweenTicks(lastTicks);
        Double ioWait = ioWait(lastTicks, ticks);
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
                si.getOperatingSystem().getProcessCount(), diskBusy(secs), ioWait, temperature());
    }

    /** Three quick samples averaged, so one short spike does not decide a checkup. */
    public Sample fresh() throws InterruptedException {
        if (demo.on()) {
            return demo.sample();
        }
        List<Sample> list = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Thread.sleep(700);
            list.add(sample());
        }
        Sample last = list.get(2);
        return new Sample(Instant.now(), avg(list, Sample::cpu), avg(list, Sample::mem), last.memFree(), last.memTotal(),
                avg(list, Sample::paging), last.procs(), avg(list, Sample::disk), avg(list, Sample::ioWait),
                avg(list, Sample::temp));
    }

    private static Double avg(List<Sample> list, Function<Sample, Double> field) {
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

    /** Pages moved between RAM and the swap file since boot, or -1 if the OS does not say. */
    private long pages() {
        var vm = si.getHardware().getMemory().getVirtualMemory();
        return vm.getSwapPagesIn() < 0 || vm.getSwapPagesOut() < 0 ? -1 : vm.getSwapPagesIn() + vm.getSwapPagesOut();
    }

    /** Share of time the busiest disk spent working. */
    private Double diskBusy(double secs) {
        Double busiest = null;
        for (HWDiskStore d : si.getHardware().getDiskStores()) {
            long t = d.getTransferTime();
            Long before = lastTransfer.put(d.getName(), t);
            if (before != null && t > 0) {
                double pct = Math.min(100, 100.0 * (t - before) / (secs * 1000));
                busiest = busiest == null ? pct : Math.max(busiest, pct);
            }
        }
        return busiest;
    }

    /** Time the processor waited for the disk. Windows does not report it, so it stays null there. */
    private static Double ioWait(long[] before, long[] after) {
        int i = TickType.IOWAIT.getIndex();
        long total = 0;
        for (int t = 0; t < after.length; t++) {
            total += after[t] - before[t];
        }
        return after[i] == 0 || total <= 0 ? null : 100.0 * (after[i] - before[i]) / total;
    }

    private Double temperature() {
        try {
            double t = si.getHardware().getSensors().getCpuTemperature();
            return t > 0 && !Double.isNaN(t) ? t : null;
        } catch (Exception e) {
            return null;
        }
    }

    public synchronized List<Proc> procs() {
        if (demo.on()) {
            return demo.procs();
        }
        int cores = si.getHardware().getProcessor().getLogicalProcessorCount();
        Map<Integer, OSProcess> now = new HashMap<>();
        List<Proc> list = new ArrayList<>();
        for (OSProcess p : si.getOperatingSystem().getProcesses()) {
            if (p.getProcessID() == 0) {
                continue;
            }
            now.put(p.getProcessID(), p);
            OSProcess before = lastProcs.get(p.getProcessID());
            double load = before != null ? p.getProcessCpuLoadBetweenTicks(before) : p.getProcessCpuLoadCumulative();
            Double cpu = Double.isNaN(load) || cores == 0 ? null : 100.0 * load / cores;
            list.add(new Proc(p.getProcessID(), p.getName(), cpu, p.getResidentSetSize(), p.getUser(), p.getPath()));
        }
        lastProcs = now;
        return list;
    }

    /** Ports that programs are waiting on for connections from other computers (not this PC itself). */
    public List<Listener> listeners() {
        List<Listener> list = new ArrayList<>();
        try {
            for (var c : si.getOperatingSystem().getInternetProtocolStats().getConnections()) {
                if (c.getState() == InternetProtocolStats.TcpState.LISTEN && !isLoopback(c.getLocalAddress())) {
                    list.add(new Listener(c.getLocalPort(), c.getowningProcessId()));
                }
            }
        } catch (Exception e) {
            // the OS does not tell: the security check says it could not look
        }
        return list;
    }

    private static boolean isLoopback(byte[] address) {
        try {
            return java.net.InetAddress.getByAddress(address).isLoopbackAddress();
        } catch (Exception e) {
            return true;
        }
    }

    /** Remote ports of connections this PC has open to other computers. */
    public List<Listener> outgoing() {
        List<Listener> list = new ArrayList<>();
        try {
            for (var c : si.getOperatingSystem().getInternetProtocolStats().getConnections()) {
                if (c.getState() == InternetProtocolStats.TcpState.ESTABLISHED && !isLoopback(c.getForeignAddress())) {
                    list.add(new Listener(c.getForeignPort(), c.getowningProcessId()));
                }
            }
        } catch (Exception e) {
            // the OS does not tell
        }
        return list;
    }

    public long memFree() {
        return si.getHardware().getMemory().getAvailable();
    }

    public List<Disk> disks() {
        if (demo.on()) {
            return demo.disks();
        }
        List<Disk> list = new ArrayList<>();
        for (File root : File.listRoots()) {
            if (root.getTotalSpace() > 0) {
                list.add(new Disk(root.getPath(), root.getTotalSpace(), root.getUsableSpace()));
            }
        }
        return list;
    }

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
        m.put("demo", demo.mode());
        return m;
    }
}
