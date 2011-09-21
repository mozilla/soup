package org.mozilla.labs;

import android.content.Context;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class SoupWebView extends WebView {
	public SoupWebView(Context context) {
		super(context);
		this.getSettings().setJavaScriptEnabled(true);
        this.setWebViewClient(new WebViewClient() {
        	@Override
        	public boolean shouldOverrideUrlLoading(WebView view, String url) {
        		return false;
        	}
        });
	}
}
