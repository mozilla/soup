package org.mozilla.labs.Soup.app;

import org.mozilla.labs.Soup.R;

import android.app.Application;
import android.preference.PreferenceManager;
import android.util.Log;

public class SoupApplication extends Application {

    private static final String TAG = "SoupApplication";

	@Override
    public void onCreate() {
    	Log.d(TAG, "onCreate");
    	
        /*
         * This populates the default values from the preferences XML file.
         * 
         * TODO: Actually set defaults in preferences.xml
         */
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    @Override
    public void onTerminate() {
    }
	
}
