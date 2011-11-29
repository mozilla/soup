package org.mozilla.labs.Soup.app;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.labs.Soup.R;
import org.mozilla.labs.Soup.service.DetachableResultReceiver;
import org.mozilla.labs.Soup.service.SyncService;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Message;
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
import android.widget.Toast;

import com.phonegap.DroidGap;

public abstract class SoupActivity extends DroidGap implements
		DetachableResultReceiver.Receiver {

	private static final String TAG = "SoupActivity";

	public static final String ACTION_WEBAPP = "org.mozilla.labs.webapp";

	static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
			Gravity.CENTER_VERTICAL);

	private SoupChromeClient appClient;
	public ProgressDialog progress;

	private View childContainer = null;
	private WebView childView = null;

	private DetachableResultReceiver receiver;
	private boolean syncStarted = false;

	private class SoupChildViewClient extends WebViewClient {

		public SoupChildViewClient() {
		}

		/**
		 * Give the host application a chance to take over the control when a new url is about to be loaded in the current
		 * WebView.
		 * 
		 * @param view
		 *          The WebView that is initiating the callback.
		 * @param url
		 *          The url to be loaded.
		 * @return true to override, false for default behavior
		 */
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Log.d(TAG + ".SoupChildViewClient", "onPageStarted:  " + url);

			if (!TextUtils.isEmpty(url) && !url.equals("about:blank")) {
				injectJavaScript(childView, false);
			}

			super.onPageStarted(view, url, favicon);
		}

		public void onReceivedSslError(WebView view, SslErrorHandler handler,
				SslError error) {
			Log.d(TAG + ".SoupChildViewClient", "onReceivedSslError");

			// TODO: Only allow paypal, though getUrl is null here!

			handler.proceed();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.phonegap.DroidGap.GapViewClient#onPageFinished(android.webkit.WebView, java.lang.String)
		 */
		@Override
		public void onPageFinished(WebView view, String url) {
			Log.d(TAG + ".SoupChildViewClient", "onPageFinished: " + url);

			if (!TextUtils.isEmpty(url) && !url.equals("about:blank")) {
				injectJavaScript(childView, false);
			}

			super.onPageFinished(view, url);
		}

	}

	/**
	 * SoupChildClient
	 * 
	 * WebChromeClient for child webkit
	 */
	private class SoupChildChromeClient extends WebChromeClient {

		public void onCloseWindow(WebView view) {
			// Closing our only dialog without checking what view is!
			closeChildView();
		}

	}

	/**
	 * SoupViewClient
	 * 
	 * WebViewClient for main webkit
	 */
	private class SoupViewClient extends GapViewClient {

		public SoupViewClient(DroidGap ctx) {
			super(ctx);
		}

		public void onReceivedSslError(WebView view, SslErrorHandler handler,
				SslError error) {
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
		 * 
		 * @see com.phonegap.DroidGap.GapViewClient#onPageStarted(android.webkit.WebView, java.lang.String,
		 * android.graphics.Bitmap)
		 */
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Log.d(TAG + ".SoupViewClient", "onPageStarted: " + url);

			if (!TextUtils.isEmpty(url) && !url.equals("about:blank")) {
				// TODO: Only inject phonegap for trusted views
				injectJavaScript(appView, true);
			}

			super.onPageStarted(view, url, favicon);

			// try {
			// progress.setTitle("Loading " + new URI(url).getAuthority());
			// } catch (URISyntaxException e) {
			//
			// }
			//
			// if (favicon != null) {
			// progress.setIcon(new BitmapDrawable(favicon));
			// } else {
			// progress.setIcon(null);
			// }
			//
			// if (!progress.isShowing()) {
			// progress.show();
			// }
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.phonegap.DroidGap.GapViewClient#onPageFinished(android.webkit.WebView, java.lang.String)
		 */
		@Override
		public void onPageFinished(WebView view, String url) {
			Log.d(TAG + ".SoupViewClient", "onPageFinished: " + url);

			if (!TextUtils.isEmpty(url) && !url.equals("about:blank")) {
				injectJavaScript(appView, true);
			}

			// Sets title, handled by application container
			SoupActivity.this.setTitle(view.getTitle());

			// progress.dismiss();

			super.onPageFinished(view, url);
		}
	}

	/**
	 * SoupGapClient
	 * 
	 * WebChromeClient for main window
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
		 * 
		 * @see android.view.View.OnClickListener#onClick(android.view.View)
		 */
		public void onClick(View v) {
			Log.d(TAG + ".SoupChromeClient", "onClick");

			closeChildView();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see com.phonegap.DroidGap.GapClient#onJsPrompt(android.webkit.WebView, java.lang.String, java.lang.String,
		 * java.lang.String, android.webkit.JsPromptResult)
		 */
		@Override
		public boolean onJsPrompt(WebView view, String url, String message,
				String defaultValue, JsPromptResult result) {

			// FIXME: This is a phonegap issue when using loadUrl to inject JS on redirecting pages
			if (url.equals("about:blank")) {
				result.cancel();
				return true;
			}

			return super.onJsPrompt(view, url, message, defaultValue, result);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.webkit.WebChromeClient#onCreateWindow(android.webkit.WebView, boolean, boolean, android.os.Message)
		 */
		@Override
		public boolean onCreateWindow(WebView view, boolean modal, boolean user,
				Message result) {

			LayoutInflater inflater = LayoutInflater.from(SoupActivity.this);

			childContainer = inflater.inflate(R.layout.popup, null);
			childView = (WebView) childContainer.findViewById(R.id.webViewPopup);
			ImageButton close = (ImageButton) childContainer
					.findViewById(R.id.subwindow_close);
			close.setOnClickListener(this);

			childView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
			childView.setMapTrackballToArrowKeys(false);
			// childView.zoomOut();

			final WebSettings settings = childView.getSettings();
			settings.setDomStorageEnabled(true);
			settings.setBuiltInZoomControls(true);
			settings.setJavaScriptEnabled(true);
			settings.setAllowFileAccess(true);

			// settings.setSupportMultipleWindows(true);
			settings.setJavaScriptCanOpenWindowsAutomatically(true);

			ViewGroup content = (ViewGroup) getWindow().getDecorView();
			content.addView(childContainer, COVER_SCREEN_PARAMS);

			childView.setWebViewClient(new SoupChildViewClient());
			childView.setWebChromeClient(new SoupChildChromeClient());

			childView.requestFocus(View.FOCUS_DOWN);
			childView.requestFocusFromTouch();

			WebView.WebViewTransport transport = (WebView.WebViewTransport) result.obj;

			transport.setWebView(childView);
			result.sendToTarget();

			return true;
		}
	}

	/**
	 * Inject JavaScript into the specified webview
	 * 
	 * @param view
	 * @param trusted
	 *          View is on a trusted domain (will install phonegap)
	 */
	private void injectJavaScript(WebView view, boolean trusted) {

		String[] files;
		if (trusted) {
			files = new String[] { "phonegap/phonegap-1.2.0.js",
					"phonegap/moz-id.js", "phonegap/moz-apps.js", "phonegap/moz-apps.js",
					"phonegap/moz-apps-mgmt.js", "soup-addon.js" };
		} else {
			files = new String[] { "soup-addon.js" };
		}

		StringBuilder builder = new StringBuilder();

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

		Log.d(TAG, "injectJavaScript: " + builder.length());

		// TODO: Log errors?
		view.loadUrl("javascript:" + builder.toString());
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		progress = new ProgressDialog(this);
		progress.setIndeterminate(true);

		// Resolve the intent (provided by child classes)
		this.onResolveIntent();
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

		super.setStringProperty("loadingDialog", "Loading App");
		// super.setStringProperty("loadingPageDialog", "Loading App");
		// super.setStringProperty("errorUrl", "file:///android_asset/www/error.html");
		// super.setIntegerProperty("splashscreen", R.drawable.splash);

		super.init();

		// Set our own extended webkit client and clientview
		appClient = new SoupChromeClient(SoupActivity.this);
		appView.setWebChromeClient(appClient);
		setWebViewClient(this.appView, new SoupViewClient(this));

		final WebSettings settings = appView.getSettings();

		// Allow window.open, bridged by onCreateWindow
		settings.setSupportMultipleWindows(true);

		return true;
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

		// If back key
		// if (keyCode == KeyEvent.KEYCODE_BACK) {
		// if (childView != null) {
		// closeChildView();
		// return false;
		// }
		// }

		return super.onKeyDown(keyCode, event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.global, menu);
		return true;
	}

	public boolean onPrepareOptionsMenu(Menu menu) {

		// TODO: Hide login/logout based on status
		menu.findItem(R.id.global_login).setVisible(false);
		menu.findItem(R.id.global_logout).setVisible(false);

		return super.onPrepareOptionsMenu(menu);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.global_settings: {
			startActivity(new Intent(this, SharedSettings.class));
			return true;
		}
		case R.id.global_login: {
			return true;
		}
		case R.id.global_logout: {
			return true;
		}
		}
		return false;
	}

	public void closeChildView() {
		if (childView == null)
			return;

		childView.destroy();

		ViewGroup content = (ViewGroup) getWindow().getDecorView();
		content.removeView(childContainer);

		childContainer = null;
		childView = null;
	}

	protected void sync() {
		if (syncStarted)
			return;

		Toast.makeText(this, "Starting sync", Toast.LENGTH_SHORT).show();

		final Intent intent = new Intent(Intent.ACTION_SYNC, null, this,
				SyncService.class);
		intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, receiver);
		startService(intent);
	}

	public void onReceiveResult(int resultCode, Bundle resultData) {
		switch (resultCode) {
		case SyncService.STATUS_RUNNING:
			// show progress
			break;
		case SyncService.STATUS_FINISHED:
			syncStarted = false;
			// List results = resultData.getParcelableList("results");
			// do something interesting
			// hide progress
			break;
		case SyncService.STATUS_ERROR:
			
			// handle the error;
			break;
		}
	}

	public static JSONArray findAll() {

		JSONArray list = new JSONArray();

		try {
			// http://www.limejs.com/roundball.webapp
			JSONObject app1 = new JSONObject();
			app1.put("origin", "http://www.limejs.com");
			app1.put("manifest_url", "http://www.limejs.com/roundball.webapp");
			app1.put(
					"manifest",
					new JSONObject(
							"{\"name\":\"Roundball\",\"description\":\"Roundball is a fun match three puzzle game where you form horizontal or vertical lines of at least three similar objects by swapping two adjacent items. The more matches you make, the higher your score. Two game modes: Classic and Timed mode. Works on regular computer or on touchscreens.\",\"launch_path\":\"/static/roundball/index.html\",\"developer\":{\"name\":\"Digital Fruit\",\"url\":\"http://www.limejs.com/\"},\"icons\":{\"128\":\"/static/roundball_icon_128.png\"},\"installs_allowed_from\":[\"*\"]}"));
			list.put(app1);

			// http://sinuousgame.com/manifest.webapp
			// JSONObject app2 = new JSONObject();
			// app2.put("origin", "http://sinuousgame.com");
			// app2.put("manifest_url", "http://sinuousgame.com/manifest.webapp");
			// app2.put(
			// "manifest",
			// new JSONObject(
			// "{\"name\":\"Sinuous\",\"description\":\"Avoid the red dots in this fun and addictive game.\",\"launch_path\":\"/\",\"developer\":{\"name\":\"Hakim El Hattab\",\"url\":\"http://hakim.se/experiments/\"},\"icons\":{\"128\":\"/assets/images/icon_128.png\"},\"installs_allowed_from\":[\"*\"]}"));
			// list.put(app2);
			//
			// // http://shazow.net
			// // http://shazow.net/linerage/gameon/manifest.json
			// JSONObject app3 = new JSONObject();
			// app3.put("origin", "http://shazow.net");
			// app3.put("manifest_url", "http://shazow.net/linerage/gameon/manifest.json");
			// app3.put(
			// "manifest",
			// new JSONObject(
			// "{\"name\":\"LineRage\",\"description\":\"You are a line. Don't hit things.\",\"launch_path\":\"/linerage/gameon/index.html\",\"developer\":{\"name\":\"Andrey Petrov\",\"url\":\"http://shazow.net\"},\"icons\":{\"16\":\"/linerage/gameon/icon_16.png\",\"32\":\"/linerage/gameon/icon_32.png\",\"128\":\"/linerage/gameon/icon_128.png\"},\"installs_allowed_from\":[\"*\"]}"));
			// list.put(app3);
			//
			// // http://stillalivejs.t4ils.com
			// // http://stillalivejs.t4ils.com/play/manifest.webapp
			// JSONObject app4 = new JSONObject();
			// app4.put("origin", "http://stillalivejs.t4ils.com");
			// app4.put("manifest_url", "http://stillalivejs.t4ils.com/play/manifest.webapp");
			// app4.put(
			// "manifest",
			// new JSONObject(
			// "{\"name\":\"StillAliveJS\",\"description\":\"StillAliveJS, or SaJS, is a puzzle game inspired by Portal: The Flash Version which is a 2D renewal of Portal, developed by Valve Corporation.\n\nSaJS consists primarily in a series of platform puzzles that must be solved by teleporting the character and other simple objects using a Portal Gun. The unusual physics allowed by this device is the emphasis of StillAliveJS.\",\"launch_path\":\"/play/index.html\",\"developer\":{\"name\":\"t4ils and Zeblackos\",\"url\":\"http://stillalivejs.t4ils.com/\"},\"icons\":{\"128\":\"/play/images/icon128.png\"},\"installs_allowed_from\":[\"*\"]}"));
			// list.put(app4);

		} catch (JSONException e) {
		}

		return list;
	}

	protected abstract void onResolveIntent();

}
