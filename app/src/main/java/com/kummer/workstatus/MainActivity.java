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
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.HttpAuthHandler;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
    private WebView myWebView;
    private long appStartTimeMillis;
    private Handler memoryHandler = new Handler(Looper.getMainLooper());
    private Runnable memoryRunnable = new Runnable() {
        @Override
        public void run() {
            int logging_interval_minutes = 30;
            logMemoryUsage();
            memoryHandler.postDelayed(this, (long) logging_interval_minutes * 60 * 1000);
        }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        appStartTimeMillis = android.os.SystemClock.elapsedRealtime();
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

            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                logger.error("WebView render process gone on " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL +
                        ". App Uptime: " + getAppUptime() + // Add a helper to get app uptime for context
                        ". Crashed: " + detail.didCrash() +
                        ", Renderer Priority: " + detail.rendererPriorityAtExit());

                if (view != null) {
                    logger.info("Attempting to fully destroy and recreate WebView after render process gone.");
                    final ViewGroup parent = (ViewGroup) view.getParent();
                    if (parent != null) {
                        parent.removeView(view);
                    }
                    view.destroy();
                    myWebView = null; // Ensure old instance is cleared

                    // Re-inflate or create a new WebView instance and add it back
                    // This depends on how your layout is structured.
                    // If it's defined in XML:
                    // getLayoutInflater().inflate(R.layout.your_webview_container, parent, true);
                    // myWebView = parent.findViewById(R.id.main_activity_webview);
                    // Or if you create it programmatically:
                    myWebView = new WebView(MainActivity.this);
                    parent.addView(myWebView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                    // Ensure you re-apply all settings, WebViewClient, WebChromeClient, and load the URL
                    // It's best to encapsulate this in a method.
                    setupAndLoadWebView();

                    logger.info("WebView re-initialization process initiated.");
                } else {
                    logger.error("WebView instance was null in onRenderProcessGone. Cannot recover automatically. App may become unstable.");
                    // Consider more drastic recovery or user notification if this path is hit.
                }
                return true; // Crucial: Return true to indicate you've handled it.
            }
        });
    }

    // Helper method in your Activity
    private void setupAndLoadWebView() {
        // If myWebView is not null, ensure it's the correct new instance
        if (myWebView == null) { // Or re-find it if inflated from XML
            myWebView = findViewById(R.id.main_activity_webview); // Or however you get your WebView instance
            if (myWebView == null) {
                logger.fatal("Failed to re-initialize WebView (findViewById failed). App will likely fail.");
                // Potentially finish activity or show critical error
                return;
            }
        }

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // Apply any other settings you need (cache mode, DOM storage, etc.)
        // webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        // webSettings.setDomStorageEnabled(true);


        myWebView.setWebViewClient(new WebViewClient()); // Ensure this is the same class that has onRenderProcessGone
        // myWebView.setWebChromeClient(new YourCustomWebChromeClient()); // If you use one

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String url = prefs.getString("work_status_url", "");
        if (!url.isEmpty()) {
            logger.info("Reloading URL in newly initialized WebView: " + url);
            myWebView.loadUrl(url);
        } else {
            logger.warn("URL is empty after WebView re-initialization. Navigating to settings.");
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(i);
            // Potentially finish this activity if it can't operate without a URL
        }
    }

    // Example helper for uptime
    private String getAppUptime() {
        long currentElapsedTimeMillis = android.os.SystemClock.elapsedRealtime();
        long uptimeMillis = currentElapsedTimeMillis - appStartTimeMillis;
        long uptimeMinutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(uptimeMillis);
        return uptimeMinutes + " minutes";
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
    public void onConfigurationChanged(Configuration newConfig) {
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
        long heapSize = Runtime.getRuntime().totalMemory() / 1048576L;

        logger.info("---------------------");
        logger.info("Free Memory: " + freeMemory + " MB, " +
                "Total Memory: " + totalMemory + " MB, " +
                "Total PSS: " + totalPss + " KB, " +
                "Private Dirty: " + privateDirty + " KB, " +
                "Heap Size: " + heapSize + " MB");
    }
}