package com.kummer.workstatus;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.apache.log4j.Appender;
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
import org.apache.log4j.FileAppender;

public class Log4jHelper implements UncaughtExceptionHandler {

    private static final String CRASH_REPORT_URL = "http://192.168.1.200:8123/api/webhook/work-status-phone-crashed";
    private static final Logger logger = getLogger(Log4jHelper.class.getName());
    private final UncaughtExceptionHandler defaultUEH;
    private static boolean isConfigured = false;
    private static String deviceName = "";
    private static String versionName = "";

    public static void configure(Context context) {
        if (!isConfigured) {
            if (context == null) {
                Log.e("Log4jHelper", "Failed to configure Log4j: Context is null");
                return;
            }

            final LogConfigurator logConfigurator = new LogConfigurator();
            File logDirectory = context.getExternalFilesDir(null);

            if (logDirectory != null) {
                String logFilePath = logDirectory.getAbsolutePath() + File.separator + "app.log";
                Log.i("Log4jHelper", "Log file path: " + logFilePath);
                logConfigurator.setFileName(logFilePath);
                logConfigurator.setRootLevel(Level.ALL); // Set to ALL for maximum logging
                logConfigurator.setLevel("org.apache", Level.ALL);
                logConfigurator.setUseFileAppender(true);
                logConfigurator.setFilePattern("%d{yyyy-MM-dd HH:mm:ss} [%t] %-5p %c{1} - %m%n");
                logConfigurator.setMaxFileSize(1024 * 1024); // Set the maximum log file size to 1MB
                logConfigurator.setMaxBackupSize(5); // Set the number of backup files to keep
                logConfigurator.setUseLogCatAppender(true); // Ensure Logcat appender is enabled
                logConfigurator.configure();

                //Force flushing
                Logger rootLogger = LogManager.getRootLogger();
                Appender fileAppender = rootLogger.getAppender("FILE");
                if (fileAppender instanceof FileAppender) {
                    ((FileAppender) fileAppender).setImmediateFlush(true);
                }
                // Set the uncaught exception handler
                Thread.setDefaultUncaughtExceptionHandler(new Log4jHelper());
                isConfigured = true;
            } else {
                Log.e("Log4jHelper", "Failed to configure Log4j: Log directory is null");
            }
            // Get device name and version name only once
            deviceName = getDeviceName();
            versionName = getVersionName(context);
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
        logger.error(String.format(Locale.US, "%s - %s - Uncaught exception on thread %s\n%s", versionName, timestamp, thread.getName(), stackTrace));

        // Send the crash report (in a separate thread)
        new Thread(() -> {
            try {
                sendCrashReport();
            } catch (Exception e) {
                logger.error("Error sending crash report", e);
            }
        }).start();

        // Pass the exception on to the default handler to avoid skipping the crash
        if(defaultUEH!=null){
            defaultUEH.uncaughtException(thread, throwable);
        }
    }

    public void sendNotification(String message) throws Exception {
        URL url = new URL(CRASH_REPORT_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                logger.info(versionName + " - " + deviceName + " - " + message + " sent successfully. Response code: " + responseCode);
            } else {
                logger.error(versionName + " - " + deviceName + " - " + "Failed to send notification. Response code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }
    public Log4jHelper(){
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }
    private void sendCrashReport() throws Exception {
        sendNotification("Crash report");
    }

    private static String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }
    private static String getVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Log4jHelper", "Failed to get version name", e);
            return "Unknown";
        }
    }
}