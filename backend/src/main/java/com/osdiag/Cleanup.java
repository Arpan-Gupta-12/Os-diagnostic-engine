package com.osdiag;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cleanups")
public class Cleanup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant time;
    @Column(length = 20)
    private String what;
    private int files;
    private long bytes;
    private int failed;
    private long bytesBefore;
    private long bytesAfter;
    private Long freeBefore;
    private Long freeAfter;
    private boolean verified;
    @Column(columnDefinition = "boolean not null default false")
    private boolean demo;
    @Column(length = 500)
    private String summary;

    public Long getId() { return id; }
    public Instant getTime() { return time; }
    public void setTime(Instant time) { this.time = time; }
    public String getWhat() { return what; }
    public void setWhat(String what) { this.what = what; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
    public int getFiles() { return files; }
    public void setFiles(int files) { this.files = files; }
    public long getBytes() { return bytes; }
    public void setBytes(long bytes) { this.bytes = bytes; }
    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
    public long getBytesBefore() { return bytesBefore; }
    public void setBytesBefore(long bytesBefore) { this.bytesBefore = bytesBefore; }
    public long getBytesAfter() { return bytesAfter; }
    public void setBytesAfter(long bytesAfter) { this.bytesAfter = bytesAfter; }
    public Long getFreeBefore() { return freeBefore; }
    public void setFreeBefore(Long freeBefore) { this.freeBefore = freeBefore; }
    public Long getFreeAfter() { return freeAfter; }
    public void setFreeAfter(Long freeAfter) { this.freeAfter = freeAfter; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
}
