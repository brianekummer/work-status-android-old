package com.kummer.workstatus;

import android.util.Log;

import timber.log.Timber;

public class FileLoggingTree extends Timber.DebugTree {

    private final LogToFile logger;

    public FileLoggingTree(LogToFile logger) {
        this.logger = logger;
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        // Log to Logcat as usual
        super.log(priority, tag, message, t);

        // Also write to file if the priority is at least warning
        if (priority >= Log.WARN) {
            String logMessage;
            switch (priority) {
                case Log.VERBOSE:
                    logMessage = "VERBOSE";
                    break;
                case Log.DEBUG:
                    logMessage = "DEBUG";
                    break;
                case Log.INFO:
                    logMessage = "INFO";
                    break;
                case Log.WARN:
                    logMessage = "WARN";
                    break;
                case Log.ERROR:
                    logMessage = "ERROR";
                    break;
                case Log.ASSERT:
                    logMessage = "ASSERT";
                    break;
                default:
                    logMessage = "UNKNOWN";
                    break;
            }
            logMessage += ": " + tag + " - " + message;

            logger.logEvent(logMessage);

            if (t != null) {
                logger.logEvent(t.toString());
            }
        }
    }
}