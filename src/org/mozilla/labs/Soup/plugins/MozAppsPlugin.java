package org.mozilla.labs.Soup.plugins;

import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.labs.Soup.app.AppActivity;
import org.mozilla.labs.Soup.http.ImageFactory;
import org.mozilla.labs.Soup.provider.AppsContract.Apps;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class MozAppsPlugin extends Plugin {

	private static final String TAG = "MozAppsPlugin";

	public static final String ACTION_INSTALL = "install";

	public static final String ACTION_AM_INSTALLED = "amInstalled";

	private Cursor findAppByOrigin(String origin) {
		String[] projection = new String[] { Apps.ORIGIN, Apps.MANIFEST, Apps.INSTALL_DATA, Apps.INSTALL_ORIGIN,
				Apps.INSTALL_TIME };

		Cursor cur = ctx.managedQuery(Apps.CONTENT_URI, projection, Apps.ORIGIN + " = ?", new String[] { origin },
				Apps.DEFAULT_SORT_ORDER);

		if (cur.moveToFirst() == false) {
			return null;
		}

		return cur;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.phonegap.api.Plugin#execute(java.lang.String, org.json.JSONArray, java.lang.String)
	 */
	@Override
	public PluginResult execute(String action, JSONArray data, String callback) {
		Log.d(TAG, "Called with " + action);

		PluginResult result = null;

		Uri originUri = Uri.parse(data.optString(0, webView.getUrl()));
		String origin = originUri.getScheme() + "://" + originUri.getAuthority();

		Cursor cur = findAppByOrigin(origin);

		if (ACTION_INSTALL.equals(action)) {

			try {
				if (cur != null) {
					// TODO: Update install_data

					final JSONObject app = Apps.toJSONObject(cur);

					Log.d(TAG, "App was installed: " + app);

					ctx.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(ctx, app.optJSONObject("manifest").optString("name") + " already installed",
									Toast.LENGTH_LONG).show();
						}
					});

					result = new PluginResult(Status.OK, app);
				} else {
					String url = data.optString(0);
					JSONObject install_data = data.optJSONObject(1);

					HttpClient client = new DefaultHttpClient();
					HttpGet get = new HttpGet(url);
					HttpResponse responseGet = client.execute(get);
					String manifestResponse = EntityUtils.toString(responseGet.getEntity());

					JSONObject manifest = new JSONObject(manifestResponse);

					Log.d(TAG, "Parsed manifest: " + manifest);

					ContentValues values = new ContentValues();

					final String name = manifest.getString("name");
					values.put(Apps.NAME, name);
					values.put(Apps.DESCRIPTION, manifest.getString("description"));

					String iconUrl = origin + manifest.getJSONObject("icons").getString("128");
					final Bitmap bitmap = ImageFactory.getResizedImage(iconUrl, 72, 72);

					if (bitmap != null) {
						values.put(Apps.ICON, ImageFactory.bitmapToBytes(bitmap));
					} else {
						Log.w(TAG, "Icon failed: " + iconUrl);
					}

					values.put(Apps.ORIGIN, origin);
					values.put(Apps.MANIFEST_URL, url);
					values.put(Apps.MANIFEST, manifest.toString());

					if (install_data != null) {
						values.put(Apps.INSTALL_DATA, install_data.toString());

						if (install_data.has("receipt")) {
							values.put(Apps.INSTALL_RECEIPT, install_data.optString("receipt"));
						}
					}

					final Uri uri = ctx.getContentResolver().insert(Apps.CONTENT_URI, values);

					ctx.runOnUiThread(new Runnable() {
						public void run() {
							AlertDialog.Builder dlg = new AlertDialog.Builder(ctx);
							if (bitmap != null) {
								dlg.setIcon(new BitmapDrawable(bitmap));
							}
							dlg.setTitle("Installed " + name);
							dlg.setMessage("Launch it now?");
							dlg.setCancelable(true);

							dlg.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									final Intent shortcutIntent = new Intent(ctx, AppActivity.class);
									shortcutIntent.setAction(AppActivity.ACTION_WEBAPP);
									shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									shortcutIntent.putExtra("uri", uri);
								}
							});
							dlg.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
								}
							});

							dlg.create();
							dlg.show();
						}
					});

					result = new PluginResult(Status.OK);
				}

			} catch (Exception e) {
				Log.w(TAG, action + " failed", e);
				result = new PluginResult(Status.JSON_EXCEPTION);
			}
		} else if (ACTION_AM_INSTALLED.equals(action)) {
			try {
				if (cur != null) {
					JSONObject app = Apps.toJSONObject(cur);

					if (app != null) {
						result = new PluginResult(Status.OK, app);
					} else {
						result = new PluginResult(Status.ERROR);
					}
				} else {
					result = new PluginResult(Status.ERROR);
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
