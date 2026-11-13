package com.osdiag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class OsAdapterTest {

    @Test
    void readsAWindowsRegistryLine() {
        var item = WindowsOs.parseRegistryLine("    OneDrive    REG_SZ    \"C:\\OneDrive.exe\" /background", "Current user");
        assertEquals("OneDrive", item.name());
        assertEquals("\"C:\\OneDrive.exe\" /background", item.command());
        assertNull(WindowsOs.parseRegistryLine("HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", "x"));
    }

    @Test
    void readsTheWindowsPowerPlan() {
        assertEquals("Balanced", WindowsOs.parsePlan("Power Scheme GUID: 381b4222-f694-41f0-9685-ff5bb260df2e  (Balanced)"));
    }

    @Test
    void readsALinuxAutostartFile() {
        var item = LinuxOs.parseDesktopFile("chat.desktop", List.of("[Desktop Entry]", "Name=Chat", "Exec=/usr/bin/chat --tray"));
        assertEquals("Chat", item.name());
        assertEquals("/usr/bin/chat --tray", item.command());
        assertNull(LinuxOs.parseDesktopFile("off.desktop", List.of("Name=Off", "Hidden=true")));
    }

    @Test
    void manyStartupItemsGetAdvice() {
        var items = java.util.Collections.nCopies(9, new OsAdapter.Startup("a", "a", "x"));
        assertEquals(1, StartupApi.tips(items, "Balanced").size());
        assertTrue(StartupApi.tips(List.of(), "Power saver").get(0).startsWith("Power saver"));
        assertTrue(StartupApi.tips(List.of(), null).isEmpty());
    }
}
