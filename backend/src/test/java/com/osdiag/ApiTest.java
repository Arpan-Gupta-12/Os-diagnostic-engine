package com.osdiag;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** The whole backend through the REST API with an in-memory database. Cleanups use folders under target/ only. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void infoLiveAndProcesses() throws Exception {
        mvc.perform(get("/api/info")).andExpect(status().isOk()).andExpect(jsonPath("$.cores").isNumber());
        mvc.perform(get("/api/live")).andExpect(status().isOk());
        mvc.perform(get("/api/procs?sort=cpu")).andExpect(status().isOk()).andExpect(jsonPath("$[0].pid").isNumber());
    }

    @Test
    void scanIsSavedInHistory() throws Exception {
        mvc.perform(post("/api/scan")).andExpect(status().isOk()).andExpect(jsonPath("$.summary").isString());
        mvc.perform(get("/api/scans")).andExpect(status().isOk()).andExpect(jsonPath("$[*].status", hasItem(org.hamcrest.Matchers.notNullValue())));
    }

    @Test
    void cleanupRemovesOldFilesAndChecksTheResult() throws Exception {
        Path dir = Files.createDirectories(Path.of("target/test-tmp").toAbsolutePath());
        Path old = Files.write(dir.resolve("old.tmp"), new byte[4096]);
        Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(3, ChronoUnit.DAYS)));
        Path fresh = Files.write(dir.resolve("fresh.tmp"), new byte[10]);

        mvc.perform(get("/api/cleanup/preview")).andExpect(jsonPath("$.files").value(1));
        assertTrue(Files.exists(old), "preview changes nothing");

        mvc.perform(post("/api/cleanup")).andExpect(status().isOk())
                .andExpect(jsonPath("$.files").value(1))
                .andExpect(jsonPath("$.verified").value(true));
        assertFalse(Files.exists(old));
        assertTrue(Files.exists(fresh), "recent files are kept");
        mvc.perform(get("/api/cleanups")).andExpect(jsonPath("$[0].files").value(1));
    }

    @Test
    void thumbnailsAreCleanedLikeTempFiles() throws Exception {
        Path dir = Files.createDirectories(Path.of("target/test-cache").toAbsolutePath());
        Path old = Files.write(dir.resolve("thumb.png"), new byte[2048]);
        Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(3, ChronoUnit.DAYS)));

        mvc.perform(post("/api/cleanup?what=thumbnails")).andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.what").value("thumbnails"));
        assertFalse(Files.exists(old));
        mvc.perform(post("/api/cleanup?what=nonsense")).andExpect(status().isBadRequest());
    }

    @Test
    void limitsAreValidatedSavedAndReset() throws Exception {
        mvc.perform(put("/api/limits").contentType("application/json").content("{\"cpu\": 5}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("cpu must be greater than or equal to 20"));
        mvc.perform(put("/api/limits").contentType("application/json").content("{\"cpu\": 70}"))
                .andExpect(jsonPath("$.cpu").value(70)).andExpect(jsonPath("$.mem").value(85));
        mvc.perform(get("/api/limits")).andExpect(jsonPath("$.cpu").value(70));
        mvc.perform(delete("/api/limits")).andExpect(jsonPath("$.cpu").value(85));
        mvc.perform(get("/api/limits")).andExpect(jsonPath("$.cpu").value(85));
    }

    @Test
    void demoModeUsesSampleDataAndTouchesNothing() throws Exception {
        Path dir = Files.createDirectories(Path.of("target/test-tmp").toAbsolutePath());
        Path old = Files.write(dir.resolve("demo-keep.tmp"), new byte[100]);
        Files.setLastModifiedTime(old, FileTime.from(Instant.now().minus(3, ChronoUnit.DAYS)));
        try {
            mvc.perform(post("/api/demo?mode=slow")).andExpect(status().isOk());
            mvc.perform(get("/api/info")).andExpect(jsonPath("$.demo").value("slow"));
            mvc.perform(post("/api/scan")).andExpect(jsonPath("$.status").value("Critical")).andExpect(jsonPath("$.demo").value(true));
            mvc.perform(post("/api/cleanup")).andExpect(jsonPath("$.demo").value(true));
            assertTrue(Files.exists(old), "demo mode never deletes");
            mvc.perform(get("/api/startup")).andExpect(jsonPath("$.items.length()").value(11));
            mvc.perform(post("/api/demo?mode=bad")).andExpect(status().isBadRequest());
        } finally {
            mvc.perform(post("/api/demo?mode=off"));
            Files.deleteIfExists(old);
        }
        mvc.perform(get("/api/scans")).andExpect(jsonPath("$[?(@.demo == true)]").isEmpty());
    }

    @Test
    void historyCanBeFilteredAndExported() throws Exception {
        mvc.perform(post("/api/scan"));
        mvc.perform(get("/api/scans?status=Nothing")).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/scans?days=1")).andExpect(jsonPath("$").isNotEmpty());
        mvc.perform(get("/api/scans.csv")).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(
                        org.hamcrest.Matchers.startsWith("time,status,score")));
    }

    @Test
    void startupReviewAlwaysAnswers() throws Exception {
        mvc.perform(get("/api/startup")).andExpect(status().isOk()).andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void thePlanListsCleanupsAndAdvice() throws Exception {
        mvc.perform(get("/api/plan")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.kind == 'clean')].key").value(org.hamcrest.Matchers.hasItems("temp", "thumbnails", "browser", "reports")))
                .andExpect(jsonPath("$[?(@.key == 'startup')]").isNotEmpty());
    }

    @Test
    void securityCheckAnswersAndDemoShowsWarningSigns() throws Exception {
        mvc.perform(get("/api/security")).andExpect(status().isOk()).andExpect(jsonPath("$.checked").isArray());
        try {
            mvc.perform(post("/api/demo?mode=slow"));
            mvc.perform(get("/api/security")).andExpect(jsonPath("$.indicators.length()").value(org.hamcrest.Matchers.greaterThan(3)));
            mvc.perform(post("/api/cleanup?what=browser")).andExpect(jsonPath("$.demo").value(true));
        } finally {
            mvc.perform(post("/api/demo?mode=off"));
        }
        mvc.perform(get("/api/security")).andExpect(jsonPath("$.indicators[?(@.title =~ /.*svch0st.*/)]").isEmpty());
    }

    @Test
    void closingAndPowerSwitchingNeedValidRequestsAndDemoChangesNothing() throws Exception {
        mvc.perform(post("/api/programs/close?name=no-such-program-xyz")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/programs/close?name=java")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/power?plan=turbo")).andExpect(status().isBadRequest());
        try {
            mvc.perform(post("/api/demo?mode=slow"));
            mvc.perform(post("/api/programs/close?name=chrome")).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Demo:")));
            mvc.perform(post("/api/power?plan=balanced")).andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Demo:")));
            mvc.perform(get("/api/plan")).andExpect(jsonPath("$[?(@.key == 'programs')].closable[0]").isNotEmpty())
                    .andExpect(jsonPath("$[?(@.key == 'large')]").isNotEmpty());
        } finally {
            mvc.perform(post("/api/demo?mode=off"));
        }
    }
}
