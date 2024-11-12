package utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final String LOG_FILE = "server.log";  // Path to the log file

    // Method to log informational messages
    public static void info(String message) {
        log("INFO", message);
    }

    // Method to log error messages
    public static void error(String message) {
        log("ERROR", message);
    }

    // Method to log debug messages
    public static void debug(String message) {
        log("DEBUG", message);
    }

    // Private method to write the log to a file and/or console
    private static void log(String level, String message) {
        String timestamp = getTimestamp();
        String logMessage = String.format("[%s] %s: %s", timestamp, level, message);

        // Write to the console
        System.out.println(logMessage);

        // Write to the log file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(LOG_FILE, true))) {
            writer.write(logMessage);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("Failed to write to the log file: " + e.getMessage());
        }
    }

    // Method to get the timestamp in the desired format
    private static String getTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }
}