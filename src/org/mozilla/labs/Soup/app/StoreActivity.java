package org.mozilla.labs.Soup.app;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class StoreActivity extends SoupActivity {
	
	private static final String TAG = "StoreActivity";
	
	protected void onResolveIntent() {

        Log.d(TAG, "onResolveIntent");
        
		final Intent intent = getIntent();
		final String action = intent.getAction();
        
        if (Intent.ACTION_VIEW.equals(action)) {
            Toast.makeText(StoreActivity.this, "TODO: Open app store detail view", Toast.LENGTH_SHORT).show();
        }
        
        // Init web views
        
        if (onCreateLayout()) {
        	
        	// only set URL for fresh views
        	
        	super.loadUrl("https://apps-preview-dev.allizom.org/en-US/apps/");
        }
        
	}
}
