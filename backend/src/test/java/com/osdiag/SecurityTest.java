package com.osdiag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class SecurityTest {

    static Proc proc(int pid, String name, String path) {
        return new Proc(pid, name, 1.0, 1000, "me", path);
    }

    @Test
    void anOrdinaryPcHasNoWarningSigns() {
        var found = Security.check(new Security.Input(
                List.of(proc(1, "svchost.exe", "C:\\Windows\\System32\\svchost.exe"),
                        proc(2, "chrome.exe", "C:\\Program Files\\Google\\Chrome\\chrome.exe")),
                List.of(new OsAdapter.Startup("OneDrive", "C:\\OneDrive.exe /background", "Current user")),
                List.of("# comment", "127.0.0.1 localhost", "0.0.0.0 ads.example"), List.of(new Listener(135, 4)),
                List.of(new Listener(443, 2)), true));
        assertTrue(found.isEmpty());
    }

    @Test
    void aProgramInTheTempFolderIsReported() {
        var found = Security.riskyFolders(List.of(proc(1, "setup.exe", "C:\\Users\\me\\AppData\\Local\\Temp\\setup.exe")));
        assertEquals(1, found.size());
        assertEquals("Check", found.get(0).level());
    }

    @Test
    void systemLookAlikesAreReported() {
        var found = Security.lookAlikes(List.of(proc(1, "svch0st.exe", ""), proc(2, "lsass.exe", "C:\\Users\\me\\lsass.exe"),
                proc(3, "lsass.exe", "C:\\Windows\\System32\\lsass.exe"), proc(4, "svchost.exe", "")));
        assertEquals(2, found.size());
        assertEquals("Warning", found.get(0).level());
    }

    @Test
    void protectedProgramsWithoutAPathAreNotAccused() {
        assertTrue(Security.lookAlikes(List.of(proc(4, "svchost.exe", null))).isEmpty());
    }

    @Test
    void hiddenEncodedStartupCommandsAreWarnings() {
        var found = Security.startupRisks(List.of(
                new OsAdapter.Startup("Updater", "powershell -WindowStyle Hidden -EncodedCommand SQBF", "Current user"),
                new OsAdapter.Startup("Tool", "wscript C:\\tool.vbs", "Current user")));
        assertEquals(List.of("Warning", "Check"), found.stream().map(Security.Indicator::level).toList());
    }

    @Test
    void aHostsRedirectIsReportedButBlockingIsNot() {
        var found = Security.hostsRedirects(List.of("0.0.0.0 ads.example", "203.0.113.7 www.mybank.example"));
        assertEquals(1, found.size());
        assertTrue(found.get(0).title().contains("www.mybank.example"));
    }

    @Test
    void remoteAccessPortsAreNamedWithTheirProgram() {
        var found = Security.openPorts(List.of(new Listener(3389, 7), new Listener(3389, 7), new Listener(80, 8)),
                List.of(proc(7, "termsvc.exe", "")));
        assertEquals(1, found.size());
        assertTrue(found.get(0).text().contains("termsvc.exe"));
    }

    @Test
    void namesAreComparedLetterByLetter() {
        assertEquals(1, Security.distance("svch0st", "svchost"));
        assertEquals(0, Security.distance("lsass", "lsass"));
    }

    @Test
    void aConnectionToABotnetPortIsAWarning() {
        var found = Security.badConnections(List.of(new Listener(6667, 9), new Listener(443, 9)), List.of(proc(9, "tool.exe", "")));
        assertEquals(1, found.size());
        assertTrue(found.get(0).title().contains("tool.exe"));
    }

    @Test
    void readsAScheduledTaskAndSkipsWindowsOwnTasks() {
        String mine = "\"PC\",\"\\Updater\",\"N/A\",\"Ready\",\"Interactive\",\"N/A\",\"1\",\"me\",\"C:\\Temp\\up.exe\",\"x\"";
        String windows = mine.replace("\\Updater", "\\Microsoft\\Windows\\Defrag");
        assertEquals("C:\\Temp\\up.exe", WindowsOs.parseTaskLine(mine).command());
        assertEquals("Updater", WindowsOs.parseTaskLine(mine).name());
        assertNull(WindowsOs.parseTaskLine(windows));
    }
}
