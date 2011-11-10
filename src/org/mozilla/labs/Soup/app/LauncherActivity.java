package org.mozilla.labs.Soup.app;

import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class LauncherActivity extends SoupActivity {
	
	protected void onResolveIntent() {

        Log.i("LauncherActivity", "resolveIntent");
        
		final Intent intent = getIntent();
		final String action = intent.getAction();
        
        if (Intent.ACTION_CREATE_SHORTCUT.equals(action)) {
            Toast.makeText(LauncherActivity.this, "TODO: Prompt for app shortcut", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Init web views
        
        if (onCreateLayout()) {
        	
        	// only set URL for fresh views
        	
        	super.loadUrl("file:///android_asset/www/index.html");
        }
        
	}
}
