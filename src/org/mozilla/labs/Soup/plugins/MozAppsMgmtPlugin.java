package org.mozilla.labs.Soup.plugins;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.labs.Soup.app.AppActivity;
import org.mozilla.labs.Soup.app.SoupActivity;

import android.content.Intent;
import android.util.Log;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class MozAppsMgmtPlugin extends Plugin {
	
	private static final String TAG = "MozAppsMgmtPlugin";

	public static final String ACTION_LIST = "list";

	public static final String ACTION_LAUNCH = "launch";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.phonegap.api.Plugin#execute(java.lang.String, org.json.JSONArray, java.lang.String)
	 */
	@Override
	public PluginResult execute(String action, JSONArray data, String callback) {
		Log.d(TAG, "Called with " + action);
		
		PluginResult result = null;

		SoupActivity soup = (SoupActivity) this.ctx;

		if (ACTION_LIST.equals(action)) {

			try {
				JSONArray list = soup.findAll();

				Log.d(TAG, "List: " + list.length());

				result = new PluginResult(Status.OK, list);
			} catch (Exception e) {
				Log.d("MozAppsMgmtPlugin Exception", e.getMessage() + " " + e.toString());
				result = new PluginResult(Status.JSON_EXCEPTION);
			}

		} else if (ACTION_LAUNCH.equals(action)) {

			try {
				JSONObject entry = soup.findOneByOrigin(data.optString(0));

				if (entry == null) {
					result = new PluginResult(Status.ERROR);
				} else {

					Intent shortcutIntent = new Intent(this.ctx, AppActivity.class);
					shortcutIntent.setAction(AppActivity.ACTION_WEBAPP);
					shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

					JSONObject manifest = entry.optJSONObject("manifest");
					String origin = entry.optString("origin");
					String uri = origin + manifest.optString("launch_path");

					shortcutIntent.putExtra("uri", uri);

					Log.d(TAG, "Launching " + uri);

					// Instant start
					this.ctx.startActivity(shortcutIntent);

					result = new PluginResult(Status.OK);
				}

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
