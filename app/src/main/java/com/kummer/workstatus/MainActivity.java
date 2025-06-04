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
        EdgeToEdge.enable(this);
        setContentView(R.layout.main_activity);

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

        // Start logging memory usage
        memoryHandler.post(memoryRunnable);

        // Set up the webview
        setupAndLoadWebView();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupAndLoadWebView() {
        if (myWebView == null) {
            myWebView = findViewById(R.id.main_activity_webview);
            if (myWebView == null) {
                logger.fatal("Failed to re-initialize WebView (findViewById failed). App will likely fail.");
                return;
            }
        }

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // Check if the error is ERR_CACHE_MISS, in which case the browser/webview failed
                // to retrieve cache. This is expected since caching is not used for this site,
                // so do nothing.
                if (error.getErrorCode() == android.webkit.WebViewClient.ERROR_UNKNOWN) {
                    String description = error.getDescription().toString();
                    if (description.contains("net::ERR_CACHE_MISS")) {
                        return;
                    }
                }
                else if (request.isForMainFrame()) {
                    logger.error("WebView error on main frame: " + error.getErrorCode() + " " + error.getDescription());
                }
                // Handle other errors (if needed)
                super.onReceivedError(view, request, error);
            }

            // TODO- Remove a bunch of these overridesIs Android even upgrading the webview
            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                // Ignore it for now
                super.onReceivedHttpError(view, request, errorResponse);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                logger.info("WebView page started: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                logger.info("WebView page finished: " + url);
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
                // TODO- HAVE GEMINI ADD DOCUMENTATION AND EXPLAIN THIS TO ME
                logger.error("WebView render process gone on " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL +
                        ". Crashed: " + detail.didCrash() +
                        ", Renderer Priority: " + detail.rendererPriorityAtExit());

                if (view != null) {
                    logger.info("Attempting to fully destroy and recreate WebView after render process gone.");
                    //***** START TODO- EXTRACT INTO FN *****//
                    final ViewGroup parent = (ViewGroup) view.getParent();
                    if (parent != null) {
                        parent.removeView(view);
                    }
                    view.destroy();
                    myWebView = null; // Ensure old instance is cleared
                    //***** END EXTRACT *****//

                    // Create a new WebView instance and add it back
                    myWebView = new WebView(MainActivity.this);
                    parent.addView(myWebView, new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));

                    // Add it back to the parent (assuming the parent is a FrameLayout or similar)
                    // If your original WebView was directly in the XML layout, you might need
                    // a container FrameLayout to add the new WebView into.
                    // For simplicity, if 'main_activity_webview_container' is a FrameLayout in your XML:
                    // FrameLayout container = findViewById(R.id.main_activity_webview_container);
                    // container.addView(myWebView);
                    // If the original WebView was the root or you're replacing it directly
                    // and its parent can take a new child this way:
                    if (parent != null) {
                        parent.addView(myWebView);
                    } else {
                        // Fallback: If parent is null, re-set content view, then re-find a container
                        // This is less ideal. Better to have a dedicated container.
                        setContentView(R.layout.main_activity); // Re-inflate
                        // Re-initialize myWebView if it was part of the layout
                        // myWebView = findViewById(R.id.main_activity_webview);
                        // This path needs careful handling depending on your layout structure.
                        logger.error("WebView parent was null, complex recovery needed.");
                        return false; // Indicate recovery might not be complete
                    }

                    setupAndLoadWebView();

                    logger.info("WebView re-initialization process initiated.");
                } else {
                    logger.error("WebView instance was null in onRenderProcessGone. Cannot recover automatically. App may become unstable.");
                    // Consider more drastic recovery or user notification if this path is hit.
                }
                return true; // Crucial: Return true to indicate you've handled it.
            }
        });

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String url = prefs.getString("work_status_url", "");
        if (!url.isEmpty()) {
            logger.info("Loading URL in WebView: " + url);
            myWebView.loadUrl(url);
        } else {
            logger.info("URL is empty after WebView initialization/re-initialization. Navigating to settings.");
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(i);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        logger.info("onStart() called");
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        // This isn't really necessary, but makes the app more stable when I rotate
        // the screen when mounting/unmounting the phone on the wall. There is no
        // need to do anything else here since we are just handling the configuration
        // change- the WebView will keep displaying the same content.
        super.onConfigurationChanged(newConfig);
        logger.info("onConfigurationChanged() called");
    }

    @Override
    protected void onDestroy() {
        logger.info("onDestroy() called, app is likely ending normally");
        sendNotification("App finished in onDestroy()", "Error sending app finish notification");

        // It's good practice to destroy the WebView to release resources
        if (myWebView != null) {
            //***** TODO- EXTRACT TOTO A FN *****//
            // Remove it from the view hierarchy first
            ViewGroup parent = (ViewGroup) myWebView.getParent();
            if (parent != null) {
                parent.removeView(myWebView);
            }
            myWebView.removeAllViews(); // Clear all child views     << THIS IS IMPORTANT, NEED THIS
            myWebView.destroy();        // Destroy the WebView itself
            myWebView = null;           // Nullify the reference
            //***** END OF EXTRACT *****//
        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
        logger.info("onDestroy() called, app is likely ending normally");
        sendNotification("App finished in onStop()", "Error sending app finish notification");
    }

    private void sendNotification(String notificationMessage, String logMessage) {
        // Send the notification on a new thread. Network operations, like sending HTTP requests,
        // must never be done on the main thread. Doing so could make the app unresponsive and crash.
        new Thread(() -> {
            try {
                Log4jHelper log4jHelper = new Log4jHelper();
                log4jHelper.sendNotification(notificationMessage);
            } catch (Exception e) {
                logger.error(logMessage, e);
            }
        }).start();
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

        logger.info("Free Memory: " + freeMemory + " MB, " +
            "Total Memory: " + totalMemory + " MB, " +
            "Total PSS: " + totalPss + " KB, " +
            "Private Dirty: " + privateDirty + " KB, " +
            "Heap Size: " + heapSize + " MB");
    }
}