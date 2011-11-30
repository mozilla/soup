package org.mozilla.labs.Soup.plugins;

import java.net.URI;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.labs.Soup.app.SoupApplication;
import org.mozilla.labs.Soup.service.SoupClient;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class MozIdPlugin extends Plugin {

	private static final String TAG = "MozIdPlugin";

	public static final String ACTION_PRE_VERIFY = "preVerify";

	public static final String ACTION_DURING_VERIFY = "duringVerify";

	public static final String ACTION_POST_VERIFY = "postVerify";

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.phonegap.api.Plugin#execute(java.lang.String, org.json.JSONArray, java.lang.String)
	 */
	@Override
	public PluginResult execute(String action, JSONArray data, String callback) {
		Log.d(TAG, "Called with " + action);

		PluginResult result = null;

		if (ACTION_PRE_VERIFY.equals(action)) {
			try {
				URI uri = new URI(webView.getUrl());
				String audience = uri.getScheme() + "://" + uri.getHost();
				result = preVerify(audience);
			} catch (Exception e) {
				Log.w(TAG, action + " failed", e);
				result = new PluginResult(Status.JSON_EXCEPTION);
			}
		} else if (ACTION_DURING_VERIFY.equals(action)) {
			try {
				result = duringVerify(data.optString(0));
			} catch (Exception e) {
				Log.w(TAG, action + " failed", e);
				result = new PluginResult(Status.JSON_EXCEPTION);
			}
		} else if (ACTION_POST_VERIFY.equals(action)) {
			try {
				result = postVerify(data.optString(0), data.optString(1));
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

	/**
	 * 
	 * @param audience
	 * @return
	 * @throws Exception
	 */
	public PluginResult duringVerify(String audience) throws Exception {

		Log.d(TAG, "duringVerify continues with " + audience);

		return new PluginResult(Status.OK, audience);
	}

	public PluginResult postVerify(final String audience, final String assertion)
			throws Exception {

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);

		String verifiedEmail = SoupClient.verifyId(ctx, assertion, audience);

		Log.d(TAG, "postVerify returned " + verifiedEmail + " for " + audience);

		if (verifiedEmail != null) {
			JSONObject assertions = new JSONObject(prefs.getString("assertions",
					new JSONObject().toString()));
			assertions.put(audience, assertion);
			prefs.edit().putString("assertions", assertions.toString())
					.putString("email", verifiedEmail).commit();
		}
		
		((SoupApplication) ctx.getApplication()).triggerSync();
		
		return new PluginResult(Status.ERROR, assertion);
	}

}
