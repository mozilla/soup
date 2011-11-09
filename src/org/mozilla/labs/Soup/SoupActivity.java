package org.mozilla.labs.Soup;

import java.io.IOException;
import java.io.InputStream;

import com.phonegap.*;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

public class SoupActivity extends DroidGap {
	public static final String ACTION_WEBAPP = "org.mozilla.labs.webapp";
	static final FrameLayout.LayoutParams COVER_SCREEN_PARAMS = new FrameLayout.LayoutParams(
	    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_VERTICAL
	);
	
	private WebView childView;
	
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
			super.onPageFinished(view, url);
			
			injectJavaScript(appView);
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
			super.onPageFinished(view, url);
			
			injectJavaScript(appView);
		}
	}
	
	private class SoupGapClient extends GapClient implements OnClickListener {

		private View container;
		
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
	        
	        // settings.setSupportMultipleWindows(true);
	        settings.setJavaScriptCanOpenWindowsAutomatically(true);
			
			attachChildWindow(container);
			
			SoupChildViewClient client = new SoupChildViewClient();
			childView.setWebViewClient(client);
			
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
		// tried: "javascript:if(!window.$soup) (function(){ var s = document.createElement('script'); s.src = 'file:///android_asset/www/js/soup-addon.js'; document.getElementsByTagName('head')[0].appendChild(s); })();"
		
		String strContent;
		try {
			InputStream is = getAssets().open("www/js/soup-addon.js");
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
		
		Log.i("SoupActivity", "injectJavaScript " + strContent.length());
		
		view.loadUrl("javascript:" + strContent);
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        super.init();
        
        appView.setWebChromeClient(new SoupGapClient(SoupActivity.this));
        setWebViewClient(this.appView, new SoupViewClient(this));
        
        // Allow window.open, bridged by onCreateWindow
        appView.getSettings().setSupportMultipleWindows(true);
        
        Intent i = getIntent();
        Log.i("SoupActivity", "onCreate called with Intent " + i);
        if (ACTION_WEBAPP.equals(i.getAction())) {
        	String uri = i.getStringExtra("uri");
        	super.loadUrl(uri);
        } else {
        	super.loadUrl("file:///android_asset/www/index.html");
        }
    }
    
    /**
     * Called when the activity receives a new intent
     **/
    @Override
    protected void onNewIntent(Intent intent) {
    	Log.i("SoupActivity", "onNewIntent called with Intent " + intent);
    	
        super.onNewIntent(intent);
    }
    
    /* (non-Javadoc)
     * @see com.phonegap.DroidGap#onPause()
     */
    @Override
    public void onPause() {
    	super.onPause();
    	// Whoah.
    	// finish();
    }
}
