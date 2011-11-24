package org.mozilla.labs.Soup.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.labs.Soup.provider.AppsContract.Apps;

import android.app.Activity;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;

public class SyncService extends IntentService {

	private static final String TAG = "SyncService";

	public static final String EXTRA_STATUS_RECEIVER = "org.mozilla.labs.soup.extra.STATUS_RECEIVER";

	public static final int STATUS_RUNNING = 0x1;
	public static final int STATUS_ERROR = 0x2;
	public static final int STATUS_FINISHED = 0x3;

	private static final int SECOND_IN_MILLIS = (int) DateUtils.SECOND_IN_MILLIS;

	private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
	private static final String ENCODING_GZIP = "gzip";

	private HttpClient httpClient;

	private ContentResolver resolver;

	private JSONObject syncSession;

	public SyncService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		httpClient = getHttpClient(this);
		resolver = getContentResolver();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent " + intent);

		final ResultReceiver receiver = intent
				.getParcelableExtra(EXTRA_STATUS_RECEIVER);
		if (receiver != null)
			receiver.send(STATUS_RUNNING, Bundle.EMPTY);

		final Context ctx = this;
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		final int localSince = prefs.getInt("sync_since", 0);

		try {

			/**
			 * Local list
			 */

			Cursor cur = resolver.query(Apps.CONTENT_URI, Apps.APP_PROJECTION, null,
					null, Apps.DEFAULT_SORT_ORDER);

			cur.moveToFirst();

			JSONObject list = new JSONObject();

			while (cur.isAfterLast() == false) {
				JSONObject app = Apps.toJSONObject(cur);

				if (app != null) {
					try {
						list.put(app.optString("origin"), app);
					} catch (JSONException e) {
					}
				}

				cur.moveToNext();
			}
			
			cur.close();

			// Prepare request

			Uri.Builder builder = Uri.parse(
					syncSession.optString("http_authorization")).buildUpon();
			builder.appendQueryParameter("since", String.valueOf(localSince));
			String url = builder.build().toString();

			// Make request

			JSONObject resopnse = executeGet(url,
					syncSession.optString("collection_url"));

			Log.d(TAG, "List: " + resopnse);

		} catch (Exception e) {
			Log.e(TAG, "Problem while syncing", e);

			if (receiver != null) {
				// Pass back error to surface listener
				final Bundle bundle = new Bundle();
				bundle.putString(Intent.EXTRA_TEXT, e.toString());
				receiver.send(STATUS_ERROR, bundle);
			}
		}

		// Announce success to any surface listener
		Log.d(TAG, "sync finished");
		if (receiver != null)
			receiver.send(STATUS_FINISHED, Bundle.EMPTY);
	}

	private boolean authenticate() {

		if (syncSession != null) {
			return true;
		}

		// Get config

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
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
			response = executePost(url, params);
		} catch (Exception e) {
			Log.w(TAG, "Could not execute request", e);
			return false;
		}

		if (!response.has("http_authorization") || !response.has("collection_url")) {
			Log.w(TAG, "Missing fields in response " + response);
			return false;
		}

		syncSession = response;

		return true;
	}

	/**
	 * Execute a {@link HttpGet} request, passing a valid response through
	 * {@link XmlHandler#parseAndApply(XmlPullParser, ContentResolver)}.
	 */
	public JSONObject executeGet(String url, String authorization)
			throws Exception {
		final HttpGet request = new HttpGet(url);

		if (authorization != null) {
			request.setHeader("Authorization", authorization);
		}

		return execute(request);
	}

	/**
	 * Execute a {@link HttpGet} request, passing a valid response through
	 * {@link XmlHandler#parseAndApply(XmlPullParser, ContentResolver)}.
	 * 
	 * @return
	 */
	public JSONObject executePost(final String url,
			final List<NameValuePair> params) throws Exception {
		final HttpPost request = new HttpPost(url);

		if (params != null) {
			try {
				((HttpResponse) request).setEntity(new UrlEncodedFormEntity(params));
			} catch (UnsupportedEncodingException e) {
				Log.w(TAG, "Could not encode entity", e);
			}
		}

		return execute(request);
	}

	/**
	 * Execute this {@link HttpUriRequest}, passing a valid response through
	 * {@link XmlHandler#parseAndApply(XmlPullParser, ContentResolver)}.
	 */
	public JSONObject execute(HttpUriRequest request) throws Exception {
		try {
			final HttpResponse resp = httpClient.execute(request);

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

}
