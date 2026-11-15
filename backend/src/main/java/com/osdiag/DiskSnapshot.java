package com.osdiag;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
import jakarta.persistence.Table;

/** The size and free space of one drive, measured during a checkup. */
@Entity
@Table(name = "disk_snapshots", indexes = @Index(name = "idx_disk_snapshots_captured_at", columnList = "capturedAt"))
public class DiskSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_id", foreignKey = @ForeignKey(name = "fk_disk_snapshots_scan"))
    private Scan scan;
    @Column(name = "scan_id", insertable = false, updatable = false)
    private Long scanId;
    @Column(length = 255)
    private String driveName;
    private long totalSpace;
    private long freeSpace;
    private long usedSpace;
    private Instant capturedAt;

    static DiskSnapshot of(Scan scan, Disk disk, Instant capturedAt) {
        DiskSnapshot d = new DiskSnapshot();
        d.scan = scan;
        d.driveName = disk.name();
        d.totalSpace = disk.total();
        d.freeSpace = disk.free();
        d.usedSpace = disk.total() - disk.free();
        d.capturedAt = capturedAt;
        return d;
    }

    public Long getId() { return id; }
    @JsonIgnore
    public Scan getScan() { return scan; }
    public Long getScanId() { return scanId; }
    public String getDriveName() { return driveName; }
    public long getTotalSpace() { return totalSpace; }
    public long getFreeSpace() { return freeSpace; }
    public long getUsedSpace() { return usedSpace; }
    public Instant getCapturedAt() { return capturedAt; }
}
