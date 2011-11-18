package org.mozilla.labs.Soup.plugins;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.labs.Soup.app.AppActivity;
import org.mozilla.labs.Soup.provider.AppsContract.Apps;

import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

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

		String[] projection = new String[] { Apps.ORIGIN, Apps.MANIFEST, Apps.INSTALL_DATA, Apps.INSTALL_ORIGIN,
				Apps.INSTALL_TIME };

		if (ACTION_LIST.equals(action)) {

			try {
				JSONArray list = Apps.syncedList(ctx);

				result = new PluginResult(Status.OK, list);
			} catch (Exception e) {
				Log.w(TAG, action + "failed", e);
				result = new PluginResult(Status.JSON_EXCEPTION);
			}

		} else if (ACTION_LAUNCH.equals(action)) {

			try {
				Cursor cur = ctx.managedQuery(Apps.CONTENT_URI, projection, Apps.ORIGIN + " = ?",
						new String[] { data.optString(0) }, Apps.DEFAULT_SORT_ORDER);

				if (cur.moveToFirst() == false) {
					Log.w(TAG, "Could not find " + data.optString(0));

					result = new PluginResult(Status.ERROR);
				} else {
					JSONObject manifest = new JSONObject(cur.getString(cur.getColumnIndex(Apps.MANIFEST)));

					String origin = cur.getString(cur.getColumnIndex(Apps.ORIGIN));
					String uri = origin + manifest.optString("launch_path");
					final String name = manifest.optString("name");

					final Intent shortcutIntent = new Intent(this.ctx, AppActivity.class);
					shortcutIntent.setAction(AppActivity.ACTION_WEBAPP);
					shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					shortcutIntent.putExtra("uri", uri);

					ctx.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(ctx, "Launching " + name, Toast.LENGTH_SHORT).show();

							ctx.startActivity(shortcutIntent);
						}
					});

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
