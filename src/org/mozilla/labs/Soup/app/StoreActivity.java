package org.mozilla.labs.Soup.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class StoreActivity extends SoupActivity {
	
	private static final String TAG = "StoreActivity";
	
	protected void onResolveIntent() {

        Log.d(TAG, "onResolveIntent");
        
		final Intent intent = getIntent();
        
//        if (Intent.ACTION_VIEW.equals(action)) {
//            Toast.makeText(StoreActivity.this, "TODO: Open app store detail view", Toast.LENGTH_SHORT).show();
//        }
        
        // Init web views
        
        if (onCreateLayout()) {
        	// Allow overriding the Store with an new landing page
        	String uri = intent.getStringExtra("uri");
        	
        	if (TextUtils.isEmpty(uri)) {
        		SharedPreferences settings = getSharedPreferences(SharedSettings.PREFS_NAME, 0);
        		
        	    uri = settings.getString("dev_store", "https://apps.mozillalabs.com/appdir/");
        	}
        	
        	// only set URL for fresh views
        	super.loadUrl(uri);
        }
        
	}
}
