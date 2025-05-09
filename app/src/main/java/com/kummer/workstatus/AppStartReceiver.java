package com.kummer.workstatus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.apache.log4j.Logger;

class AppRestartReceiver extends BroadcastReceiver {
    private static final Logger logger = Log4jHelper.getLogger(AppRestartReceiver.class.getName());

    @Override
    public void onReceive(Context context, Intent intent) {
        logger.info("AppRestartReceiver: onReceive() called. App is being restarted by the AlarmManager"); // Added logging here

        // Create an intent to launch the main activity
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launchIntent != null) {
            // Clear the task stack and start the main activity
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);
        }
    }
}