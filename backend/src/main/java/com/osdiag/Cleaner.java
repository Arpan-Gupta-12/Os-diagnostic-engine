package com.osdiag;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

/**
 * Removes old temporary files, safely. A file is only touched if ALL of these are true:
 * 1. The folder is a temp, cache or crash-report folder, not the home folder or a drive root.
 * 2. It is a regular file. Links are never followed and folders are left in place.
 * 3. It belongs to the current user.
 * 4. It is older than the minimum age, so files in use are left alone.
 * Files that cannot be deleted are skipped and counted.
 */
public class Cleaner {

    public record Result(int files, long bytes, int failed) {
        Result plus(Result other) {
            return new Result(files + other.files, bytes + other.bytes, failed + other.failed);
        }
    }

    /** Dry run: what would be removed. Only files whose name starts with prefix count ("" = all). */
    public static Result preview(Path dir, Duration minAge, String prefix) {
        List<Path> files = find(dir, minAge, prefix);
        return new Result(files.size(), size(files), 0);
    }

    public static Result clean(Path dir, Duration minAge, String prefix) {
        int removed = 0;
        int failed = 0;
        long bytes = 0;
        for (Path f : find(dir, minAge, prefix)) {
            try {
                long size = Files.size(f);
                Files.delete(f);
                removed++;
                bytes += size;
            } catch (IOException e) {
                failed++;
            }
        }
        return new Result(removed, bytes, failed);
    }

    static boolean isSafe(Path dir, String prefix) {
        if (!dir.isAbsolute() || !Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        Path p = dir.normalize();
        if (p.getParent() == null || p.equals(Path.of(System.getProperty("user.home")).normalize())) {
            return false;
        }
        String s = p.toString().toLowerCase(Locale.ROOT).replace('\\', '/');
        boolean tempLike = s.contains("tmp") || s.contains("temp") || s.contains("cache");
        boolean reports = s.endsWith("/crashdumps") || s.endsWith("/wer/reportarchive") || s.endsWith("/wer/reportqueue");
        return tempLike || reports || (!prefix.isEmpty() && s.endsWith("/explorer"));
    }

    /** On Windows the owner looks like "PC\\user", so only the part after the backslash is compared. */
    static boolean isMine(Path file) {
        try {
            String owner = Files.getOwner(file, LinkOption.NOFOLLOW_LINKS).getName();
            return owner.substring(owner.lastIndexOf('\\') + 1).equalsIgnoreCase(System.getProperty("user.name"));
        } catch (IOException | UnsupportedOperationException e) {
            return false;
        }
    }

    private static List<Path> find(Path dir, Duration minAge, String prefix) {
        List<Path> found = new ArrayList<>();
        if (dir == null || !isSafe(dir, prefix)) {
            return found;
        }
        Instant cutoff = Instant.now().minus(minAge);
        try {
            // no FileVisitOption means links are not followed
            Files.walkFileTree(dir, EnumSet.noneOf(FileVisitOption.class), 6, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                    boolean old = a.lastModifiedTime().toInstant().isBefore(cutoff);
                    boolean named = f.getFileName().toString().startsWith(prefix);
                    if (a.isRegularFile() && old && named && isMine(f) && found.size() < 20000) {
                        found.add(f);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path f, IOException e) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // the folder disappeared: keep what was found
        }
        return found;
    }

    private static long size(List<Path> files) {
        long total = 0;
        for (Path f : files) {
            try {
                total += Files.size(f);
            } catch (IOException e) {
                // already gone
            }
        }
        return total;
    }
}
