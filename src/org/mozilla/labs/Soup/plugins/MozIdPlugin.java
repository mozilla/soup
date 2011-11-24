package org.mozilla.labs.Soup.plugins;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

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

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		String urlId = settings.getString("dev_identity", "https://browserid.org")
				+ "/sign_in";

		URI uriId = new URI(urlId);
		String originId = uriId.getScheme() + "://" + uriId.getAuthority();

		if (new URI(audience).getScheme().equals("file")) {
			URI storeUri = new URI(settings.getString("dev_sync",
					"https://myapps.mozillalabs.com"));
			audience = storeUri.getScheme() + "://" + storeUri.getAuthority();
		}

		JSONObject assertions = new JSONObject(settings.getString("assertions",
				new JSONObject().toString()));

		Log.d(TAG, "preVerify continues on " + urlId + " to verify " + audience);

		JSONObject event = new JSONObject().put("audience", audience)
				.put("url", urlId).put("origin", originId);

		String assertion = assertions.optString(audience);
		if (!TextUtils.isEmpty(assertion)) {
			String email = verifyAssertion(assertion, audience, originId);

			if (!TextUtils.isEmpty(email)) {
				event.put("email", email).put("assertion", assertion);
			} else {
				assertions.remove(audience);
				settings.edit().putString("assertions", assertions.toString()).commit();
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

		Log.d(TAG, "postVerify assertion: " + assertion);

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(ctx);

		JSONObject assertions = new JSONObject(settings.getString("assertions",
				new JSONObject().toString()));
		assertions.put(audience, assertion);
		settings.edit().putString("assertions", assertions.toString()).commit();

		// JSONObject parsedAssertion = new JSONObject(new String(Base64.decode(assertion, Base64.DEFAULT)));

		// JSONObject certAssertion = new JSONObject(new
		// String(Base64.decode(parsedAssertion.optJSONArray("certificates")
		// .optString(1), Base64.DEFAULT)));

		// Log.d(TAG, "Parsed: " + certAssertion);

		return new PluginResult(Status.ERROR, assertion);
	}

	private String verifyAssertion(String assertion, String audience,
			String storeAuthority) {

		Log.d(TAG, "verifyAssertion for " + assertion + ", " + audience + " on "
				+ storeAuthority);

		HttpClient client = new DefaultHttpClient();
		HttpPost request = new HttpPost(storeAuthority + "/verify");

		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
		params.add(new BasicNameValuePair("audience", audience));
		params.add(new BasicNameValuePair("assertion", assertion));
		try {
			request.setEntity(new UrlEncodedFormEntity(params));
		} catch (UnsupportedEncodingException e) {
			return null;
		}

		HttpResponse response = null;
		try {
			response = client.execute(request);
		} catch (Exception e) {
			Log.w(TAG, "verifyAssertion failed execute", e);
			return null;
		}

		client.getConnectionManager().shutdown();

		if (response.getStatusLine().getStatusCode() != 200) {
			Log.w(TAG, "verifyAssertion failed with status "
					+ response.getStatusLine().getStatusCode());
			return null;
		}

		// Evaluate response

		String responseString = null;
		JSONObject responseBody = null;
		try {
			responseString = EntityUtils.toString(response.getEntity());
			responseBody = new JSONObject(responseString);
		} catch (Exception e) {
			Log.w(TAG, "verifyAssertion could not parse " + responseString, e);
			return null;
		}

		if (!responseBody.has("email")) {
			Log.w(TAG, "verifyAssertion has no email " + responseBody);
			return null;
		}

		return responseBody.optString("email");
	}
}
