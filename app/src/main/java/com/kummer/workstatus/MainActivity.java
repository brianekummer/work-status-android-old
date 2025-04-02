package com.kummer.workstatus;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;
import org.apache.log4j.Logger;

public class MainActivity extends AppCompatActivity {

    private static final Logger logger = Log4jHelper.getLogger(MainActivity.class.getName());
    private static boolean log4jConfigured = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main_activity);

        // Configure Log4j only once
        if (!log4jConfigured) {
            Log4jHelper.configure(this);
            log4jConfigured = true;
        }
        logger.info("App Started");

        Window window = getWindow();

        // Always keep the screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Hide the status bar and navigation bar
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(window, window.getDecorView());
        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());

        // Set up the webview
        WebView myWebView = findViewById(R.id.main_activity_webview);
        myWebView.getSettings().setJavaScriptEnabled(true);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onStart() {
        super.onStart();
        logger.info("onStart() called"); // Added logging here
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String url = prefs.getString("work_status_url", "");
        if (url.isEmpty()) {
            logger.info("work_status_url is empty. Navigating to SettingsActivity"); // Added logging here

            // If do not have the URL, navigate to settings activity so user can set it
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(i);
        } else {
            logger.info("Loading URL: " + url); // Added logging here
            //This line was changed, as it was causing a crash when the url was empty
            WebView myWebView = findViewById(R.id.main_activity_webview);
            myWebView.loadUrl(url);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        logger.info("onDestroy() called, app is likely ending normally");

        // Send the notification on a new thread. Network operations, like sending HTTP requests,
        // must never be done on the main thread. Doing so could make the app unresponsive and crash.
        new Thread(() -> {
            try {
                Log4jHelper log4jHelper = new Log4jHelper();
                log4jHelper.sendNotification("App finished in onDestroy()");
            } catch (Exception e) {
                logger.error("Error sending app finish notification", e);
            }
        }).start();
    }
}