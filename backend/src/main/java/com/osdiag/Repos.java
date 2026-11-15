package com.osdiag;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

interface ScanRepo extends JpaRepository<Scan, Long> {
    List<Scan> findByDemoAndTimeAfterOrderByTimeDesc(boolean demo, Instant since, Pageable page);

    List<Scan> findByDemoAndStatusAndTimeAfterOrderByTimeDesc(boolean demo, String status, Instant since, Pageable page);
}

interface CleanupRepo extends JpaRepository<Cleanup, Long> {
    List<Cleanup> findByDemoAndTimeAfterOrderByTimeDesc(boolean demo, Instant since, Pageable page);
}

interface LimitsRepo extends JpaRepository<Limits, Long> {
    default Limits get() {
        return findById(1L).orElseGet(Limits::new);
    }
}

interface PerformanceSampleRepo extends JpaRepository<PerformanceSample, Long> {
    List<PerformanceSample> findByTimeAfterOrderByTimeDesc(Instant since, Pageable page);

    @Transactional
    long deleteByTimeBefore(Instant cutoff);
}

interface SecurityScanRepo extends JpaRepository<SecurityScan, Long> {
    List<SecurityScan> findByScanTimeAfterOrderByScanTimeDesc(Instant since, Pageable page);
}

interface DiskSnapshotRepo extends JpaRepository<DiskSnapshot, Long> {
    List<DiskSnapshot> findByCapturedAtAfterOrderByCapturedAtDesc(Instant since, Pageable page);
}

interface StartupSnapshotRepo extends JpaRepository<StartupSnapshot, Long> {
    Optional<StartupSnapshot> findFirstByOrderByCapturedAtDesc();

    List<StartupSnapshot> findByCapturedAtAfterOrderByCapturedAtDesc(Instant since, Pageable page);
}

interface SystemActionLogRepo extends JpaRepository<SystemActionLog, Long> {
    List<SystemActionLog> findByExecutionTimeAfterOrderByExecutionTimeDesc(Instant since, Pageable page);
}

interface OptimizationRunRepo extends JpaRepository<OptimizationRun, Long> {
    Optional<OptimizationRun> findFirstByScanIdOrderByStartedAtDesc(Long scanId);

    List<OptimizationRun> findByStartedAtAfterOrderByStartedAtDesc(Instant since, Pageable page);
}
