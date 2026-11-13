package com.osdiag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OptimizeTest {

    static final long GB = 1024L * 1024 * 1024;

    @TempDir
    Path tmp;

    static Proc proc(String name, String user, long mem) {
        return new Proc(5000, name, 1.0, mem, user, "");
    }

    @Test
    void onlyMyOwnNonSystemProgramsCanBeClosed() {
        String me = System.getProperty("user.name");
        assertTrue(CloseApi.closable(proc("chrome", me, GB)));
        assertTrue(CloseApi.closable(proc("Chrome.exe", "PC\\" + me, GB)));
        assertFalse(CloseApi.closable(proc("chrome", "someone-else", GB)));
        assertFalse(CloseApi.closable(proc("chrome", null, GB)));
        assertFalse(CloseApi.closable(proc("svchost.exe", me, GB)));
        assertFalse(CloseApi.closable(proc("explorer", me, GB)));
    }

    @Test
    void thisAppIsNeverClosed() {
        String me = System.getProperty("user.name");
        assertFalse(CloseApi.closable(new Proc((int) ProcessHandle.current().pid(), "anything", 1.0, GB, me, "")));
    }

    @Test
    void heavyProgramsGetCloseButtonsOnlyWhenTheyMayBeClosed() {
        String me = System.getProperty("user.name");
        Sample busy = new Sample(Instant.now(), 95.0, 95.0, GB, 8 * GB, 0.0, 100, 10.0, 1.0, 50.0);
        var actions = Plan.advice(busy, List.of(proc("editor", me, 3 * GB), proc("svchost.exe", me, 4 * GB)), new Limits(),
                List.of(), "Balanced", List.of());
        var programs = actions.stream().filter(a -> a.key().equals("programs")).findFirst().orElseThrow();
        assertEquals(List.of("editor"), programs.closable());
    }

    @Test
    void aSavingPowerPlanGetsASwitchOffer() {
        var actions = Plan.advice(null, List.of(), new Limits(), List.of(), "Power saver", List.of());
        assertTrue(actions.stream().anyMatch(a -> a.key().equals("power-saver")));
        assertTrue(Plan.advice(null, List.of(), new Limits(), List.of(), "Balanced", List.of()).stream()
                .noneMatch(a -> a.key().equals("power-saver")));
    }

    @Test
    void bigFilesAreListedBiggestFirstAndNeverTouched() throws Exception {
        Path small = Files.write(tmp.resolve("small.bin"), new byte[10]);
        Path big = Files.write(tmp.resolve("big.bin"), new byte[3000]);
        Path bigger = Files.write(tmp.resolve("bigger.bin"), new byte[5000]);
        var found = Large.find(List.of(tmp, tmp.resolve("missing")), 1000, 5);
        assertEquals(2, found.size());
        assertEquals(bigger.toString(), found.get(0).path());
        assertTrue(Files.exists(small) && Files.exists(big) && Files.exists(bigger));
    }
}
