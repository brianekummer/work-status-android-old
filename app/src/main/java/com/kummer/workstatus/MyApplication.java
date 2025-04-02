package com.kummer.workstatus;

import android.app.Application;
import org.apache.log4j.Logger;

public class MyApplication extends Application {

    private static final Logger logger = Log4jHelper.getLogger(MyApplication.class.getName());

    @Override
    public void onCreate() {
        super.onCreate();
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