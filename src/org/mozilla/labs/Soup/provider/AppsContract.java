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

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
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
		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
				+ "/apps");

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

		public final static String[] APP_PROJECTION = new String[] { Apps.ORIGIN,
				Apps.MANIFEST, Apps.INSTALL_DATA, Apps.INSTALL_ORIGIN,
				Apps.INSTALL_TIME };

		public static JSONObject toJSONObject(Cursor cur) {
			String manifest = cur.getString(cur.getColumnIndex(Apps.MANIFEST));

			JSONObject app = new JSONObject();

			try {
				app.put("origin", cur.getString(cur.getColumnIndex(Apps.ORIGIN)));
				app.put("manifest", new JSONObject(manifest));
				app.put("install_data",
						cur.getString(cur.getColumnIndex(Apps.INSTALL_DATA)));
				app.put("install_origin",
						cur.getString(cur.getColumnIndex(Apps.INSTALL_ORIGIN)));
				app.put("install_time",
						cur.getString(cur.getColumnIndex(Apps.INSTALL_TIME)));
			} catch (JSONException e) {
				return null;
			}

			return app;
		}

	}
}
