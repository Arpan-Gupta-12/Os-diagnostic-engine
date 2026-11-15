package com.osdiag;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/** One live measurement kept for history. The live screen still reads the last 60 samples from memory. */
@Entity
@Table(name = "performance_samples", indexes = @Index(name = "idx_performance_samples_time", columnList = "time"))
public class PerformanceSample {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant time;
    private Double cpu;
    private Double mem;
    private Long memFree;
    private Long memTotal;
    private Double paging;
    private Integer procs;
    private Double disk;
    private Double ioWait;
    private Double temp;

    static PerformanceSample from(Sample s) {
        PerformanceSample p = new PerformanceSample();
        p.time = s.time();
        p.cpu = s.cpu();
        p.mem = s.mem();
        p.memFree = s.memFree();
        p.memTotal = s.memTotal();
        p.paging = s.paging();
        p.procs = s.procs();
        p.disk = s.disk();
        p.ioWait = s.ioWait();
        p.temp = s.temp();
        return p;
    }

    public Long getId() { return id; }
    public Instant getTime() { return time; }
    public Double getCpu() { return cpu; }
    public Double getMem() { return mem; }
    public Long getMemFree() { return memFree; }
    public Long getMemTotal() { return memTotal; }
    public Double getPaging() { return paging; }
    public Integer getProcs() { return procs; }
    public Double getDisk() { return disk; }
    public Double getIoWait() { return ioWait; }
    public Double getTemp() { return temp; }
}
