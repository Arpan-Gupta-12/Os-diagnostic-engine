package com.osdiag;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** The limits the rules use. One row in the database, the defaults apply until the user changes them. */
@Entity
@Table(name = "limits")
public class Limits {

    @Id
    private Long id = 1L;
    @Min(20) @Max(100)
    private int cpu = 85;
    @Min(20) @Max(100)
    private int mem = 85;
    @Min(1) @Max(100000)
    private int paging = 100;
    @Min(5) @Max(90)
    private int program = 25;
    @Min(1) @Max(50)
    private int disk = 10;
    @Min(20) @Max(100)
    private int diskBusy = 90;
    @Min(40) @Max(110)
    private int temp = 90;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public int getCpu() { return cpu; }
    public void setCpu(int cpu) { this.cpu = cpu; }
    public int getMem() { return mem; }
    public void setMem(int mem) { this.mem = mem; }
    public int getPaging() { return paging; }
    public void setPaging(int paging) { this.paging = paging; }
    public int getProgram() { return program; }
    public void setProgram(int program) { this.program = program; }
    public int getDisk() { return disk; }
    public void setDisk(int disk) { this.disk = disk; }
    public int getDiskBusy() { return diskBusy; }
    public void setDiskBusy(int diskBusy) { this.diskBusy = diskBusy; }
    public int getTemp() { return temp; }
    public void setTemp(int temp) { this.temp = temp; }
}
