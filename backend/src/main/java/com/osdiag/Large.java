package com.osdiag;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/** Finds the biggest files in a few folders. It only looks: these are personal files and are never deleted. */
public class Large {

    public record Entry(String path, long bytes, Instant changed) {
    }

    public static List<Entry> find(List<Path> folders, long minBytes, int limit) {
        List<Entry> found = new ArrayList<>();
        int[] visited = { 0 };
        for (Path folder : folders) {
            if (!Files.isDirectory(folder)) {
                continue;
            }
            try {
                Files.walkFileTree(folder, EnumSet.noneOf(FileVisitOption.class), 4, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                        if (a.isRegularFile() && a.size() >= minBytes) {
                            found.add(new Entry(f.toString(), a.size(), a.lastModifiedTime().toInstant()));
                        }
                        return ++visited[0] > 20000 ? FileVisitResult.TERMINATE : FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path f, IOException e) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                // the folder disappeared: keep what was found
            }
        }
        return found.stream().sorted(Comparator.comparingLong(Entry::bytes).reversed()).limit(limit).toList();
    }
}
