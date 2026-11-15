package com.osdiag;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One cleanup offered in an Optimize session. It points to the cleanups row once the user ran it. */
@Entity
@Table(name = "optimization_actions")
public class OptimizationAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "optimization_run_id", nullable = false, foreignKey = @ForeignKey(name = "fk_optimization_actions_run"))
    private OptimizationRun run;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleanup_id", foreignKey = @ForeignKey(name = "fk_optimization_actions_cleanup"))
    private Cleanup cleanup;
    @Column(name = "cleanup_id", insertable = false, updatable = false)
    private Long cleanupId;
    @Column(length = 20)
    private String actionKey;
    private int plannedFiles;
    private long plannedBytes;
    private boolean selected;

    public Long getId() { return id; }
    @JsonIgnore
    public OptimizationRun getRun() { return run; }
    public void setRun(OptimizationRun run) { this.run = run; }
    @JsonIgnore
    public Cleanup getCleanup() { return cleanup; }
    public void setCleanup(Cleanup cleanup) { this.cleanup = cleanup; }
    public Long getCleanupId() { return cleanupId; }
    public String getActionKey() { return actionKey; }
    public void setActionKey(String actionKey) { this.actionKey = actionKey; }
    public int getPlannedFiles() { return plannedFiles; }
    public void setPlannedFiles(int plannedFiles) { this.plannedFiles = plannedFiles; }
    public long getPlannedBytes() { return plannedBytes; }
    public void setPlannedBytes(long plannedBytes) { this.plannedBytes = plannedBytes; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
