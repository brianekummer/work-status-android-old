package com.kummer.workstatus;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class LogToFile implements UncaughtExceptionHandler {

    private final String logDirectoryName = "work-status-logs";
    private final long maxLogFileSize = 1024 * 1024; // 1 MB
    private final int maxLogFiles = 5; // Keep up to 5 log files
    private final Context context;
    private UncaughtExceptionHandler defaultUEH;

    public LogToFile(Context context) {
        this.context = context;
        // Get the current default uncaught exception handler
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        // Set this class as the new default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    private String logFileName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return "app_log_" + dateFormat.format(new Date()) + ".txt";
    }

    public void logEvent(String message) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        String logMessage = timestamp + " - " + message + "\n";

        try {
            File logDirectory = getLogDirectory();
            if (logDirectory == null) {
                Log.e("LogToFile", "Could not access external storage for logging.");
                return;
            }

            rotateLogFiles(logDirectory);
            File file = new File(logDirectory, logFileName());
            try (FileOutputStream outputStream = new FileOutputStream(file, true)) {
                outputStream.write(logMessage.getBytes());
                if (file.length() > maxLogFileSize) {
                    Log.i("LogToFile", "Log file reached max size, will be rotated on the next write");
                }
            }
        } catch (Exception e) {
            Log.e("LogToFile", "Error writing to log file", e);
        }
    }

    private void rotateLogFiles(File logDirectory) {
        File[] files = logDirectory.listFiles(file -> file.getName().startsWith("app_log_"));
        if (files != null && files.length >= maxLogFiles) {
            // Sort files by modification date (oldest first)
            Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            files[0].delete(); // Delete the oldest file
        }
    }

    private File getLogDirectory() {
        File directory = context.getExternalFilesDir(null); // Use app-private directory

        if (directory == null) {
            Log.e("LogToFile", "Failed to get app-private external storage directory.");
            return null;
        }

        // Create the directory if it doesn't exist
        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                Log.e("LogToFile", "Failed to create directory: " + directory.getAbsolutePath());
                return null;
            }
        }

        return directory;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // Log the crash information
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        logEvent(String.format(Locale.US, "%s - Uncaught exception on thread %s\n%s", timestamp, thread.getName(), Log.getStackTraceString(throwable)));

        // Send the crash notification in a separate thread
        new Thread(() -> {
            try {
                sendCrashNotification();
            } catch (IOException e) {
                Log.e("LogToFile", "Error sending crash report", e);
            }
        }).start();

        // Pass the exception on to the default handler to avoid skipping the crash
        defaultUEH.uncaughtException(thread, throwable);
    }

    private void sendCrashNotification() throws IOException {
        URL url = new URL("http://192.168.1.200:8123/api/webhook/work-status-phone-crashed");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                Log.i("LogToFile", "Crash notification sent successfully. Response code: " + responseCode);
            } else {
                Log.e("LogToFile", "Failed to send notification. Response code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }
}