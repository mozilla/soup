package org.mozilla.labs.Soup.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;

public class SoupClient {

	private static final String TAG = "SoupClient";

	private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;

	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";

	private static HttpClient httpClient;
	private static JSONObject authorization;

	/**
	 * Execute a {@link HttpGet} request, passing a valid response through
	 * {@link XmlHandler#parseAndApply(XmlPullParser, ContentResolver)}.
	 */
	private static JSONObject executeGet(Context ctx, String url,
			String authorization) throws Exception {
		final HttpGet request = new HttpGet(url);

		if (authorization != null) {
			request.setHeader("Authorization", authorization);
		}

		return execute(ctx, request);
	}

	/**
	 * Execute a {@link HttpGet} request, passing a valid response through
	 * {@link XmlHandler#parseAndApply(XmlPullParser, ContentResolver)}.
	 * 
	 * @return
	 */
	private static JSONObject executePost(Context ctx, final String url,
			final List<NameValuePair> params) throws Exception {
		final HttpPost request = new HttpPost(url);

		if (params != null) {
			try {
				request.setEntity(new UrlEncodedFormEntity(params));
			} catch (UnsupportedEncodingException e) {
				Log.w(TAG, "Could not encode entity", e);
			}
		}

		return execute(ctx, request);
	}

	/**
	 * Execute this {@link HttpUriRequest}, passing a valid response through
	 * {@link XmlHandler#parseAndApply(XmlPullParser, ContentResolver)}.
	 */
	private static JSONObject execute(Context ctx, HttpUriRequest request)
			throws Exception {
		try {
			final HttpResponse resp = getHttpClient(ctx).execute(request);

			final int status = resp.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				throw new Exception("Unexpected server response "
						+ resp.getStatusLine() + " for " + request.getRequestLine());
			}

			try {
				return new JSONObject(EntityUtils.toString(resp.getEntity()));
			} catch (Exception e) {
				throw new Exception("Malformed response for "
						+ request.getRequestLine(), e);
			}
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * Generate and return a {@link HttpClient} configured for general use, including setting an application-specific
	 * user-agent string.
	 */
	public static HttpClient getHttpClient(Context context) {
		if (httpClient != null) {
			return httpClient;
		}

		final HttpParams params = new BasicHttpParams();

		// Use generous timeouts for slow mobile networks
		HttpConnectionParams.setConnectionTimeout(params, 20 * SECOND_IN_MILLIS);
		HttpConnectionParams.setSoTimeout(params, 20 * SECOND_IN_MILLIS);

		HttpConnectionParams.setSocketBufferSize(params, 8192);
		HttpProtocolParams.setUserAgent(params, buildUserAgent(context));

		final DefaultHttpClient client = new DefaultHttpClient(params);

		client.addRequestInterceptor(new HttpRequestInterceptor() {
			public void process(HttpRequest request, HttpContext context) {
				// Add header to accept gzip content
				if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
					request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
				}
			}
		});

		client.addResponseInterceptor(new HttpResponseInterceptor() {
			public void process(HttpResponse response, HttpContext context) {
				// Inflate any responses compressed with gzip
				final HttpEntity entity = response.getEntity();
				final Header encoding = entity.getContentEncoding();
				if (encoding != null) {
					for (HeaderElement element : encoding.getElements()) {
						if (element.getName().equalsIgnoreCase(ENCODING_GZIP)) {
							response.setEntity(new InflatingEntity(response.getEntity()));
							break;
						}
					}
				}
			}
		});

		httpClient = client;

		return client;
	}

	/**
	 * Build and return a user-agent string that can identify this application to remote servers. Contains the package
	 * name and version code.
	 */
	private static String buildUserAgent(Context context) {
		try {
			final PackageManager manager = context.getPackageManager();
			final PackageInfo info = manager.getPackageInfo(context.getPackageName(),
					0);

			// Some APIs require "(gzip)" in the user-agent string.
			return info.packageName + "/" + info.versionName + " ("
					+ info.versionCode + ") (gzip)";
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	/**
	 * Simple {@link HttpEntityWrapper} that inflates the wrapped {@link HttpEntity} by passing it through
	 * {@link GZIPInputStream}.
	 */
	private static class InflatingEntity extends HttpEntityWrapper {
		public InflatingEntity(HttpEntity wrapped) {
			super(wrapped);
		}

		@Override
		public InputStream getContent() throws IOException {
			return new GZIPInputStream(wrappedEntity.getContent());
		}

		@Override
		public long getContentLength() {
			return -1;
		}
	}

	private static boolean authorize(Context ctx) {

		if (authorization != null) {
			return true;
		}

		// Get config

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		Uri syncUri = null;
		syncUri = Uri.parse(prefs.getString("dev_sync",
				"https://myapps.mozillalabs.com"));
		String audience = syncUri.getScheme() + "://" + syncUri.getAuthority();

		String assertion = null;
		try {
			assertion = new JSONObject(prefs.getString("assertions",
					new JSONObject().toString())).optString(audience);
		} catch (JSONException e) {
		}

		if (assertion == null) {
			Log.w(TAG, "Missing assertion for " + audience);
			return false;
		}

		Log.d(TAG, "Authenticate for " + audience);

		// Make request

		String url = syncUri.buildUpon().path("/verify").toString();

		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
		params.add(new BasicNameValuePair("audience", audience));
		params.add(new BasicNameValuePair("assertion", assertion));

		JSONObject response = null;
		try {
			response = executePost(ctx, url, params);
		} catch (Exception e) {
			Log.w(TAG, "Could not execute request", e);
			return false;
		}

		if (!response.has("http_authorization") || !response.has("collection_url")) {
			Log.w(TAG, "Missing fields in response " + response);
			return false;
		}

		authorization = response;

		return true;
	}

	public static JSONObject getAllApps(Context ctx, int localSince) {
		if (!authorize(ctx)) {
			return null;
		}

		Uri.Builder builder = Uri.parse(
				authorization.optString("collection_url")).buildUpon();
		builder.appendQueryParameter("since", String.valueOf(localSince));
		String url = builder.build().toString();

		// Make request

		try {
			return executeGet(ctx, url, authorization.optString("http_authorization"));
		} catch (Exception e) {
			Log.w(TAG, "getAllApps failed for " + url, e);
			return null;
		}
	}
	
	public static String verifyId(Context ctx, String assertion, String audience) {

		
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		Uri.Builder builder = Uri.parse(prefs.getString("dev_identity",
				"https://browserid.org")).buildUpon();
		builder.path("/verify");
		
		String url = builder.build().toString();
		
		Log.d(TAG, "verifyId for " + audience + " from " + url + ": " + assertion);

		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
		params.add(new BasicNameValuePair("audience", audience));
		params.add(new BasicNameValuePair("assertion", assertion));
		
		JSONObject response;
		try {
			response = executePost(ctx, url, params);
		} catch (Exception e) {
			Log.w(TAG, "verifyId failed", e);
			return null;
		}
		
		String email = response.optString("email");
		
		if (TextUtils.isEmpty(email)) {
			Log.w(TAG, "verifyId response had no email " + response);
			return null;
		}
		
		Log.w(TAG, "verifyId returned email " + email);

		return email;
	}

}
