package org.mozilla.labs;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.webkit.WebView;

public class SoupWeb extends Activity {
	public static final String ACTION_WEBAPP = "org.mozilla.labs.WEBAPP";
	
	private void handleIntent(Intent i) {
		Log.i("onNewIntent", "Got intent! " + i);
		if (i == null) return;
		
        if (ACTION_WEBAPP.equals(i.getAction())) {
            String uri = i.getStringExtra("uri");
            Log.i("onNewIntent", "Got URI " + uri);
            
            WebView webview = new SoupWebView(this);
            webview.loadUrl(uri);
            setContentView(webview);
        } else {
        	Log.e("handleIntent", "Got incorrect intent!");
        }
	}
	
	@Override
	public void onResume() {
		super.onResume();
		handleIntent(getIntent());
	}
}
