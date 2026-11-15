package com.osdiag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** One Optimize session: the checkup it started from and the cleanups that were offered and run after it. */
@Entity
@Table(name = "optimization_runs", indexes = @Index(name = "idx_optimization_runs_started_at", columnList = "startedAt"))
public class OptimizationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "scan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_optimization_runs_scan"))
    private Scan scan;
    @Column(name = "scan_id", insertable = false, updatable = false)
    private Long scanId;
    private Instant startedAt;
    private Instant finishedAt;

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OptimizationAction> actions = new ArrayList<>();

    public Long getId() { return id; }
    @JsonIgnore
    public Scan getScan() { return scan; }
    public void setScan(Scan scan) { this.scan = scan; }
    public Long getScanId() { return scanId; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public void setFinishedAt(Instant finishedAt) { this.finishedAt = finishedAt; }
    public List<OptimizationAction> getActions() { return actions; }
    public void setActions(List<OptimizationAction> actions) { this.actions = actions; }
}
