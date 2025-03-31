package com.kummer.workstatus;

import android.content.Context;
import android.util.Log;

import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.mindpipe.android.logging.log4j.LogConfigurator;

public class Log4jHelper implements UncaughtExceptionHandler {

    private static final String CRASH_NOTIFICATION_URL = "http://192.168.1.200:8123/api/webhook/work-status-phone-crashed";
    private static final Logger logger = getLogger(Log4jHelper.class.getName());
    private UncaughtExceptionHandler defaultUEH;
    private static boolean isConfigured = false;

    public static void configure(Context context) {
        if (!isConfigured) {
            final LogConfigurator logConfigurator = new LogConfigurator();
            File logDirectory = context.getExternalFilesDir(null);

            if (logDirectory != null) {
                String logFilePath = logDirectory.getAbsolutePath() + File.separator + "app.log";
                Log.i("Log4jHelper", "Log file path: " + logFilePath);
                logConfigurator.setFileName(logFilePath);
                logConfigurator.setRootLevel(Level.INFO);
                logConfigurator.setLevel("org.apache", Level.ALL);
                logConfigurator.setUseFileAppender(true);
                logConfigurator.setFilePattern("%d{yyyy-MM-dd HH:mm:ss} [%t] %-5p %c{1} - %m%n");
                logConfigurator.setMaxFileSize(1024 * 1024); // Set the maximum log file size to 1MB
                logConfigurator.setMaxBackupSize(5); // Set the number of backup files to keep
                logConfigurator.setUseLogCatAppender(true);
                logConfigurator.configure();
                // Set the uncaught exception handler
                Thread.setDefaultUncaughtExceptionHandler(new Log4jHelper());
                isConfigured = true;
            } else {
                Log.e("Log4jHelper", "Failed to configure Log4j: Log directory is null");
            }
        }
    }

    public static Logger getLogger(String name) {
        return LogManager.getLogger(name);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        // Log the crash information
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = dateFormat.format(new Date());
        String stackTrace = Log.getStackTraceString(throwable);
        logger.error(String.format(Locale.US, "%s - Uncaught exception on thread %s\n%s", timestamp, thread.getName(), stackTrace));

        // Send the crash notification in a separate thread
        new Thread(() -> {
            try {
                sendCrashNotification();
            } catch (Exception e) {
                logger.error("Error sending crash report", e);
            }
        }).start();

        // Pass the exception on to the default handler to avoid skipping the crash
        if(defaultUEH!=null){
            defaultUEH.uncaughtException(thread, throwable);
        }
    }

    private void sendCrashNotification() throws Exception {
        URL url = new URL(CRASH_NOTIFICATION_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info("Crash notification sent successfully. Response code: " + responseCode);
            } else {
                logger.error("Failed to send crash notification. Response code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }
    public Log4jHelper(){
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }
}