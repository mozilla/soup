package org.mozilla.labs.Soup.app;

import android.content.Intent;
import android.util.Log;
import android.view.View;

public class AppActivity extends SoupActivity {

	private String currentUri;

	protected void onResolveIntent() {
        
		final Intent intent = getIntent();
		final String action = intent.getAction();
		
		Log.i("AppActivity", "onResolveIntent " + intent);
        
        if (ACTION_WEBAPP.equals(action)) {
        	String uri = intent.getStringExtra("uri");
        	
        	boolean didInitialize = onCreateLayout();
        	
        	if (!didInitialize) {
        		if (uri.equals(currentUri)) {
        			Log.i("AppActivity", "onResolveIntent skipped " + uri);
        			return;
        		}
        		
        		// Hide old app
        		appView.setVisibility(View.INVISIBLE);
        	}
        	
        	Log.i("AppActivity", "onResolveIntent loading " + uri);
        	
        	currentUri = uri;
        	super.loadUrl(uri);
        }
		
	}
	
}
