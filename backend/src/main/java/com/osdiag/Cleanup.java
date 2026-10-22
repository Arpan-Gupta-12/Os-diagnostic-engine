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
    private int files; // removed
    private long bytes; // freed
    private int failed; // could not be removed (in use / no permission)
    private long bytesBefore; // old files found before cleaning
    private long bytesAfter; // old files still there afterwards (measured again)
    private Long freeBefore; // free space of the temp drive
    private Long freeAfter;
    private boolean verified; // true only if the measurement shows the folder really shrank
    @Column(length = 500)
    private String summary;

    public Long getId() { return id; }
    public Instant getTime() { return time; }
    public void setTime(Instant time) { this.time = time; }
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
