package org.mozilla.labs.Soup.app;

import android.content.Intent;
import android.util.Log;
import android.view.View;

public class AppActivity extends SoupActivity {
	
	private static final String TAG = "AppActivity";
	
	private String currentUri;

	protected void onResolveIntent() {
        
		final Intent intent = getIntent();
		final String action = intent.getAction();
		
		Log.d(TAG, "onResolveIntent " + intent);
        
        if (ACTION_WEBAPP.equals(action)) {
        	String uri = intent.getStringExtra("uri");
        	
        	boolean didInitialize = onCreateLayout();
        	
        	if (!didInitialize) {
        		if (uri.equals(currentUri)) {
        			Log.d(TAG, "onResolveIntent skipped " + uri);
        			return;
        		}
        		
        		// Hide old app
        		appView.setVisibility(View.INVISIBLE);
        	}
        	
        	Log.d(TAG, "onResolveIntent loading " + uri);
        	
        	currentUri = uri;
        	super.loadUrl(uri);
        }
		
	}
	
}
