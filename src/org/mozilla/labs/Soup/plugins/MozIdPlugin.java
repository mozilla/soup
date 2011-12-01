package org.mozilla.labs.Soup.plugins;

import java.net.URI;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.labs.Soup.R;
import org.mozilla.labs.Soup.app.AppActivity;
import org.mozilla.labs.Soup.app.SoupApplication;
import org.mozilla.labs.Soup.provider.AppsContract.Apps;
import org.mozilla.labs.Soup.service.SoupClient;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class MozIdPlugin extends Plugin {

	private static final String TAG = "MozIdPlugin";

	public static final String ACTION_PRE_VERIFY = "preVerify";

	public static final String ACTION_POST_VERIFY = "postVerify";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.phonegap.api.Plugin#execute(java.lang.String, org.json.JSONArray, java.lang.String)
	 */
	@Override
	public PluginResult execute(String action, JSONArray data, String callback) {
		Log.d(TAG, "Called " + action + ": " + data);

		try {

			if (action.equals("preVerify")) {

				URI uri = new URI(webView.getUrl());
				String audience = uri.getScheme() + "://" + uri.getHost();

				return preVerify(audience);

			} else if (action.equals("postVerify")) {

				return postVerify(data.optString(0), data.optString(1));

			}

		} catch (Exception e) {
			Log.w(TAG, action + " failed", e);
			return new PluginResult(Status.JSON_EXCEPTION);
		}

		return new PluginResult(Status.INVALID_ACTION);
	}

	/**
	 * preVerify
	 * 
	 * @param audience
	 * @return PluginResult
	 * @throws Exception
	 */
	public PluginResult preVerify(String audience) throws Exception {

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		String urlId = prefs.getString("dev_identity", "https://browserid.org")
				+ "/sign_in";

		URI identUri = new URI(urlId);
		String identOrigin = identUri.getScheme() + "://" + identUri.getAuthority();

		// Fallback to sync service audience for local dashboard
		if (new URI(audience).getScheme().equals("file")) {
			URI syncUri = new URI(prefs.getString("dev_sync",
					"https://myapps.mozillalabs.com"));
			audience = syncUri.getScheme() + "://" + syncUri.getAuthority();
		}

		Log.d(TAG, "preVerify continues on " + urlId + " to verify " + audience);

		JSONObject assertions = new JSONObject(prefs.getString("assertions",
				new JSONObject().toString()));

		JSONObject event = new JSONObject().put("audience", audience)
				.put("url", urlId).put("origin", identOrigin);

		String email = prefs.getString("email", null);
		if (!TextUtils.isEmpty(email)) {
			Log.d(TAG, "preVerify has stored email " + email);

			event.put("email", email);
		}

		String assertion = assertions.optString(audience);
		if (!TextUtils.isEmpty(assertion)) {
			String verifiedEmail = SoupClient.verifyId(ctx, assertion, audience);

			Log.d(TAG, "preVerify verified " + verifiedEmail + " from " + assertion);

			if (!TextUtils.isEmpty(verifiedEmail)) {
				event.put("email", verifiedEmail).put("assertion", assertion);

				if (email != verifiedEmail) {
					prefs.edit().putString("email", verifiedEmail).commit();
					event.put("email", email);
				}
			} else {
				assertions.remove(audience);
				prefs.edit().putString("assertions", assertions.toString()).commit();
			}
		}

		Log.d(TAG, "preVerify returns " + event);

		return new PluginResult(Status.OK, event);
	}

	public PluginResult postVerify(final String audience, final String assertion)
			throws Exception {

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);

		final String verifiedEmail = SoupClient.verifyId(ctx, assertion, audience);

		Log.d(TAG, "postVerify returned " + verifiedEmail + " for " + audience);

		if (verifiedEmail != null) {
			JSONObject assertions = new JSONObject(prefs.getString("assertions",
					new JSONObject().toString()));
			assertions.put(audience, assertion);
			prefs.edit().putString("assertions", assertions.toString()).commit();

			if (!verifiedEmail.equals(prefs.getString("email", null))) {

				ctx.runOnUiThread(new Runnable() {

					public void run() {

						AlertDialog.Builder confirmDlg = new AlertDialog.Builder(ctx);

						confirmDlg.setTitle("Remember " + verifiedEmail
								+ " for all BrowserID logins?");

						confirmDlg.setNegativeButton(android.R.string.cancel,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
									}
								}).setPositiveButton(android.R.string.ok,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										prefs.edit().putString("email", verifiedEmail).commit();
									}
								});

						confirmDlg.create();
						confirmDlg.show();
					}
				});
			}

			((SoupApplication) ctx.getApplication()).triggerSync();

			return new PluginResult(Status.OK, assertion);

		} else {

			ctx.runOnUiThread(new Runnable() {

				public void run() {
					if (TextUtils.isEmpty(assertion) || assertion.equals("null")) {
						Toast.makeText(ctx, "Login failed (no assertion)",
								Toast.LENGTH_SHORT).show();
					} else {
						Toast.makeText(ctx, "Verify failed for " + assertion,
								Toast.LENGTH_SHORT).show();
					}

				}

			});

			return new PluginResult(Status.OK);
		}

	}

}
