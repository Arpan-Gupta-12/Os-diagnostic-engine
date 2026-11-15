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

/** One program that starts with the PC, as listed in a startup snapshot. */
@Entity
@Table(name = "startup_entries")
public class StartupEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "snapshot_id", nullable = false, foreignKey = @ForeignKey(name = "fk_startup_entries_snapshot"))
    private StartupSnapshot snapshot;
    @Column(length = 255)
    private String name;
    @Column(length = 1000)
    private String command;
    @Column(length = 100)
    private String location;

    public Long getId() { return id; }
    @JsonIgnore
    public StartupSnapshot getSnapshot() { return snapshot; }
    public void setSnapshot(StartupSnapshot snapshot) { this.snapshot = snapshot; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
}
