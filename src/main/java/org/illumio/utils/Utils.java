package org.illumio.utils;

import java.io.*;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

public class Utils {

    public static void persistTimestamp(String filePath) {
        String timestamp = Instant.now().toString();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(timestamp);
            System.out.println("Timestamp persisted successfully.");
        } catch (IOException e) {
            System.err.println("Error saving timestamp: " + e.getMessage());
        }
    }

    public static String loadTimestamp(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            return reader.readLine();
        } catch (IOException e) {
            System.err.println("Error loading timestamp: " + e.getMessage());
            return null;
        }
    }

    public static boolean isMoreThanWeekOld(String timestamp) {
        if (timestamp == null) {
            return false;
        }

        try {
            Instant savedTimestamp = Instant.parse(timestamp);
            Instant now = Instant.now();
            Duration duration = Duration.between(savedTimestamp, now);

            return duration.toDays() > 7;
        } catch (DateTimeParseException e) {
            System.err.println("Error parsing timestamp: " + e.getMessage());
            return false;
        }
    }

}
