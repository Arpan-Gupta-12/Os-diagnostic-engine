package com.osdiag;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

/** One security check that was saved, with the warning signs it found. */
@Entity
@Table(name = "security_scans", indexes = @Index(name = "idx_security_scans_scan_time", columnList = "scanTime"))
public class SecurityScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant scanTime;
    @Column(length = 500)
    private String summary;
    private int totalFindings;

    @OneToMany(mappedBy = "securityScan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<SecurityFinding> findings = new ArrayList<>();

    public Long getId() { return id; }
    public Instant getScanTime() { return scanTime; }
    public void setScanTime(Instant scanTime) { this.scanTime = scanTime; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public int getTotalFindings() { return totalFindings; }
    public void setTotalFindings(int totalFindings) { this.totalFindings = totalFindings; }
    public List<SecurityFinding> getFindings() { return findings; }
    public void setFindings(List<SecurityFinding> findings) { this.findings = findings; }
}
