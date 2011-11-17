package org.mozilla.labs.Soup.plugins;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.labs.Soup.app.AppActivity;
import org.mozilla.labs.Soup.app.SoupActivity;
import org.mozilla.labs.Soup.http.ImageFactory;
import org.mozilla.labs.Soup.provider.AppsContract.Apps;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import android.widget.Toast;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class MozAppsMgmtPlugin extends Plugin {

	private static final String TAG = "MozAppsMgmtPlugin";

	public static final String ACTION_LIST = "list";

	public static final String ACTION_LAUNCH = "launch";

	private static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";

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
				Cursor cur = ctx.managedQuery(Apps.CONTENT_URI, projection, null, null, Apps.DEFAULT_SORT_ORDER);

				cur.moveToFirst();
				int index = cur.getColumnIndex(Apps.NAME);
				Log.d(TAG, "Iterating " + index);

				JSONArray list = new JSONArray();

				while (cur.isAfterLast() == false) {
					JSONObject app = Apps.toJSONObject(cur);

					if (app != null) {
						list.put(app);
					}

					cur.moveToNext();
				}

				result = new PluginResult(Status.OK, list);
			} catch (Exception e) {
				Log.d("MozAppsMgmtPlugin Exception", e.getMessage() + " " + e.toString());
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
					final String icon = origin + manifest.optJSONObject("icons").optString("128");

					final Intent shortcutIntent = new Intent(this.ctx, AppActivity.class);
					shortcutIntent.setAction(AppActivity.ACTION_WEBAPP);
					shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					shortcutIntent.putExtra("uri", uri);

					ctx.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(ctx, "Launching " + name, Toast.LENGTH_SHORT).show();

							Bitmap bitmap = ImageFactory.getResizedImage(icon, 72, 72);

							Intent intent = new Intent();
							intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
							intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
							intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
							if (bitmap != null) {
								intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
							}
							// Disallow the creation of duplicate shortcuts (i.e. same
							// url, same title, but different screen position).
							intent.putExtra(EXTRA_SHORTCUT_DUPLICATE, false);

							ctx.sendBroadcast(intent);

							// Instant start

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
