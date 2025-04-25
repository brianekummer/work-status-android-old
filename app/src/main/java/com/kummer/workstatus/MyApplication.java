package com.kummer.workstatus;

import android.app.Application;
import android.content.Intent;

import androidx.core.content.ContextCompat;

import org.apache.log4j.Logger;

public class MyApplication extends Application {

    private static final Logger logger = Log4jHelper.getLogger(MyApplication.class.getName());

    @Override
    public void onCreate() {
        super.onCreate();
        Log4jHelper.configure(this);
        Intent serviceIntent = new Intent(this, LoggingService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
        logger.info("MyApplication onCreate() called");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        logger.info("MyApplication onTerminate() called. App process is about to be killed.");

        // Send the notification in a separate thread
        new Thread(() -> {
            try {
                Log4jHelper log4jHelper = new Log4jHelper();
                log4jHelper.sendNotification("App terminated");
            } catch (Exception e) {
                logger.error("Error sending app termination notification", e);
            }
        }).start();
    }
}