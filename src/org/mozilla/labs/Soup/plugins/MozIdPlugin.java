package org.mozilla.labs.Soup.plugins;

import java.net.URI;

import net.oauth.jsontoken.JsonToken;
import net.oauth.jsontoken.JsonTokenParser;
import net.oauth.jsontoken.SystemClock;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.mozilla.labs.Soup.app.SharedSettings;
import org.mozilla.labs.Soup.app.SoupApplication;

import android.content.SharedPreferences;
import android.util.Base64;
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

		return result;
	}

	public PluginResult preVerify(String audience) throws Exception {

		SharedPreferences settings = ctx.getSharedPreferences(SharedSettings.PREFS_NAME, SoupApplication.MODE_PRIVATE);
		String urlId = settings.getString("dev_identity", "https://browserid.org") + "/sign_in";

		URI uriId = new URI(urlId);
		String originId = uriId.getScheme() + "://" + uriId.getAuthority();

		if (new URI(audience).getScheme().equals("file")) {
			URI storeUri = new URI(settings.getString("dev_store", "https://apps-preview.allizom.org"));
			audience = storeUri.getScheme() + "://" + storeUri.getAuthority();
		}

		Log.d(TAG, "preVerify continues on " + urlId + " to verify " + audience);

		JSONObject assertions = new JSONObject(settings.getString("assertions", new JSONObject().toString()));

		return new PluginResult(Status.OK, new JSONObject().put("audience", audience).put("url", urlId)
				.put("origin", originId).put("assertion", assertions.optString(audience)));
	}

	public PluginResult duringVerify(String audience) throws Exception {

		Log.d(TAG, "duringVerify continues with " + audience);

		return new PluginResult(Status.OK, audience);
	}

	public PluginResult postVerify(final String audience, final String assertion) throws Exception {

		Log.d(TAG, "postVerify assertion: " + assertion);

		SharedPreferences settings = ctx.getSharedPreferences(SharedSettings.PREFS_NAME, SoupApplication.MODE_PRIVATE);
		String urlStore = settings.getString("dev_store", "https://apps-preview.allizom.org");

		JSONObject assertions = new JSONObject(settings.getString("assertions", new JSONObject().toString()));
		assertions.put(audience, assertion);
		settings.edit().putString("assertions", assertions.toString()).commit();

		// JSONObject parsedAssertion = new JSONObject(new String(Base64.decode(assertion, Base64.DEFAULT)));

		// JSONObject certAssertion = new JSONObject(new
		// String(Base64.decode(parsedAssertion.optJSONArray("certificates")
		// .optString(1), Base64.DEFAULT)));

		// Log.d(TAG, "Parsed: " + certAssertion);

		return new PluginResult(Status.OK, assertion);
	}
}
