package com.osdiag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "scans")
public class Scan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant time;
    private String status; // Healthy, Needs attention, Critical or Unknown
    private Integer score; // 0-100, null if CPU or memory could not be measured
    @Column(length = 500)
    private String summary;
    private Double cpu;
    private Double mem;
    private Long memFree;
    private Integer procs;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(length = 800)
    private List<String> problems = new ArrayList<>();

    public Long getId() { return id; }
    public Instant getTime() { return time; }
    public void setTime(Instant time) { this.time = time; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Double getCpu() { return cpu; }
    public void setCpu(Double cpu) { this.cpu = cpu; }
    public Double getMem() { return mem; }
    public void setMem(Double mem) { this.mem = mem; }
    public Long getMemFree() { return memFree; }
    public void setMemFree(Long memFree) { this.memFree = memFree; }
    public Integer getProcs() { return procs; }
    public void setProcs(Integer procs) { this.procs = procs; }
    public List<String> getProblems() { return problems; }
    public void setProblems(List<String> problems) { this.problems = problems; }
}
