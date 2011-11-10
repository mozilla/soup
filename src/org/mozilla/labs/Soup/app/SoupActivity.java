package org.mozilla.labs.Soup.app;

import java.io.IOException;
import java.io.InputStream;

import org.mozilla.labs.Soup.R;

import com.phonegap.*;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;

public abstract class SoupActivity extends DroidGap {
	public static final String ACTION_WEBAPP = "org.mozilla.labs.webapp";
	static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
	    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_VERTICAL
	);
	
	private SoupGapClient appClient;
	
	private class SoupChildViewClient extends WebViewClient {

		public SoupChildViewClient() {
		}
		
		/**
         * Give the host application a chance to take over the control when a new url 
         * is about to be loaded in the current WebView.
         * 
         * @param view          The WebView that is initiating the callback.
         * @param url           The url to be loaded.
         * @return              true to override, false for default behavior
         */
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
        	Log.i("SoupChildViewClient", "Going to load " + url);
        	view.loadUrl(url);
        	return true;
        }
		
		/* (non-Javadoc)
		 * @see android.webkit.WebViewClient#onLoadResource(android.webkit.WebView, java.lang.String)
		 */
		@Override
		public void onLoadResource(WebView view, String url) {
			Log.i("SoupChildViewClient", "onLoadResource " + url);
		}
		
		@Override
		public void onReceivedError(WebView view, int err, String desc, String url) {
			Log.i("SoupChildViewClient", "onReceivedError " + url + ": " + desc);
		}
		
		/* (non-Javadoc)
		 * @see android.webkit.WebViewClient#onPageFinished(android.webkit.WebView, java.lang.String)
		 */
		@Override
		public void onPageFinished(WebView view, String url) {
			Log.i("SoupChildViewClient", "onPageFinished " + url);
			injectJavaScript(appView);
			
			super.onPageFinished(view, url);
		}
		
	}

	private class SoupChildClient extends WebChromeClient {
		
		public void onCloseWindow(WebView view) {
			// Closing our only dialog without checking what view is!
			appClient.onClick(view);
		}
		
	}
	
	private class SoupViewClient extends GapViewClient {

		public SoupViewClient(DroidGap ctx) {
			super(ctx);
        }
		
		/* (non-Javadoc)
		 * @see com.phonegap.DroidGap.GapViewClient#onPageFinished(android.webkit.WebView, java.lang.String)
		 */
		@Override
		public void onPageFinished(WebView view, String url) {
			Log.i("SoupViewClient", "onPageFinished " + url);
			injectJavaScript(appView);
			
			super.onPageFinished(view, url);
		}
	}
	
	private class SoupGapClient extends GapClient implements OnClickListener {

		private View container;
		private WebView childView;
		
		/**
		 * @param context
		 */
		public SoupGapClient(Context context) {
			super(context);
		}
		
		
		/* (non-Javadoc)
		 * @see android.view.View.OnClickListener#onClick(android.view.View)
		 */
		public void onClick(View v) {
			Log.i("onClick", "Close clicked, removing Child");
			
			childView.destroy();
			removeChildWindow(container);
		}
		
		@Override
		public void onCloseWindow(WebView view) {
			// Closing our only dialog without checking what view is!
			onClick(view);
		}
		
		@Override
		public boolean onCreateWindow(WebView view, boolean modal, boolean user, Message result) {
			// TODO Launch on UI thread
			createChildWindow();
			
			WebView.WebViewTransport transport = (WebView.WebViewTransport) result.obj;
			
			transport.setWebView(childView);
			result.sendToTarget();
			
			return true;
		}
		
		private void createChildWindow() {
			LayoutInflater inflater = LayoutInflater.from(SoupActivity.this);
			
			container = inflater.inflate(R.layout.popup, null);
			childView = (WebView) container.findViewById(R.id.webViewPopup);
			ImageButton close = (ImageButton) container.findViewById(R.id.subwindow_close);
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
			
			attachChildWindow(container);
			
			childView.setWebViewClient(new SoupChildViewClient());
			childView.setWebChromeClient(new SoupChildClient());
			
			childView.requestFocus(View.FOCUS_DOWN);
			childView.requestFocusFromTouch();
		}
	}
	
	/**
	 * @param view
	 */
	public void attachChildWindow(View view) {
		ViewGroup content = (ViewGroup)getWindow().getDecorView();
		content.addView(view, COVER_SCREEN_PARAMS);
	}
	
	/**
	 * @param view
	 */
	public void removeChildWindow(View view) {
		ViewGroup content = (ViewGroup)getWindow().getDecorView();
		content.removeView(view);
	}
	
	private void injectJavaScript(WebView view) {
		injectSingleFile(view, "phonegap-1.2.0.js", "");
		injectSingleFile(view, "soup-addon.js", "");
	}
	
	private void injectSingleFile(WebView view, String file, String prepend) {
		String strContent;
		
		try {
			InputStream is = getAssets().open("www/js/" + file);
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			// Read the entire asset into a local byte buffer.
			// Convert the buffer into a string.
			strContent = new String(buffer);
		} catch (IOException e) {
			return;
		}
		
		view.loadUrl("javascript:" + prepend + strContent);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.i("SoupActivity", "onCreate called with Intent " + getIntent().getAction());

        // Resolve the intent

        this.onResolveIntent();
    }
    
    /**
     * Called when the activity receives a new intent
     **/
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        if (intent.equals(getIntent())) {
            Log.i("SoupActivity", "onNewIntent equals current Intent: " + intent);
        	return;
        }

        Log.i("SoupActivity", "onNewIntent called with new Intent " + intent);
        
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
    		Log.i("SoupActivity", "init skipped");
    		return false;
    	}
    	
    	super.setStringProperty("loadingDialog", "Loading App");
    	super.setStringProperty("errorUrl", "file:///android_asset/www/error.html");
    	// super.setIntegerProperty("splashscreen", R.drawable.splash);
    	
    	super.init();

        // Set our own extended webkit client and clientview
    	appClient = new SoupGapClient(SoupActivity.this);
        appView.setWebChromeClient(appClient);
        setWebViewClient(this.appView, new SoupViewClient(this));
        
        final WebSettings settings = appView.getSettings();
        settings.setAllowFileAccess(true);
        
        // Allow window.open, bridged by onCreateWindow
        settings.setSupportMultipleWindows(true);
        
        return true;
    }

	protected abstract void onResolveIntent();

}
