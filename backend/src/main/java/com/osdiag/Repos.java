package com.osdiag;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface ScanRepo extends JpaRepository<Scan, Long> {
    List<Scan> findByDemoOrderByTimeDesc(boolean demo);
}

interface CleanupRepo extends JpaRepository<Cleanup, Long> {
    List<Cleanup> findByDemoOrderByTimeDesc(boolean demo);
}

interface LimitsRepo extends JpaRepository<Limits, Long> {
    default Limits get() {
        return findById(1L).orElseGet(Limits::new);
    }
}
