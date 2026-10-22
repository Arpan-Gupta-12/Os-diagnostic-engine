package com.osdiag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CleanerTest {

    @TempDir
    Path tmp; // the JUnit folder path contains "junit" and a temp folder, so it counts as a temp folder

    Path file(String name, Duration age) throws Exception {
        Path f = Files.write(tmp.resolve(name), new byte[1000]);
        Files.setLastModifiedTime(f, FileTime.from(Instant.now().minus(age)));
        return f;
    }

    @Test
    void removesOnlyOldFiles() throws Exception {
        Path old = file("old.tmp", Duration.ofDays(3));
        Path fresh = file("new.tmp", Duration.ofMinutes(5));

        assertEquals(1, Cleaner.preview(tmp, Duration.ofHours(24)).files());
        assertTrue(Files.exists(old), "a preview deletes nothing");

        assertEquals(1, Cleaner.clean(tmp, Duration.ofHours(24)).files());
        assertFalse(Files.exists(old));
        assertTrue(Files.exists(fresh));
    }

    @Test
    void neverFollowsLinks() throws Exception {
        Path personal = Files.createDirectory(tmp.resolve("personal"));
        Path thesis = Files.write(personal.resolve("thesis.docx"), new byte[10]);
        Files.setLastModifiedTime(thesis, FileTime.from(Instant.now().minus(Duration.ofDays(30))));
        Path cache = Files.createDirectory(tmp.resolve("cache"));
        try {
            Files.createSymbolicLink(cache.resolve("link"), personal);
        } catch (Exception e) {
            return; // no permission to make links on this PC
        }
        Cleaner.clean(cache, Duration.ZERO);
        assertTrue(Files.exists(thesis));
    }

    @Test
    void refusesPersonalFolders() {
        Path home = Path.of(System.getProperty("user.home"));
        assertEquals(0, Cleaner.preview(home, Duration.ZERO).files());
        assertEquals(0, Cleaner.preview(home.getRoot(), Duration.ZERO).files());
    }
}
