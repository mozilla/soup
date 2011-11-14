package org.mozilla.labs.Soup.plugins;

import org.json.JSONArray;
import android.util.Log;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class MozAppsPlugin extends Plugin {
	
	private static final String TAG = "MozAppsPlugin";

	public static final String ACTION_INSTALL = "install";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.phonegap.api.Plugin#execute(java.lang.String, org.json.JSONArray, java.lang.String)
	 */
	@Override
	public PluginResult execute(String action, JSONArray data, String callback) {
		Log.d(TAG, "Called with " + action);
		
		PluginResult result = null;

		if (ACTION_INSTALL.equals(action)) {
			try {
				result = new PluginResult(Status.OK);
			} catch (Exception e) {
				Log.w(TAG, action + " failed", e);
				result = new PluginResult(Status.JSON_EXCEPTION);
			}
		} else {
			result = new PluginResult(Status.INVALID_ACTION);
		}

		return result;
	}
}
