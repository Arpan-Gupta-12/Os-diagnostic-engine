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


public class Cleaner {

    public record Result(int files, long bytes, int failed) {
    }

    public static Result preview(Path dir, Duration minAge) {
        List<Path> files = find(dir, minAge);
        return new Result(files.size(), size(files), 0);
    }

    public static Result clean(Path dir, Duration minAge) {
        int removed = 0;
        int failed = 0;
        long bytes = 0;
        for (Path f : find(dir, minAge)) {
            try {
                long size = Files.size(f);
                Files.delete(f);
                removed++;
                bytes += size;
            } catch (IOException e) {
                failed++; // in use (Windows) or no permission
            }
        }
        return new Result(removed, bytes, failed);
    }

    static boolean isSafe(Path dir) {
        if (!dir.isAbsolute() || !Files.isDirectory(dir, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        Path p = dir.normalize();
        if (p.getParent() == null || p.equals(Path.of(System.getProperty("user.home")).normalize())) {
            return false;
        }
        String s = p.toString().toLowerCase(Locale.ROOT);
        return s.contains("tmp") || s.contains("temp") || s.contains("cache");
    }

    /** On Windows the owner looks like "PC\\user", so only the part after the backslash is compared. */
    static boolean isMine(Path file) {
        try {
            String owner = Files.getOwner(file, LinkOption.NOFOLLOW_LINKS).getName();
            return owner.substring(owner.lastIndexOf('\\') + 1).equalsIgnoreCase(System.getProperty("user.name"));
        } catch (IOException | UnsupportedOperationException e) {
            return false; // cannot tell who owns it: do not touch it
        }
    }

    private static List<Path> find(Path dir, Duration minAge) {
        List<Path> found = new ArrayList<>();
        if (!isSafe(dir)) {
            return found;
        }
        Instant cutoff = Instant.now().minus(minAge);
        try {
            // an empty option set means links are NOT followed
            Files.walkFileTree(dir, EnumSet.noneOf(FileVisitOption.class), 6, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                    boolean old = a.lastModifiedTime().toInstant().isBefore(cutoff);
                    if (a.isRegularFile() && old && isMine(f) && found.size() < 20000) {
                        found.add(f);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path f, IOException e) {
                    return FileVisitResult.CONTINUE; // cannot read it: skip
                }
            });
        } catch (IOException e) {
            // folder disappeared: keep what we found
        }
        return found;
    }

    private static long size(List<Path> files) {
        long total = 0;
        for (Path f : files) {
            try {
                total += Files.size(f);
            } catch (IOException e) {
                // removed in the meantime
            }
        }
        return total;
    }
}
