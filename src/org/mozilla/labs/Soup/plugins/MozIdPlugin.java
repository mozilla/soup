package org.mozilla.labs.Soup.plugins;

import java.net.URI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.labs.Soup.app.SoupApplication;
import org.mozilla.labs.Soup.service.SoupClient;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class MozIdPlugin extends Plugin {

	private static final String TAG = "MozIdPlugin";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.phonegap.api.Plugin#execute(java.lang.String, org.json.JSONArray, java.lang.String)
	 */
	@Override
	public PluginResult execute(String action, JSONArray data, String callbackId) {
		Log.d(TAG, "Called " + action + ": " + data);

		try {

			if (action.equals("preVerify")) {

				URI uri = new URI(webView.getUrl());
				String audience = uri.getScheme() + "://" + uri.getHost();

				return preVerify(audience, callbackId);

			} else if (action.equals("postVerify")) {

				return postVerify(data, callbackId);

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
	public PluginResult preVerify(String defaultAudience, final String callbackId)
			throws Exception {

		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		String urlId = prefs.getString("dev_identity", "https://browserid.org")
				+ "/sign_in";

		URI identUri = new URI(urlId);
		String identOrigin = identUri.getScheme() + "://" + identUri.getAuthority();

		// Fallback to sync service audience for local dashboard
		if (new URI(defaultAudience).getScheme().equals("file")) {
			URI syncUri = new URI(prefs.getString("dev_sync",
					"https://myapps.mozillalabs.com"));
			defaultAudience = syncUri.getScheme() + "://" + syncUri.getAuthority();
		}

		final String audience = defaultAudience;

		Log.d(TAG, "preVerify continues on " + urlId + " to verify " + audience);

		final JSONObject assertions = new JSONObject(prefs.getString("assertions",
				new JSONObject().toString()));

		final JSONObject event = new JSONObject().put("audience", audience)
				.put("url", urlId).put("origin", identOrigin);

		final String email = prefs.getString("email", null);
		if (!TextUtils.isEmpty(email)) {
			Log.d(TAG, "preVerify has stored email " + email);

			event.put("email", email);
		}

		final String assertion = assertions.optString(audience);
		if (!TextUtils.isEmpty(assertion)) {
			ctx.runOnUiThread(new Runnable() {

				public void run() {

					ProgressDialog dlg = ProgressDialog.show(ctx, null, "Verifying email", true, false);
					
					String verifiedEmail = SoupClient.verifyId(ctx, assertion, audience);

					Log.d(TAG, "preVerify verified " + verifiedEmail + " from "
							+ assertion);

					if (!TextUtils.isEmpty(verifiedEmail)) {
						try {
							event.put("email", verifiedEmail).put("assertion", assertion);

							if (email != verifiedEmail) {
								prefs.edit().putString("email", verifiedEmail).commit();
								event.put("email", email);
							}
						} catch (JSONException e) {
						}
					} else {
						assertions.remove(audience);
						prefs.edit().putString("assertions", assertions.toString())
								.commit();
					}
					
					Log.d(TAG, "preVerify returns " + event);
					
					dlg.dismiss();

					success(new PluginResult(Status.OK, event), callbackId);
				}
			});
			
			PluginResult result = new PluginResult(Status.NO_RESULT);
			result.setKeepCallback(true);
			return result;
		}
		
		Log.d(TAG, "preVerify returns " + event);

		return new PluginResult(Status.OK, event);
	}

	public PluginResult postVerify(final JSONArray data, final String callbackId)
			throws Exception {

		if (data.isNull(1) || TextUtils.isEmpty(data.optString(1))) {
			ctx.runOnUiThread(new Runnable() {

				public void run() {
					Toast.makeText(ctx, "Login failed (no assertion)", Toast.LENGTH_SHORT)
							.show();
				}

			});

			return new PluginResult(Status.OK);
		}

		final String audience = data.optString(0);
		final String assertion = data.optString(1);

		ctx.runOnUiThread(new Runnable() {

			public void run() {

				ProgressDialog dlg = ProgressDialog.show(ctx, null, "Verifying email", true, false);

				final String verifiedEmail = SoupClient.verifyId(ctx, assertion,
						audience);

				Log.d(TAG, "postVerify returned " + verifiedEmail + " for " + audience
						+ " using " + assertion);

				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(ctx);

				if (verifiedEmail != null) {

					// Save assertions to preferences
					JSONObject assertions = null;
					try {
						assertions = new JSONObject(prefs.getString("assertions",
								new JSONObject().toString()));
						assertions.put(audience, assertion);
					} catch (JSONException e) {
					}
					prefs.edit().putString("assertions", assertions.toString()).commit();

					dlg.dismiss();

					// Save email if new
					if (!verifiedEmail.equals(prefs.getString("email", null))) {
						prefs.edit().putString("email", verifiedEmail).commit();

						ctx.runOnUiThread(new Runnable() {

							public void run() {
								Toast.makeText(ctx, "Remembered login for " + verifiedEmail,
										Toast.LENGTH_SHORT).show();
							}
						});
					}
					
					dlg.dismiss();

					success(new PluginResult(Status.OK, 0), callbackId);

					((SoupApplication) ctx.getApplication()).triggerSync();
				}
			}
		});

		PluginResult result = new PluginResult(Status.NO_RESULT);
		result.setKeepCallback(true);
		return result;
	}

}
