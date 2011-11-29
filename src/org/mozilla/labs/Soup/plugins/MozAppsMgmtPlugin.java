package org.mozilla.labs.Soup.plugins;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.labs.Soup.app.AppActivity;
import org.mozilla.labs.Soup.provider.AppsContract.Apps;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
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

		if (ACTION_LIST.equals(action)) {

			try {
				result = list();
				
			} catch (Exception e) {
				Log.w(TAG, action + " failed", e);
				result = new PluginResult(Status.JSON_EXCEPTION);
			}

		} else if (ACTION_LAUNCH.equals(action)) {

			try {
				result = launch(data.optString(0));

			} catch (Exception e) {
				Log.w(TAG, action + " failed", e);
				result = new PluginResult(Status.JSON_EXCEPTION);
			}
		} else {
			result = new PluginResult(Status.INVALID_ACTION);
		}

		Log.d(TAG, "Returns " + result.getJSONString());

		return result;
	}

	private PluginResult launch(String query) {

		String[] projection = new String[] { Apps.ORIGIN, Apps.MANIFEST,
				Apps.INSTALL_DATA, Apps.INSTALL_ORIGIN, Apps.INSTALL_TIME };
		
		Cursor cur = ctx.managedQuery(Apps.CONTENT_URI, projection, Apps.ORIGIN
				+ " = ?", new String[] { query },
				Apps.DEFAULT_SORT_ORDER);

		if (cur.moveToFirst() == false) {
			Log.w(TAG, "Could not find " + query);

			return new PluginResult(Status.ERROR);
		}
			
		JSONObject manifest;
		try {
			manifest = new JSONObject(cur.getString(cur
					.getColumnIndex(Apps.MANIFEST)));
		} catch (JSONException e) {
			return new PluginResult(Status.ERROR);
		}

		String origin = cur.getString(cur.getColumnIndex(Apps.ORIGIN));
		String uri = origin + manifest.optString("launch_path");
		final String name = manifest.optString("name");

		final Intent shortcutIntent = new Intent(this.ctx, AppActivity.class);
		shortcutIntent.setAction(AppActivity.ACTION_WEBAPP);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		shortcutIntent.putExtra("uri", uri);

		ctx.runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(ctx, "Launching " + name, Toast.LENGTH_SHORT)
						.show();

				ctx.startActivity(shortcutIntent);
			}
		});

		return new PluginResult(Status.OK);
	}

	private PluginResult list() {
		// TODO: Wait for sync
		
		Cursor cur = ctx.managedQuery(Apps.CONTENT_URI, Apps.APP_PROJECTION, null,
				null, Apps.DEFAULT_SORT_ORDER);

		cur.moveToFirst();

		JSONArray list = new JSONArray();

		while (cur.isAfterLast() == false) {
			JSONObject app = Apps.toJSONObject(cur);

			if (app != null) {
				list.put(app);
			}

			cur.moveToNext();
		}

		return new PluginResult(Status.OK, list);
	}
	
	/**
	 * ContentObserver for Apps
	 */
  public class AppsObserver extends ContentObserver {
  	 
    public AppsObserver(Handler handler) {
        super(handler);
    }

    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        // Send the on change message to your database manager.
        watch();
    }
}
	
	private void watch() {
		AppsObserver observer = new AppsObserver(new Handler());
		 
		ContentResolver resolver = ctx.getContentResolver();
		resolver.registerContentObserver(Apps.CONTENT_URI, true, observer);
	}
}
