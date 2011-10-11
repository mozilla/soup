package org.mozilla.labs.Soup;

import com.phonegap.*;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class SoupActivity extends DroidGap {
	public static final String ACTION_WEBAPP = "org.mozilla.labs.webapp";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Intent i = getIntent();
        Log.i("SoupActivity", "onCreate called with Intent " + i);
        if (ACTION_WEBAPP.equals(i.getAction())) {
        	String uri = i.getStringExtra("uri");
        	super.loadUrl(uri);
        } else {
        	super.loadUrl("file:///android_asset/www/index.html");
        }
    }
    
    @Override
    public void onPause() {
    	// Whoah.
    	super.onPause();
    	finish();
    }
}
