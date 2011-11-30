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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.labs.Soup.http.ImageFactory;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Convenience definitions for AppsProvider
 */
public final class AppsContract {

	@SuppressWarnings("unused")
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

		public static final String SYNCED_DATE = "synced_date";

		public static final String STATUS = "status";

		public static enum STATUS_ENUM {
			OK, DELETED
		}

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

		public final static String[] APP_PROJECTION = new String[] { Apps._ID,
				Apps.ORIGIN, Apps.MANIFEST, Apps.INSTALL_DATA, Apps.INSTALL_ORIGIN,
				Apps.INSTALL_TIME, Apps.MODIFIED_DATE, Apps.STATUS };

		public static JSONObject toJSONObject(Cursor cur) {
			return toJSONObject(cur, false);
		}

		public static JSONObject toJSONObject(Cursor cur, boolean extended) {
			boolean deleted = (cur.getInt(cur.getColumnIndex(Apps.STATUS)) == STATUS_ENUM.DELETED
					.ordinal());

			// Skip deleted entries for non-sync use
			if (!extended && deleted) {
				Log.d(TAG, "toJSONObject skipped entry");
				return null;
			}

			String manifest = cur.getString(cur.getColumnIndex(Apps.MANIFEST));

			JSONObject app = new JSONObject();

			try {
				app.put("origin", cur.getString(cur.getColumnIndex(Apps.ORIGIN)));
				app.put("manifest", new JSONObject(manifest));
				app.put("install_data",
						cur.getString(cur.getColumnIndex(Apps.INSTALL_DATA)));
				app.put("install_origin",
						cur.getString(cur.getColumnIndex(Apps.INSTALL_ORIGIN)));
				app.put("install_time", Long.valueOf(cur.getLong(cur
						.getColumnIndex(Apps.INSTALL_TIME)) / 1000));

				if (extended) {
					app.put("last_modified", Long.valueOf(cur.getLong(cur
							.getColumnIndex(Apps.MODIFIED_DATE)) / 1000));
					app.put("deleted", deleted);
				}
			} catch (JSONException e) {
				return null;
			}

			return app;
		}

		public static ContentValues toContentValues(final JSONObject app) {

			String origin = app.optString("origin");
			JSONObject manifest = app.optJSONObject("manifest");

			if (manifest == null || origin == null) {
				return null;
			}

			ContentValues values = new ContentValues();

			values.put(Apps.NAME, manifest.optString("name"));
			values.put(Apps.DESCRIPTION, manifest.optString("description"));

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
				icon = ImageFactory.getResizedImage(iconUrl, 72, 72);
			}

			if (icon != null) {
				values.put(Apps.ICON, ImageFactory.bitmapToBytes(icon));
			} else {
				Log.w(TAG, "Could not load icon from " + icons);
			}

			values.put(Apps.ORIGIN, origin);
			values.put(Apps.MANIFEST_URL, app.optString("manifest_url"));
			values.put(Apps.MANIFEST, manifest.toString());

			long installTime = app.optLong("install_time");
			if (installTime > 0) {
				installTime = Long.valueOf(System.currentTimeMillis() / 1000);
			}
			values.put(Apps.INSTALL_TIME, installTime * 1000);

			JSONObject installData = null;

			if (app.optBoolean("deleted")) {
				values.put(Apps.STATUS, STATUS_ENUM.DELETED.ordinal());
			} else {
				values.put(Apps.STATUS, STATUS_ENUM.OK.ordinal());
			}

			try {
				installData = new JSONObject(app.optString("install_data"));
			} catch (Exception e) {
			}

			if (installData != null) {
				values.put(Apps.INSTALL_DATA, installData.toString());
				if (installData.has("receipt")) {
					values.put(Apps.INSTALL_RECEIPT, installData.optString("receipt"));
				}
			}

			return values;
		}

		public static Cursor findAppByOrigin(Context ctx, String origin) {
			Cursor cur = ctx.getContentResolver().query(Apps.CONTENT_URI,
					Apps.APP_PROJECTION, Apps.ORIGIN + " = ?", new String[] { origin },
					Apps.DEFAULT_SORT_ORDER);

			if (cur.moveToFirst() == false) {
				return null;
			}

			return cur;
		}

	}

}
