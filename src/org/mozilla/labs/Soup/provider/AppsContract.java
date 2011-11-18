/* 
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mozilla.labs.Soup.provider;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.labs.Soup.app.SharedSettings;
import org.mozilla.labs.Soup.app.SoupApplication;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Convenience definitions for AppsProvider
 */
public final class AppsContract {

	private static final String TAG = "AppsContract";

	/**
	 * Special value for SyncColumns indicating that an entry has never been updated, or doesn't exist yet.
	 */
	public static final long UPDATED_NEVER = -2;

	/**
	 * Special value for SyncColumns indicating that the last update time is unknown, usually when inserted from a local
	 * file source.
	 */
	public static final long UPDATED_UNKNOWN = -1;

	/**
	 * Apps table
	 */
	public static final class Apps implements BaseColumns {

		public static final String AUTHORITY = "org.mozilla.labs.Soup.provider.AppsProvider";

		/**
		 * The content:// style URL for this table
		 */
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/apps");

		/**
		 * The MIME type of {@link #CONTENT_URI} providing a directory of notes.
		 */
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.mozilla.apps";

		/**
		 * The MIME type of a {@link #CONTENT_URI} sub-directory of a single note.
		 */
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.mozilla.apps";

		/**
		 * The default sort order for this table
		 */
		public static final String DEFAULT_SORT_ORDER = "name ASC";

		public static final String ORIGIN = "origin";

		public static final String MANIFEST = "manifest";

		public static final String MANIFEST_URL = "manifest_url";

		public static final String NAME = "name";

		public static final String DESCRIPTION = "description";

		public static final String ICON = "icon";

		public static final String INSTALL_DATA = "install_data";

		public static final String INSTALL_ORIGIN = "install_origin";

		public static final String INSTALL_RECEIPT = "receipt";

		public static final String INSTALL_TIME = "install_time";

		public static final String UPDATED_DATE = "updated_date";

		public static final String VERIFIED_DATE = "verified_date";

		/**
		 * The timestamp for when the app was created
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String CREATED_DATE = "created_date";

		/**
		 * The timestamp for when the note was last modified
		 * <P>
		 * Type: INTEGER (long)
		 * </P>
		 */
		public static final String MODIFIED_DATE = "modified_date";

		private final static String URL_VERIFY = "https://myapps.mozillalabs.com/verify";
		
		private final static String[] APP_PROJECTION = new String[] { Apps.ORIGIN, Apps.MANIFEST, Apps.INSTALL_DATA, Apps.INSTALL_ORIGIN,
				Apps.INSTALL_TIME };

		private static JSONObject verifyAuth = null;

		public static JSONObject toJSONObject(Cursor cur) {
			String manifest = cur.getString(cur.getColumnIndex(Apps.MANIFEST));

			JSONObject app = new JSONObject();

			try {
				app.put("origin", cur.getString(cur.getColumnIndex(Apps.ORIGIN)));
				app.put("manifest", new JSONObject(manifest));
				app.put("install_data", cur.getString(cur.getColumnIndex(Apps.INSTALL_DATA)));
				app.put("install_origin", cur.getString(cur.getColumnIndex(Apps.INSTALL_ORIGIN)));
				app.put("install_time", cur.getString(cur.getColumnIndex(Apps.INSTALL_TIME)));
			} catch (JSONException e) {
				return null;
			}

			return app;
		}

		public static JSONObject syncLogin(Context ctx) {

			if (verifyAuth != null) {
				return verifyAuth;
			}
			
			// Get config

			SharedPreferences settings = ctx.getSharedPreferences(SharedSettings.PREFS_NAME,
					SoupApplication.MODE_PRIVATE);
			URI storeUri = null;
			try {
				storeUri = new URI(settings.getString("dev_store", "https://apps-preview.allizom.org"));
			} catch (URISyntaxException e2) {
			}
			String audience = storeUri.getScheme() + "://" + storeUri.getAuthority();

			String assertion = null;
			try {
				assertion = new JSONObject(settings.getString("assertions", new JSONObject().toString()))
						.optString(audience);
			} catch (JSONException e1) {
			}

			if (assertion == null) {
				Log.w(TAG, "syncLogin found no assertion for " + audience);
				return null;
			}

			// Make request

			String url = URL_VERIFY;

			HttpClient client = new DefaultHttpClient();
			HttpPost request = new HttpPost(url);

			List<NameValuePair> params = new ArrayList<NameValuePair>(2);
			params.add(new BasicNameValuePair("audience", audience));
			params.add(new BasicNameValuePair("assertion", assertion));
			try {
				request.setEntity(new UrlEncodedFormEntity(params));
			} catch (UnsupportedEncodingException e) {
				Log.w(TAG, "syncLogin failed setEntity", e);
				return null;
			}

			HttpResponse response = null;
			try {
				response = client.execute(request);
			} catch (Exception e) {
				Log.w(TAG, "syncLogin failed execute", e);
				return null;
			}

			client.getConnectionManager().shutdown();

			// Evaluate response

			JSONObject responseBody = null;
			try {
				responseBody = new JSONObject(EntityUtils.toString(response.getEntity()));
			} catch (Exception e) {
				Log.w(TAG, "syncLogin failed responseBody", e);
			}

			Log.i(TAG, responseBody.toString());

			verifyAuth = responseBody;
			
			return verifyAuth;
		}
		
		public static JSONArray localList(Activity ctx) {
			Cursor cur = ctx.managedQuery(Apps.CONTENT_URI, APP_PROJECTION, null, null, Apps.DEFAULT_SORT_ORDER);

			cur.moveToFirst();

			JSONArray list = new JSONArray();

			while (cur.isAfterLast() == false) {
				JSONObject app = Apps.toJSONObject(cur);
				
				if (app != null) {
					list.put(app);
				}

				cur.moveToNext();
			}
			
			return list;
		}

		public static JSONArray syncedList(Activity ctx) {

			if (syncLogin(ctx) == null) {
				return localList(ctx);
			}
			
			Cursor cur = ctx.managedQuery(Apps.CONTENT_URI, APP_PROJECTION, null, null, Apps.DEFAULT_SORT_ORDER);

			cur.moveToFirst();

			JSONObject list = new JSONObject();

			while (cur.isAfterLast() == false) {
				JSONObject app = Apps.toJSONObject(cur);
				
				if (app != null) {
					try {
						list.put(app.optString("origin"), app);
					} catch (JSONException e) {}
				}

				cur.moveToNext();
			}
			
			// Prepare request
			
			SharedPreferences settings = ctx.getSharedPreferences(SharedSettings.PREFS_NAME,
					SoupApplication.MODE_PRIVATE);
			int since = settings.getInt("sync_since", 0);
			
			Uri.Builder builder = Uri.parse(verifyAuth.optString("collection_url")).buildUpon();
			builder.appendQueryParameter("since", String.valueOf(since));
			String url = builder.build().toString();
			
			// Make request

			HttpClient client = new DefaultHttpClient();
			HttpGet request = new HttpGet(url);
			
			request.addHeader("Signature", verifyAuth.optString("authentication_header"));

			HttpResponse response = null;
			try {
				response = client.execute(request);
			} catch (Exception e) {
				Log.w(TAG, "syncLogin failed execute " + url, e);
				return localList(ctx);
			}

			client.getConnectionManager().shutdown();

			// Evaluate response

			JSONObject responseBody = null;
			try {
				responseBody = new JSONObject(EntityUtils.toString(response.getEntity()));
			} catch (Exception e) {
				Log.w(TAG, "syncLogin failed responseBody for " + url, e);
				return localList(ctx);
			}
			
			Log.d(TAG, "syncedList " + responseBody);
			
			return localList(ctx);
		}
	}
}
