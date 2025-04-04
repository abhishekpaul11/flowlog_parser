package org.illumio.utils;

import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {

    private static final String TEST_FILE = "src/test/resources/test_timestamp.txt";

    @AfterAll
    static void cleanUp() throws IOException {
        Files.deleteIfExists(new File(TEST_FILE).toPath());
    }

    @Test
    void testPersistAndLoadTimestamp() {
        Utils.persistTimestamp(TEST_FILE);
        String loaded = Utils.loadTimestamp(TEST_FILE);

        assertNotNull(loaded);
        assertDoesNotThrow(() -> Instant.parse(loaded)); // valid ISO timestamp
    }

    @Test
    void testIsOlderThanAWeekReturnsTrueForOldTimestamp() {
        Instant oldInstant = Instant.now().minus(Duration.ofDays(8));
        assertTrue(Utils.isOlderThanAWeek(oldInstant.toString()));
    }

    @Test
    void testIsOlderThanAWeekReturnsFalseForRecentTimestamp() {
        Instant recent = Instant.now().minus(Duration.ofDays(2));
        assertFalse(Utils.isOlderThanAWeek(recent.toString()));
    }

    @Test
    void testIsOlderThanAWeekReturnsTrueForNull() {
        assertTrue(Utils.isOlderThanAWeek(null));
    }

    @Test
    void testIsOlderThanAWeekReturnsTrueForInvalidFormat() {
        assertTrue(Utils.isOlderThanAWeek("not-a-valid-timestamp"));
    }

    @Test
    void testLoadTimestampReturnsNullIfFileNotFound() {
        String result = Utils.loadTimestamp("non_existent_file.txt");
        assertNull(result);
    }
}
