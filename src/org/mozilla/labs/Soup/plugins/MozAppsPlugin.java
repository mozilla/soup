package org.mozilla.labs.Soup.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.labs.Soup.R;
import org.mozilla.labs.Soup.http.ImageFactory;
import org.mozilla.labs.Soup.provider.AppsContract.Apps;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class MozAppsPlugin extends Plugin {

	private static final String TAG = "MozAppsPlugin";

	public static final String ACTION_INSTALL = "install";

	public static final String ACTION_AM_INSTALLED = "amInstalled";

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
							Toast.makeText(
									ctx,
									app.optJSONObject("manifest").optString("name")
											+ " already installed", Toast.LENGTH_LONG).show();
						}
					});

					result = new PluginResult(Status.OK, app);
				} else {

					Log.d(TAG, "Install dance");
					install(callback, originUri.toString(), data.optJSONObject(1), origin);
					Log.d(TAG, "Install dance DONE");

					result = new PluginResult(Status.NO_RESULT);
					result.setKeepCallback(true);
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

		Log.d(TAG, "Returns " + result.getJSONString());

		return result;
	}

	private Cursor findAppByOrigin(String origin) {
		String[] projection = new String[] { Apps.ORIGIN, Apps.MANIFEST,
				Apps.INSTALL_DATA, Apps.INSTALL_ORIGIN, Apps.INSTALL_TIME };

		Cursor cur = ctx.managedQuery(Apps.CONTENT_URI, projection, Apps.ORIGIN
				+ " = ?", new String[] { origin }, Apps.DEFAULT_SORT_ORDER);

		if (cur.moveToFirst() == false) {
			return null;
		}

		return cur;
	}

	public synchronized void install(final String callbackId,
			final String manifestUri, final JSONObject install_data,
			final String origin) throws Exception {
		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet(manifestUri);
		HttpResponse responseGet = client.execute(get);
		String manifestResponse = EntityUtils.toString(responseGet.getEntity());

		JSONObject manifest = new JSONObject(manifestResponse);

		Log.d(TAG, "Parsed manifest: " + manifest);

		final ContentValues values = new ContentValues();

		final String name = manifest.getString("name");
		final String description = manifest.getString("description");
		values.put(Apps.NAME, name);
		values.put(Apps.DESCRIPTION, description);

		Bitmap icon = null;
		JSONObject icons = manifest.optJSONObject("icons");

		if (icons != null && icons.length() > 0) {
			JSONArray sizes = icons.names();

			List<Integer> sizesSort = new ArrayList<Integer>();
			for (int i = 0, l = sizes.length(); i < l; i++) {
				sizesSort.add(sizes.getInt(i));
			}
			String max = Collections.max(sizesSort).toString();

			String iconUrl = origin + icons.getString(max);
			icon = ImageFactory.getResizedImage(iconUrl, 72, 72);
		}

		final Bitmap bitmap = icon;

		if (bitmap != null) {
			values.put(Apps.ICON, ImageFactory.bitmapToBytes(bitmap));
		} else {
			Log.w(TAG, "Could not load icon from " + icons);
		}

		values.put(Apps.ORIGIN, origin);
		values.put(Apps.MANIFEST_URL, manifestUri);
		values.put(Apps.MANIFEST, manifest.toString());

		if (install_data != null) {
			values.put(Apps.INSTALL_DATA, install_data.toString());

			if (install_data.has("receipt")) {
				values.put(Apps.INSTALL_RECEIPT, install_data.optString("receipt"));
			}
		}

		ctx.runOnUiThread(new Runnable() {

			public void run() {

				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(ctx);

				final boolean[] appSettings = new boolean[] {
						prefs.getBoolean("install_launch", true),
						prefs.getBoolean("install_shortcut", true) };

				AlertDialog.Builder installDlg = new AlertDialog.Builder(ctx);

				installDlg
						.setTitle("Install " + name + "?")
						.setMultiChoiceItems(R.array.install_dialog_array, appSettings,
								new DialogInterface.OnMultiChoiceClickListener() {
									public void onClick(DialogInterface dialog, int whichButton,
											boolean isChecked) {
										appSettings[whichButton] = isChecked;
									}
								}).setCancelable(true);

				// TODO: Custom layout. Message breaks multiple choice
				// .setMessage(description); // TODO: Truncate text with TextUtils.EllipsizeCallback

				if (bitmap != null) {
					installDlg.setIcon(new BitmapDrawable(bitmap));
				}

				installDlg.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								// FIXME: add error object
								error(new PluginResult(Status.ERROR, 0), callbackId);
							}
						}).setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {

								final Uri uri = ctx.getContentResolver().insert(
										Apps.CONTENT_URI, values);

								if (uri == null) {
									// FIXME: add error object
									error(new PluginResult(Status.ERROR, 0), callbackId);
									return;
								}

								success(new PluginResult(Status.OK, 0), callbackId);

								Intent shortcutIntent = new Intent(Intent.ACTION_VIEW, uri);
								shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

								if (appSettings[1]) {
									Intent intent = new Intent();
									intent
											.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
									intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
									intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
									if (bitmap != null) {
										intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
									}
									// Disallow the creation of duplicate shortcuts (i.e. same
									// url, same title, but different screen position).
									intent.putExtra("duplicate", false);

									ctx.sendBroadcast(intent);
								}

								if (appSettings[0]) {
									ctx.startActivity(shortcutIntent);
								}

							}
						});

				installDlg.create();
				installDlg.show();
			}
		});
	}
}
