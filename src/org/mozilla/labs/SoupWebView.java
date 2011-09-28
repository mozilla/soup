package org.mozilla.labs;

import android.content.Context;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SoupWebView extends WebView {
	private class SoupWebViewClient extends WebViewClient {
		@Override
    	public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Log.i("WebView", "Going to load " + url);
			view.loadUrl(url);
    		return false;
    	}
	}
	
	public SoupWebView(Context context) {
		super(context);
		this.getSettings().setJavaScriptEnabled(true);
		this.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        this.setWebViewClient(new SoupWebViewClient());
	}
}
