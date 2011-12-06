package org.mozilla.labs.Soup.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.labs.Soup.R;
import org.mozilla.labs.Soup.app.AppActivity;
import org.mozilla.labs.Soup.app.SoupApplication;
import org.mozilla.labs.Soup.http.ImageFactory;
import org.mozilla.labs.Soup.provider.AppsContract.Apps;
import org.mozilla.labs.Soup.service.SoupClient;

import android.app.AlertDialog;
import android.app.ProgressDialog;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.phonegap.api.Plugin#execute(java.lang.String, org.json.JSONArray, java.lang.String)
	 */
	@Override
	public PluginResult execute(String action, JSONArray data, String callback) {
		Log.d(TAG, "Called " + action + ": " + data);

		Uri originUri = Uri.parse(data.optString(0, webView.getUrl()));
		String origin = originUri.getScheme() + "://" + originUri.getAuthority();

		Cursor cur = Apps.findAppByOrigin(ctx, origin);

		try {

			if (action.equals("install")) {

				if (cur != null) {

					// TODO: Update install_data

					final JSONObject app = Apps.toJSONObject(cur);

					Log.d(TAG, "App was installed: " + app);

					ctx.runOnUiThread(new Runnable() {
						public void run() {
							Toast.makeText(ctx,
									app.optJSONObject("manifest").optString("name") + " updated",
									Toast.LENGTH_SHORT).show();
						}
					});

					return new PluginResult(Status.OK, app);
				}

				return install(callback, originUri.toString(), data.optJSONObject(1),
						origin);

			} else if (action.equals("amInstalled")) {

				if (cur != null) {
					JSONObject app = Apps.toJSONObject(cur);

					if (app != null) {
						return new PluginResult(Status.OK, app);
					}

					return new PluginResult(Status.ERROR);
				}

				return new PluginResult(Status.ERROR);
			}

		} catch (Exception e) {
			Log.w(TAG, action + " failed", e);
			return new PluginResult(Status.JSON_EXCEPTION);
		}

		if (cur != null) {
			cur.close();
		}

		return new PluginResult(Status.INVALID_ACTION);
	}

	public PluginResult install(final String callbackId,
			final String manifestUri, final JSONObject install_data,
			final String origin) throws Exception {

		ctx.runOnUiThread(new Runnable() {

			public void run() {

				ProgressDialog dlg = ProgressDialog.show(ctx, null,
						"Preparing installation", true, false);

				// TODO: More error codes (JSON vs IO)
				JSONObject manifest = SoupClient.getManifest(ctx, manifestUri);

				Log.d(TAG, "Parsed manifest: " + manifest);

				if (manifest == null) {
					dlg.dismiss();

					JSONObject errorEvent = null;
					try {
						errorEvent = new JSONObject().put("code", "networkError").put("message", "networkError");
					} catch (JSONException e) {
					}
					error(new PluginResult(Status.ERROR, errorEvent), callbackId);

					return;
				}

				final ContentValues values = new ContentValues();

				final String name = manifest.optString("name", "No Name");
				final String description = manifest.optString("description", "");
				values.put(Apps.NAME, name);
				values.put(Apps.DESCRIPTION, description);

				Bitmap icon = null;
				JSONObject icons = manifest.optJSONObject("icons");

				if (icons != null && icons.length() > 0) {
					JSONArray sizes = icons.names();

					List<Integer> sizesSort = new ArrayList<Integer>();
					for (int i = 0, l = sizes.length(); i < l; i++) {
						sizesSort.add(sizes.optInt(i));
					}
					String max = Collections.max(sizesSort).toString();
					
					String iconUrl = origin + icons.optString(max);
					
					Log.d(TAG, "Fetching icon " + max + ": " + iconUrl);
					
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

				final String launchUri = origin + manifest.optString("launch_path", "");

				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(ctx);

				final boolean[] appSettings = new boolean[] {
						prefs.getBoolean("install_shortcut", true),
						prefs.getBoolean("install_launch", true) };

				dlg.dismiss();

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

				if (bitmap != null) {
					installDlg.setIcon(new BitmapDrawable(bitmap));
				}

				installDlg.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {

								JSONObject errorEvent = null;
								try {
									errorEvent = new JSONObject().put("code", "denied").put(
											"message", "denied");
								} catch (JSONException e) {
								}
								error(new PluginResult(Status.ERROR, errorEvent), callbackId);

							}
						}).setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {

								final Uri uri = ctx.getContentResolver().insert(
										Apps.CONTENT_URI, values);

								if (uri == null) {
									JSONObject errorEvent = null;
									try {
										errorEvent = new JSONObject().put("code", "denied").put(
												"message", "denied");
									} catch (JSONException e) {
									}
									error(new PluginResult(Status.ERROR, errorEvent), callbackId);

									return;
								}

								success(new PluginResult(Status.OK, 0), callbackId);

								Intent shortcutIntent = new Intent(ctx, AppActivity.class);
								shortcutIntent.setAction(AppActivity.ACTION_WEBAPP);
								shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								shortcutIntent.putExtra("uri", launchUri);
								shortcutIntent.putExtra("app_uri", uri);

								// TODO: Move one more place to sync
								((SoupApplication) ctx.getApplication()).triggerSync();

								if (appSettings[0]) {
									Log.d(TAG, "Install creates shortcut");
									
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

								if (appSettings[1]) {
									Log.d(TAG, "Install launches app");
									
									ctx.startActivity(shortcutIntent);
								}

							}
						});

				installDlg.create();
				installDlg.show();
			}
		});

		PluginResult result = new PluginResult(Status.NO_RESULT);
		result.setKeepCallback(true);
		return result;
	}

}
