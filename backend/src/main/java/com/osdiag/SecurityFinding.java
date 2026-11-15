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

/** One warning sign (a Security.Indicator) found by a saved security check. */
@Entity
@Table(name = "security_findings")
public class SecurityFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "security_scan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_security_findings_scan"))
    private SecurityScan securityScan;
    @Column(length = 20)
    private String level;
    @Column(length = 300)
    private String title;
    @Column(length = 2000)
    private String text;

    public Long getId() { return id; }
    @JsonIgnore
    public SecurityScan getSecurityScan() { return securityScan; }
    public void setSecurityScan(SecurityScan securityScan) { this.securityScan = securityScan; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}
