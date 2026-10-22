package com.osdiag;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** The whole backend through the REST API with an in-memory database. Cleanup uses target/test-tmp only. */
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
}
