package com.aref.jdb.persist;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LogUtilsTest {

    private final String testLogFile = "testlog.log";

    @BeforeEach
    void setup() throws IOException {
        new File(testLogFile).delete();
    }

    @AfterEach
    void cleanup() {
        new File(testLogFile).delete();
    }

    @Test
    void testLogCreate_CreatesFileSuccessfully() throws IOException {
        RandomAccessFile logFile = LogUtils.logCreate(testLogFile);
        assertTrue(Files.exists(Paths.get(testLogFile)), "Log file should exist after creation.");
        logFile.close();
    }

    @Test
    void testLogAppend_WritesDataCorrectly() throws IOException {
        RandomAccessFile logFile = LogUtils.logCreate(testLogFile);
        LogUtils.logAppend(logFile, "First log entry");
        LogUtils.logAppend(logFile, "Second log entry");
        logFile.close();

        List<String> lines = Files.readAllLines(Paths.get(testLogFile));
        assertEquals(2, lines.size(), "File should contain two log entries.");
        assertEquals("First log entry", lines.get(0), "First line should match.");
        assertEquals("Second log entry", lines.get(1), "Second line should match.");
    }

    @Test
    void testLogAppend_AppendsToExistingFile() throws IOException {
        Path path = Paths.get(testLogFile);
        Files.write(path, "Existing log entry\n".getBytes());

        RandomAccessFile logFile = LogUtils.logCreate(testLogFile);
        LogUtils.logAppend(logFile, "New log entry");
        logFile.close();

        List<String> lines = Files.readAllLines(path);
        assertEquals(2, lines.size(), "File should contain two log entries.");
        assertEquals("Existing log entry", lines.get(0), "First line should be unchanged.");
        assertEquals("New log entry", lines.get(1), "New line should be appended.");
    }
}
