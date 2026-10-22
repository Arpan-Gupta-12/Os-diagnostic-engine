package com.osdiag;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

// Spring Data writes the database code for these two interfaces.
interface ScanRepo extends JpaRepository<Scan, Long> {
    List<Scan> findTop50ByOrderByTimeDesc();
}

interface CleanupRepo extends JpaRepository<Cleanup, Long> {
    List<Cleanup> findTop20ByOrderByTimeDesc();
}
