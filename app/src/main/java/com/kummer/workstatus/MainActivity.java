package com.kummer.workstatus;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.preference.PreferenceManager;
import org.apache.log4j.Logger;

public class MainActivity extends AppCompatActivity {

    private static final Logger logger = Log4jHelper.getLogger(MainActivity.class.getName());
    private static boolean log4jConfigured = false;
    private WebView myWebView;
    private final Handler memoryHandler = new Handler(Looper.getMainLooper());
    private final Runnable memoryRunnable = new Runnable() {
        @SuppressLint("SetJavaScriptEnabled")
        @Override
        public void run() {
            int logging_interval_minutes = 30; // Log memory every 30 minutes
            int refresh_interval_hours = 4; // Refresh every 4 hours

            logMemoryUsage();

            // Check if it's time to refresh the WebView
            long currentTime = System.currentTimeMillis();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            long lastRefreshTime = prefs.getLong("last_refresh_time", 0);

            // Check if 4 hours have passed since the last refresh
            if (currentTime - lastRefreshTime >= (long) refresh_interval_hours * 60 * 60 * 1000) {
                logger.info("---------------------");
                logger.info("refreshWebView() called");
                logMemoryUsage(); // Log memory before refresh
                logger.info("refreshWebView() before removing the webview");
                // Remove the old WebView from its parent
                if (myWebView != null) {
                    logger.info("refreshWebView() destroying the webview");
                    myWebView.destroy();
                    logger.info("refreshWebView() the webview has been destroyed");
                    myWebView = null;
                    logger.info("refreshWebView() the webview is null");
                }

                // Reconfigure webview
                if (myWebView == null) {
                    logger.info("refreshWebView() starting to reconfigure the webview");
                    myWebView = findViewById(R.id.main_activity_webview);
                    WebSettings webSettings = myWebView.getSettings();
                    webSettings.setJavaScriptEnabled(true);
                    webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    webSettings.setDomStorageEnabled(false);
                    webSettings.setDatabaseEnabled(false);
                    webSettings.setAllowFileAccess(false);
                    webSettings.setAllowContentAccess(false);
                    webSettings.setSupportMultipleWindows(false);
                    webSettings.setJavaScriptCanOpenWindowsAutomatically(false);
                    webSettings.setLoadsImagesAutomatically(true);
                    webSettings.setNeedInitialFocus(false);
                    logger.info("refreshWebView() reconfigure the webview finished");

                    // Load the URL again
                    logger.info("refreshWebView() getting the URL");
                    String url = prefs.getString("work_status_url", "");
                    logger.info("refreshWebView() the URL is: " + url);
                    if (!url.isEmpty()) {
                        logger.info("refreshWebView() loading the URL");
                        myWebView.loadUrl(url);
                        logger.info("refreshWebView() URL has been loaded");
                    } else {
                        logger.info("refreshWebView() url is empty, not loading url");
                    }
                }

                logMemoryUsage(); // Log memory after refresh
                logger.info("refreshWebView() ended");
                logger.info("---------------------");

                // Update the last refresh time
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong("last_refresh_time", currentTime);
                editor.apply();
            }

            memoryHandler.postDelayed(this, (long) logging_interval_minutes * 60 * 1000);
        }
    };

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
        myWebView = findViewById(R.id.main_activity_webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // TESTING- DOES THIS HELP MEMORY USAGE? WebView Settings (Memory Optimization)
        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); // Use cache, but fall back to network
        webSettings.setDomStorageEnabled(false); // Disable DOM storage
        webSettings.setDatabaseEnabled(false); // Disable database
        webSettings.setAllowFileAccess(false); // Disable file access
        webSettings.setAllowContentAccess(false); // Disable content access
        webSettings.setSupportMultipleWindows(false); // Disable multiple windows
        webSettings.setJavaScriptCanOpenWindowsAutomatically(false); // Disable js opening windows
        webSettings.setLoadsImagesAutomatically(true); // Load images
        webSettings.setNeedInitialFocus(false); // Disable need initial focus

        startMemoryLogging();

        // Set up the WebViewClient to handle errors
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Check if the error is ERR_CACHE_MISS
                if (error.getErrorCode() == android.webkit.WebViewClient.ERROR_UNKNOWN) {
                    String description = error.getDescription().toString();
                    if (description.contains("net::ERR_CACHE_MISS")) {
                        // This is the expected error, ignore it
                        return;
                    }
                }
                // Handle other errors (if needed)
                super.onReceivedError(view, request, error);
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                // Ignore it for now
                super.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                //ignore for now
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                //ignore for now
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                //ignore for now
                super.onReceivedSslError(view, handler, error);
            }

            @Override
            public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
                //ignore for now
                super.onReceivedHttpAuthRequest(view, handler, host, realm);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                //ignore for now
                return super.shouldOverrideUrlLoading(view, request);
            }
        });
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
            myWebView.loadUrl(url);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        logger.info("onConfigurationChanged() called");
        // No need to do anything else here since we are just handling the configuration change.
        // The WebView will keep displaying the same content.

        // Note that this isn't really necessary, but makes the app more stable when I rotate
        // the screen during debugging. Also added
        //    android:configChanges="orientation|screenSize">
        // in AndroidManifest.xml
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

    @Override
    protected void onStop() {
        super.onStop();
        logger.info("onDestroy() called, app is likely ending normally");

        // Send the notification on a new thread. Network operations, like sending HTTP requests,
        // must never be done on the main thread. Doing so could make the app unresponsive and crash.
        new Thread(() -> {
            try {
                Log4jHelper log4jHelper = new Log4jHelper();
                log4jHelper.sendNotification("App finished in onStop()");
            } catch (Exception e) {
                logger.error("Error sending app finish notification", e);
            }
        }).start();
    }

    private void startMemoryLogging() {
        memoryHandler.post(memoryRunnable);
    }

    private void logMemoryUsage() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        long freeMemory = memoryInfo.availMem / 1048576L; // Convert to MB
        long totalMemory = memoryInfo.totalMem / 1048576L;

        Debug.MemoryInfo debugMemoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(debugMemoryInfo);
        long totalPss = debugMemoryInfo.getTotalPss();
        long privateDirty = debugMemoryInfo.getTotalPrivateDirty();

        // New Memory Information
        long totalAllocatedMemory = Runtime.getRuntime().totalMemory() / 1048576L; // Convert to MB
        long totalFreeMemory = Runtime.getRuntime().freeMemory() / 1048576L; // Convert to MB
        long maxHeapSize = Runtime.getRuntime().maxMemory() / 1048576L; // Convert to MB

        logger.info("---------------------");
        logger.info("Free Memory: " + freeMemory + " MB, " +
                "Total Memory: " + totalMemory + " MB, " +
                "Total PSS: " + totalPss + " KB, " +
                "Private Dirty: " + privateDirty + " KB, " +
                "Total Allocated Memory: " + totalAllocatedMemory + " MB, " +
                "Total Free Memory: " + totalFreeMemory + " MB, " +
                "Max Heap Size: " + maxHeapSize + " MB");
    }
}