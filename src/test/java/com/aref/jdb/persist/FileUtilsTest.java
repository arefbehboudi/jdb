package com.aref.jdb.persist;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;


class FileUtilsTest {

    private final String testFilePath = "testFile.txt";

    @AfterEach
    void cleanup() {
        File file = new File(testFilePath);
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    void testSaveData_CreatesFileSuccessfully() throws IOException {
        byte[] data = "Hello, World!".getBytes();

        FileUtils.saveData(testFilePath, data);

        Path path = Paths.get(testFilePath);
        assertTrue(Files.exists(path), "File should exist after saving.");
        assertArrayEquals(data, Files.readAllBytes(path), "File content should match.");
    }

    @Test
    void testSaveData_OverwritesExistingFile() throws IOException {
        Path path = Paths.get(testFilePath);
        Files.write(path, "Old Data".getBytes());

        byte[] newData = "New Data".getBytes();
        FileUtils.saveData(testFilePath, newData);

        assertArrayEquals(newData, Files.readAllBytes(path), "File should contain new data.");
    }

    @Test
    void testSaveData_FailsOnInvalidPath() {
        String invalidPath = "/invalid/path/testFile.txt";
        byte[] data = "Test Data".getBytes();

        IOException exception = assertThrows(IOException.class, () -> FileUtils.saveData(invalidPath, data));
        assertNotNull(exception.getMessage(), "Exception should contain an error message.");
    }

}