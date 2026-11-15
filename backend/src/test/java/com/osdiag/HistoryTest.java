package com.osdiag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** What is saved in the history tables, using the in-memory database. */
@SpringBootTest
@AutoConfigureMockMvc
class HistoryTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    Monitor monitor;
    @Autowired
    Recorder recorder;
    @Autowired
    PerformanceSampleRepo samples;
    @Autowired
    SecurityScanRepo securityScans;
    @Autowired
    DiskSnapshotRepo disks;
    @Autowired
    StartupSnapshotRepo startupSnapshots;
    @Autowired
    SystemActionLogRepo actions;
    @Autowired
    OptimizationRunRepo runs;

    @Test
    void aLiveSampleIsSavedAndTheLiveEndpointStillAnswers() throws Exception {
        monitor.tick();
        long before = samples.count();
        recorder.saveSample();
        assertEquals(before + 1, samples.count());
        recorder.saveSample();
        assertEquals(before + 1, samples.count(), "the same sample is not saved twice");
        mvc.perform(get("/api/live")).andExpect(status().isOk());
        mvc.perform(get("/api/history/performance")).andExpect(status().isOk()).andExpect(jsonPath("$[0].time").exists());
    }

    @Test
    void oldSamplesAreRemovedButNewOnesStay() {
        monitor.tick();
        recorder.saveSample();
        samples.deleteByTimeBefore(Instant.now().minusSeconds(60));
        assertTrue(samples.count() > 0, "a sample from just now is kept");
        assertTrue(samples.deleteByTimeBefore(Instant.now().plusSeconds(60)) > 0);
        assertEquals(0, samples.count());
    }

    @Test
    void aCheckupSavesTheDisksItMeasured() throws Exception {
        mvc.perform(post("/api/scan")).andExpect(status().isOk());
        var saved = disks.findByCapturedAtAfterOrderByCapturedAtDesc(Instant.now().minusSeconds(60), org.springframework.data.domain.PageRequest.of(0, 50));
        assertFalse(saved.isEmpty());
        assertNotNull(saved.get(0).getScanId());
        assertEquals(saved.get(0).getTotalSpace() - saved.get(0).getFreeSpace(), saved.get(0).getUsedSpace());
        mvc.perform(get("/api/history/disks")).andExpect(jsonPath("$[0].driveName").isString());
    }

    @Test
    void readingTheSecurityPageSavesNothingButAnExplicitScanDoes() throws Exception {
        long before = securityScans.count();
        mvc.perform(get("/api/security")).andExpect(status().isOk());
        assertEquals(before, securityScans.count());
        mvc.perform(post("/api/security/scan")).andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true));
        assertEquals(before + 1, securityScans.count());
        mvc.perform(get("/api/history/security")).andExpect(jsonPath("$[0].findings").isArray());
    }

    @Test
    void securityFindingsAreSavedWithTheirScan() {
        var found = List.of(new Security.Indicator("Warning", "Port 4444 is open", "details"),
                new Security.Indicator("Check", "Unusual startup item", "more details"));
        SecurityScan saved = recorder.recordSecurity("2 warning sign(s) found.", found);
        SecurityScan loaded = securityScans.findById(saved.getId()).orElseThrow();
        assertEquals(2, loaded.getTotalFindings());
        assertEquals(2, loaded.getFindings().size());
        assertEquals("Port 4444 is open", loaded.getFindings().get(0).getTitle());
    }

    @Test
    void startupSnapshotsAreSavedOnlyWhenSomethingChanged() {
        var items = List.of(new OsAdapter.Startup("Cloud Drive", "drive.exe --background", "Current user"));
        assertTrue(recorder.recordStartup(items, "Balanced", true));
        long count = startupSnapshots.count();
        assertFalse(recorder.recordStartup(items, "Balanced", false), "nothing changed");
        assertTrue(recorder.recordStartup(items, "High performance", false), "the power plan changed");
        assertTrue(recorder.recordStartup(items, "High performance", true), "the user asked for it");
        assertEquals(count + 2, startupSnapshots.count());
        assertFalse(recorder.recordStartup(List.of(), null, true), "an unreadable PC is not saved as empty");
        var latest = startupSnapshots.findFirstByOrderByCapturedAtDesc().orElseThrow();
        assertEquals("Cloud Drive", latest.getEntries().get(0).getName());
        assertEquals("High performance", latest.getPowerPlan());
    }

    @Test
    void actionsAreLoggedWithTheirOutcome() throws Exception {
        recorder.recordAction("POWER_PLAN", "balanced", "Power saver", "Power saver", false, "The power plan was not changed.");
        mvc.perform(get("/api/history/actions")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionType").value("POWER_PLAN")).andExpect(jsonPath("$[0].success").value(false));
        mvc.perform(post("/api/programs/close?name=no-such-program-xyz")).andExpect(status().isBadRequest());
        assertTrue(actions.findAll().stream().noneMatch(a -> "no-such-program-xyz".equals(a.getTarget())),
                "a refused request is not an executed action");
    }

    @Test
    void demoModeIsNeverSavedInTheHistoryTables() throws Exception {
        long securityBefore = securityScans.count();
        long actionsBefore = actions.count();
        long disksBefore = disks.count();
        long samplesBefore;
        long startupBefore;
        try {
            mvc.perform(post("/api/demo?mode=slow"));
            samplesBefore = samples.count();
            startupBefore = startupSnapshots.count();
            mvc.perform(post("/api/scan")).andExpect(jsonPath("$.demo").value(true));
            mvc.perform(post("/api/security/scan")).andExpect(jsonPath("$.saved").value(false));
            mvc.perform(post("/api/programs/close?name=chrome"));
            mvc.perform(get("/api/startup"));
            monitor.tick();
            recorder.saveSample();
            assertEquals(samplesBefore, samples.count());
            assertEquals(startupBefore, startupSnapshots.count());
        } finally {
            mvc.perform(post("/api/demo?mode=off"));
        }
        assertEquals(securityBefore, securityScans.count());
        assertEquals(actionsBefore, actions.count());
        assertEquals(disksBefore, disks.count());
    }

    @Test
    void anOptimizeSessionLinksTheCheckupThePlanAndTheCleanups() throws Exception {
        String json = mvc.perform(post("/api/scan")).andReturn().getResponse().getContentAsString();
        long scanId = ((Number) JsonPath.read(json, "$.id")).longValue();
        mvc.perform(get("/api/plan?scanId=" + scanId)).andExpect(status().isOk());
        mvc.perform(get("/api/plan?scanId=" + scanId)).andExpect(status().isOk());
        mvc.perform(post("/api/cleanup?what=temp&scanId=" + scanId)).andExpect(status().isOk());

        OptimizationRun run = runs.findFirstByScanIdOrderByStartedAtDesc(scanId).orElseThrow();
        assertEquals(4, run.getActions().size(), "asking for the plan twice makes one run");
        OptimizationAction temp = run.getActions().stream().filter(a -> a.getActionKey().equals("temp")).findFirst().orElseThrow();
        assertTrue(temp.isSelected());
        assertNotNull(temp.getCleanupId());
        assertNotNull(run.getFinishedAt());
        assertTrue(run.getActions().stream().filter(a -> !a.getActionKey().equals("temp")).noneMatch(OptimizationAction::isSelected));
        mvc.perform(get("/api/history/optimizations")).andExpect(jsonPath("$[0].actions").isArray());
    }

    @Test
    void historyStillWorksWithoutTheNewParameters() throws Exception {
        mvc.perform(get("/api/plan")).andExpect(status().isOk());
        mvc.perform(post("/api/cleanup?what=reports")).andExpect(status().isOk());
        mvc.perform(post("/api/scan"));
        mvc.perform(get("/api/scans?status=Healthy&days=1")).andExpect(status().isOk());
        mvc.perform(get("/api/cleanups?days=1")).andExpect(status().isOk());
    }
}
