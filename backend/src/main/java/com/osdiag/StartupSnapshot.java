package com.osdiag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** The startup programs and the power plan at one moment. A new one is saved only when they changed or the user asks. */
@Entity
@Table(name = "startup_snapshots", indexes = @Index(name = "idx_startup_snapshots_captured_at", columnList = "capturedAt"))
public class StartupSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant capturedAt;
    @Column(length = 100)
    private String powerPlan;
    @Column(length = 64, nullable = false)
    private String contentHash;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<StartupEntry> entries = new ArrayList<>();

    public Long getId() { return id; }
    public Instant getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Instant capturedAt) { this.capturedAt = capturedAt; }
    public String getPowerPlan() { return powerPlan; }
    public void setPowerPlan(String powerPlan) { this.powerPlan = powerPlan; }
    @JsonIgnore
    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }
    public List<StartupEntry> getEntries() { return entries; }
    public void setEntries(List<StartupEntry> entries) { this.entries = entries; }
}
