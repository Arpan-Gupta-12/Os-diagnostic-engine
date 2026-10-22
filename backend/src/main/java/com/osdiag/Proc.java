package com.osdiag;

public record Proc(int pid, String name, Double cpu, long mem, String user) {
}
