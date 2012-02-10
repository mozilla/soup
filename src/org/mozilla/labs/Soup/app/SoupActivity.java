
package org.mozilla.labs.Soup.app;

import java.io.IOException;
import java.io.InputStream;

import org.mozilla.labs.Soup.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.JsPromptResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.phonegap.DroidGap;

public abstract class SoupActivity extends DroidGap {

    private static final String TAG = "SoupActivity";

    public static final String ACTION_WEBAPP = "org.mozilla.labs.webapp";

    static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
            Gravity.CENTER_VERTICAL);

    private SoupChromeClient appClient;

    private View childRoot = null;

    private WebView childView = null;

    private View titleView = null;

    public ProgressDialog progressDialog = null;

    private class SoupChildViewClient extends WebViewClient {

        private boolean gotHidden = false;

        public SoupChildViewClient() {
        }

        @Override
        public void onLoadResource(WebView view, String url) {

            Uri uri = Uri.parse(url);

            if (!gotHidden && uri != null) {

                final SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(SoupActivity.this);
                final Uri identityUri = Uri.parse(prefs.getString("dev_identity", ""));
                final String email = prefs.getString("email", null);

                Log.d(TAG, uri.getAuthority() + " | " + identityUri.getAuthority() + ", " + email);

                // FIXME: Review this code after proper "native" BrowserID
                // integration
                if (uri.getAuthority().equals(identityUri.getAuthority())
                        && !TextUtils.isEmpty(email)) {

                    titleView.setVisibility(View.GONE);
                    childView.setVisibility(View.GONE);

                    appView.setVisibility(View.VISIBLE);

                    progressDialog = ProgressDialog.show(SoupActivity.this, null,
                            "Verifying user …",
                            true, false);

                    gotHidden = true;
                }

            }

            super.onLoadResource(view, url);
        }

        /**
         * Give the host application a chance to take over the control when a
         * new url is about to be loaded in the current WebView.
         * 
         * @param view The WebView that is initiating the callback.
         * @param url The url to be loaded.
         * @return true to override, false for default behavior
         */
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG + ".SoupChildViewClient", "onPageStarted:  " + url);

            if (!TextUtils.isEmpty(url) && !url.equals("about:blank")) {
                // injectJavaScript(childView, false);
            }

            Uri uri = Uri.parse(url);
            if (uri != null) {
                SoupActivity.this.setTitle("Loading " + uri.getHost());
            }

            ProgressBar progress = (ProgressBar)childRoot.findViewById(R.id.title_progress_bar);
            progress.setVisibility(View.VISIBLE);

            super.onPageStarted(view, url, favicon);
        }

        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Log.d(TAG + ".SoupChildViewClient", "onReceivedSslError");

            // TODO: Only allow paypal, though getUrl is null here!

            handler.proceed();
        }

        /*
         * (non-Javadoc)
         * @see
         * com.phonegap.DroidGap.GapViewClient#onPageFinished(android.webkit
         * .WebView, java.lang.String)
         */
        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG + ".SoupChildViewClient", "onPageFinished: " + url);

            if (!TextUtils.isEmpty(url) && !url.equals("about:blank")) {
                injectJavaScript(childView, false);
            } else {
                Log.d(TAG, "Skipped injectJavaScript");
            }

            ProgressBar progress = (ProgressBar)childRoot.findViewById(R.id.title_progress_bar);
            progress.setVisibility(View.GONE);

            super.onPageFinished(view, url);
        }

    }

    /**
     * SoupChildClient WebChromeClient for child webkit
     */
    private class SoupChildChromeClient extends WebChromeClient {

        public void onCloseWindow(WebView view) {
            // Closing our only dialog without checking what view is!
            closeChildView();
        }

        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);

            // Sets title, handled by application container
            SoupActivity.this.setTitle(title);
        }

        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);

            // Sets title, handled by application container
            ImageView image = (ImageView)childRoot.findViewById(R.id.title_image);
            image.setImageBitmap(icon);
        }

        /*
         * (non-Javadoc)
         * @see
         * android.webkit.WebChromeClient#onCreateWindow(android.webkit.WebView,
         * boolean, boolean, android.os.Message)
         */
        @Override
        public boolean onCreateWindow(WebView view, boolean modal, boolean user, Message result) {
            Log.w(TAG + ".SoupChildChromeClient", "onCreateWindow");

            WebView.WebViewTransport transport = (WebView.WebViewTransport)result.obj;

            closeChildView();

            transport.setWebView(appView);
            result.sendToTarget();

            return true;
        }

    }

    /**
     * SoupViewClient WebViewClient for main webkit
     */
    private class SoupViewClient extends GapViewClient {

        public SoupViewClient(DroidGap ctx) {
            super(ctx);
        }

        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            Log.d(TAG + ".SoupViewClient", "onReceivedSslError");

            // URI uri = null;
            // try {
            // uri = new URI(view.getUrl());
            // } catch (URISyntaxException e) {
            // }
            //
            // if (uri != null && uri.getHost().endsWith(".paypal.com")) {
            // handler.proceed();
            // }

            handler.proceed();
        }

        /*
         * (non-Javadoc)
         * @see
         * com.phonegap.DroidGap.GapViewClient#onPageStarted(android.webkit.
         * WebView, java.lang.String, android.graphics.Bitmap)
         */
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Log.d(TAG + ".SoupViewClient", "onPageStarted: " + url);

            if (!TextUtils.isEmpty(url) && !url.equals("about:blank")) {
                // TODO: Only inject phonegap for trusted views
                injectJavaScript(appView, true);
            }

            Uri uri = Uri.parse(url);
            if (uri != null) {
                SoupActivity.this.setTitle("Loading " + uri.getHost());
            }

            titleView.setVisibility(View.VISIBLE);

            ProgressBar progress = (ProgressBar)root.findViewById(R.id.title_progress_bar);
            progress.setVisibility(View.VISIBLE);

            super.onPageStarted(view, url, favicon);
        }

        /*
         * (non-Javadoc)
         * @see
         * com.phonegap.DroidGap.GapViewClient#onPageFinished(android.webkit
         * .WebView, java.lang.String)
         */
        @Override
        public void onPageFinished(WebView view, String url) {
            Log.d(TAG + ".SoupViewClient", "onPageFinished: " + url);

            if (!TextUtils.isEmpty(url) && !url.equals("about:blank")) {
                injectJavaScript(appView, true);
            }

            titleView.setVisibility(View.GONE);

            super.onPageFinished(view, url);
        }
    }

    /**
     * SoupGapClient WebChromeClient for main window
     */
    private class SoupChromeClient extends GapClient implements OnClickListener {

        /**
         * @param context
         */
        public SoupChromeClient(Context context) {
            super(context);
        }

        /*
         * (non-Javadoc)
         * @see android.view.View.OnClickListener#onClick(android.view.View)
         */
        public void onClick(View v) {
            Log.d(TAG + ".SoupChromeClient", "onClick");

            closeChildView();
        }

        /*
         * (non-Javadoc)
         * @see
         * com.phonegap.DroidGap.GapClient#onJsPrompt(android.webkit.WebView,
         * java.lang.String, java.lang.String, java.lang.String,
         * android.webkit.JsPromptResult)
         */
        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
                JsPromptResult result) {

            // FIXME: This is a phonegap issue when using loadUrl to inject JS
            // on redirecting pages
            if (url == null || url.equals("about:blank")) {
                result.cancel();
                return true;
            }

            return super.onJsPrompt(view, url, message, defaultValue, result);
        }

        /*
         * (non-Javadoc)
         * @see
         * android.webkit.WebChromeClient#onCreateWindow(android.webkit.WebView,
         * boolean, boolean, android.os.Message)
         */
        @Override
        public boolean onCreateWindow(WebView view, boolean modal, boolean user, Message result) {

            titleView.setVisibility(View.GONE);

            LayoutInflater inflater = LayoutInflater.from(SoupActivity.this);

            childRoot = inflater.inflate(R.layout.popup, null);
            childView = (WebView)childRoot.findViewById(R.id.webViewPopup);

            ImageButton close = (ImageButton)childRoot.findViewById(R.id.title_close);
            close.setVisibility(View.VISIBLE);
            close.setOnClickListener(this);

            childView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
            childView.setMapTrackballToArrowKeys(false);
            // childView.zoomOut();

            final WebSettings settings = childView.getSettings();
            settings.setDomStorageEnabled(true);
            settings.setBuiltInZoomControls(true);
            settings.setJavaScriptEnabled(true);
            settings.setAllowFileAccess(true);

            settings.setSupportMultipleWindows(true);
            settings.setJavaScriptCanOpenWindowsAutomatically(true);

            // ViewGroup content = (ViewGroup) getWindow().getDecorView();
            appView.setVisibility(View.GONE);
            titleView.setVisibility(View.GONE);

            root.addView(childRoot);

            childView.setWebViewClient(new SoupChildViewClient());
            childView.setWebChromeClient(new SoupChildChromeClient());

            childView.requestFocus(View.FOCUS_DOWN);
            childView.requestFocusFromTouch();

            WebView.WebViewTransport transport = (WebView.WebViewTransport)result.obj;

            transport.setWebView(childView);
            result.sendToTarget();

            return true;
        }

        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);

            // Sets title, handled by application container
            SoupActivity.this.setTitle(title);
        }

        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);

            // Sets title, handled by application container
            ImageView image = (ImageView)root.findViewById(R.id.title_image);
            image.setImageBitmap(icon);
        }
    }

    /**
     * Inject JavaScript into the specified webview
     * 
     * @param view
     * @param trusted View is on a trusted domain (will install phonegap)
     */
    private void injectJavaScript(WebView view, boolean trusted) {

        String[] files;
        if (trusted) {
            files = new String[] {
                    "phonegap/phonegap-1.2.0.js", "phonegap/moz-id.js", "phonegap/moz-apps.js",
                    "phonegap/moz-apps.js", "phonegap/moz-apps-mgmt.js", "soup-addon.js"
            };
        } else {
            files = new String[] {
                "soup-addon.js"
            };
        }

        StringBuilder builder = new StringBuilder();

        String iwashere = "$soup_was_here$";
        builder.append(String.format(
                "javascript:try { if (typeof %s != 'undefined') throw ''; %s = {}; ", iwashere,
                iwashere));

        for (String name : files) {
            try {
                InputStream is = getAssets().open("www/js/" + name);
                int size = is.available();
                byte[] buffer = new byte[size];
                is.read(buffer);
                is.close();

                builder.append(new String(buffer));
                builder.append("\n");
            } catch (IOException e) {
                Log.e(TAG, "injectJavaScript skipped " + name, e);
            }
        }

        builder.append("; } catch(e) { if (e.message) console.error(e); }");

        Log.d(TAG, "injectJavaScript: " + builder.length());

        view.loadUrl(builder.toString());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ((SoupApplication)getApplication()).registerActivity(this);

        // Resolve the intent (provided by child classes)
        this.onResolveIntent();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ((SoupApplication)getApplication()).unregisterActivity(this);
    }

    /**
     * Called when the activity receives a new intent
     **/
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.equals(getIntent())) {
            // Log.d(TAG, "onNewIntent equals current Intent: " + intent);
            return;
        }

        // Log.d(TAG, "onNewIntent called with new Intent " + intent);

        setIntent(intent);

        this.onResolveIntent();
    }

    /**
     * Init phonegap and create layout
     * 
     * @return true if the layout was freshly created
     */
    public boolean onCreateLayout() {
        if (appView != null) {
            Log.d(TAG, "onCreateLayout skipped");
            return false;
        }

        super.setBooleanProperty("keepRunning", true);
        super.setIntegerProperty("loadUrlTimeoutValue", 60000);

        init();

        // Set our own extended webkit client and clientview
        appClient = new SoupChromeClient(SoupActivity.this);
        appView.setWebChromeClient(appClient);
        setWebViewClient(this.appView, new SoupViewClient(this));

        final WebSettings settings = appView.getSettings();

        // Allow window.open, bridged by onCreateWindow
        settings.setSupportMultipleWindows(true);

        return true;
    }

    public void clearCache() {
        super.clearCache();

        appView.clearCache(false);
        appView.clearCache(true);

        appView.clearFormData();
        appView.clearHistory();
        appView.clearMatches();

        appView.freeMemory();
    }

    /*
     * (non-Javadoc)
     * @see com.phonegap.DroidGap#init()
     */
    public void init() {
        super.init();

        appView.setVisibility(View.VISIBLE);

        LayoutInflater inflater = LayoutInflater.from(SoupActivity.this);

        titleView = inflater.inflate(R.layout.title, null);

        root.removeView(appView);
        root.addView(titleView);
        root.addView(appView);
    }

    /*
     * Custom setTitle to handle custom title bar, including for popups
     * @see android.app.Activity#setTitle(java.lang.CharSequence)
     */
    public void setTitle(CharSequence title) {
        super.setTitle(title);

        View parent = root;
        if (childRoot != null) {
            parent = childRoot;
        }

        TextView text = (TextView)parent.findViewById(android.R.id.title);
        text.setText(title);
    }

    /**
     * Called when a key is pressed.
     * 
     * @param keyCode
     * @param event
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Don't let phonegap override our keys
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            return false;
        }

        if (keyCode == KeyEvent.KEYCODE_SEARCH) {
            return false;
        }

        // If back key is pushed during popup time, we just close it
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (childView != null) {
                closeChildView();
                return false;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.global, menu);
        return true;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    public boolean onPrepareOptionsMenu(Menu menu) {

        menu.findItem(R.id.global_launcher).setVisible(!(this instanceof LauncherActivity));
        menu.findItem(R.id.global_store).setVisible(!(this instanceof StoreActivity));

        menu.findItem(R.id.global_refresh).setVisible(childView == null);

        return super.onPrepareOptionsMenu(menu);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.global_settings: {
                startActivity(new Intent(this, SharedSettings.class));
                return true;
            }
            case R.id.global_refresh: {
                appView.stopLoading();
                appView.reload();

                return true;
            }
            case R.id.global_login: {
                return true;
            }
            case R.id.global_store: {
                startActivity(new Intent(this, StoreActivity.class));
                return true;
            }
            case R.id.global_launcher: {
                startActivity(new Intent(this, LauncherActivity.class));
                return true;
            }
            case R.id.global_logout: {

                AlertDialog.Builder dlg = new AlertDialog.Builder(this);

                dlg.setTitle("Confirm signout")
                        .setMessage("Are you sure you want to clear all personal data?")
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        ((SoupApplication)SoupActivity.this.getApplication())
                                                .clearData(SoupActivity.this);
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        Toast.makeText(SoupActivity.this, "Logout cancelled",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                ;

                dlg.create();
                dlg.show();

                return true;
            }
        }
        return false;
    }

    /**
     * Close and destroy popup window
     */
    public void closeChildView() {
        if (childView == null)
            return;

        childView.destroy();

        root.removeView(childRoot);

        if (progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        titleView.setVisibility(View.VISIBLE);

        appView.setVisibility(View.VISIBLE);

        // TODO: Debug why it shows up after popup closes
        titleView.setVisibility(View.GONE);

        appView.requestFocus(View.FOCUS_DOWN);
        appView.requestFocusFromTouch();

        childRoot = null;
        childView = null;
    }

    /**
     * Intent for all webkit-enabled Soup activities
     */
    protected abstract void onResolveIntent();

}
