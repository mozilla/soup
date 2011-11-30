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

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.labs.Soup.app.SoupActivity;
import org.mozilla.labs.Soup.http.ImageFactory;
import org.mozilla.labs.Soup.provider.AppsContract.Apps;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;

/**
 * Provides access to a database of apps.
 */
public class AppsProvider extends ContentProvider {

	private static final String TAG = "AppsProvider";

	private static final String DATABASE_NAME = "apps.db";
	private static final int DATABASE_VERSION = 6;
	private static final String APPS_TABLE_NAME = "apps";

	private static HashMap<String, String> sAppsProjectionMap;
	private static HashMap<String, String> sLiveFolderProjectionMap;

	private static final int APPS = 1;
	private static final int APP_ID = 2;
	private static final int LIVE_FOLDER_APPS = 3;
	
	private static final String SQL_CREATE = "CREATE TABLE " + APPS_TABLE_NAME + " (" + Apps._ID
			+ " INTEGER PRIMARY KEY," + Apps.ORIGIN + " TEXT," + Apps.NAME
			+ " TEXT," + Apps.DESCRIPTION + " TEXT," + Apps.ICON + " TEXT,"
			+ Apps.MANIFEST + " BLOB," + Apps.MANIFEST_URL + " TEXT,"
			+ Apps.INSTALL_DATA + " BLOB," + Apps.INSTALL_RECEIPT + " BLOB,"
			+ Apps.INSTALL_ORIGIN + " TEXT," + Apps.INSTALL_TIME + " INTEGER,"
			+ Apps.VERIFIED_DATE + " INTEGER," + Apps.SYNCED_DATE + " INTEGER,"
			+ Apps.STATUS + " INTEGER," + Apps.CREATED_DATE + " INTEGER,"
			+ Apps.MODIFIED_DATE + " INTEGER" + ");";

	private static final UriMatcher sUriMatcher;

	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.d(TAG + ".DatabaseHelper", "onCreate: " + SQL_CREATE);

			db.execSQL(SQL_CREATE);

			// FIXME: ONLY development

			generateData(db);
		}

		private void generateData(SQLiteDatabase db) {
			JSONArray list = SoupActivity.findAll();

			for (int i = 0, l = list.length(); i < l; i++) {
				JSONObject app = list.optJSONObject(i);

				String origin = app.optString("origin");
				JSONObject manifest = app.optJSONObject("manifest");

				ContentValues values = new ContentValues();
				try {
					values.put(Apps.NAME, manifest.getString("name"));
					values.put(Apps.DESCRIPTION, manifest.getString("description"));

					String iconUrl = origin
							+ manifest.getJSONObject("icons").getString("128");
					Bitmap bitmap = ImageFactory.getResizedImage(iconUrl, 72, 72);

					if (bitmap != null) {
						values.put(Apps.ICON, ImageFactory.bitmapToBytes(bitmap));
					} else {
						Log.w(TAG + ".DatabaseHelper", "could not fetch icon");
					}

					values.put(Apps.ORIGIN, origin);
					values.put(Apps.MANIFEST_URL, app.getString("manifest_url"));
					values.put(Apps.MANIFEST, manifest.toString());

					Log.d(TAG + ".DatabaseHelper", "generated " + values);

				} catch (JSONException e) {
					Log.d(TAG + ".DatabaseHelper", "loadValue", e);
					continue;
				}

				Long now = Long.valueOf(System.currentTimeMillis());

				// Make sure that the fields are all set
				if (values.containsKey(AppsContract.Apps.CREATED_DATE) == false) {
					values.put(AppsContract.Apps.CREATED_DATE, now);
				}
				if (values.containsKey(AppsContract.Apps.MODIFIED_DATE) == false) {
					values.put(AppsContract.Apps.MODIFIED_DATE, now);
				}
				if (values.containsKey(AppsContract.Apps.INSTALL_TIME) == false) {
					values.put(AppsContract.Apps.INSTALL_TIME, now);
				}
				if (values.containsKey(AppsContract.Apps.SYNCED_DATE) == false) {
					values.put(AppsContract.Apps.SYNCED_DATE, now);
				}
				if (values.containsKey(AppsContract.Apps.STATUS) == false) {
					values.put(AppsContract.Apps.STATUS, 0);
				}
				if (values.containsKey(AppsContract.Apps.SYNCED_DATE) == false) {
					values.put(AppsContract.Apps.SYNCED_DATE, 0);
				}

				if (values.containsKey(AppsContract.Apps.NAME) == false) {
					Resources r = Resources.getSystem();
					values.put(AppsContract.Apps.NAME,
							r.getString(android.R.string.untitled));
				}
				if (values.containsKey(AppsContract.Apps.DESCRIPTION) == false) {
					values.put(AppsContract.Apps.DESCRIPTION, "");
				}

				if (values.containsKey(AppsContract.Apps.MANIFEST) == false) {
					values.put(AppsContract.Apps.MANIFEST, new JSONObject().toString());
				}
				if (values.containsKey(AppsContract.Apps.MANIFEST_URL) == false) {
					values.put(AppsContract.Apps.MANIFEST_URL, "");
				}
				if (values.containsKey(AppsContract.Apps.INSTALL_DATA) == false) {
					values.put(AppsContract.Apps.INSTALL_DATA,
							new JSONObject().toString());
				}

				long rowId = db.insert(APPS_TABLE_NAME, null, values);

				if (rowId > 0) {
					Log.d(TAG + ".loadValues", "Added " + rowId + " with " + values);
				}

			}
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG + ".DatabaseHelper", "Upgrading database from version "
					+ oldVersion + " to " + newVersion
					+ ", which will destroy all old data");

			db.execSQL("DROP TABLE IF EXISTS apps");
			onCreate(db);
		}
	}

	private DatabaseHelper mOpenHelper;

	@Override
	public boolean onCreate() {
		Log.d(TAG, "onCreate");

		mOpenHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		qb.setTables(APPS_TABLE_NAME);

		switch (sUriMatcher.match(uri)) {
		case APPS:
			qb.setProjectionMap(sAppsProjectionMap);
			break;

		case APP_ID:
			qb.setProjectionMap(sAppsProjectionMap);
			qb.appendWhere(Apps._ID + "=" + uri.getPathSegments().get(1));
			break;

		case LIVE_FOLDER_APPS:
			qb.setProjectionMap(sLiveFolderProjectionMap);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = AppsContract.Apps.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null,
				orderBy);

		// Tell the cursor what uri to watch, so it knows when its source data changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case APPS:
		case LIVE_FOLDER_APPS:
			return Apps.CONTENT_TYPE;

		case APP_ID:
			return Apps.CONTENT_ITEM_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		// Validate the requested uri
		if (sUriMatcher.match(uri) != APPS) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		Long now = Long.valueOf(System.currentTimeMillis());

		// Make sure that the fields are all set
		if (values.containsKey(AppsContract.Apps.CREATED_DATE) == false) {
			values.put(AppsContract.Apps.CREATED_DATE, now);
		}
		if (values.containsKey(AppsContract.Apps.MODIFIED_DATE) == false) {
			values.put(AppsContract.Apps.MODIFIED_DATE, now);
		}
		if (values.containsKey(AppsContract.Apps.INSTALL_TIME) == false) {
			values.put(AppsContract.Apps.INSTALL_TIME, now);
		}

		if (values.containsKey(AppsContract.Apps.NAME) == false) {
			Resources r = Resources.getSystem();
			values
					.put(AppsContract.Apps.NAME, r.getString(android.R.string.untitled));
		}
		if (values.containsKey(AppsContract.Apps.DESCRIPTION) == false) {
			values.put(AppsContract.Apps.DESCRIPTION, "");
		}

		if (values.containsKey(AppsContract.Apps.MANIFEST) == false) {
			values.put(AppsContract.Apps.MANIFEST, new JSONObject().toString());
		}
		if (values.containsKey(AppsContract.Apps.MANIFEST_URL) == false) {
			values.put(AppsContract.Apps.MANIFEST_URL, "");
		}
		if (values.containsKey(AppsContract.Apps.INSTALL_DATA) == false) {
			values.put(AppsContract.Apps.INSTALL_DATA, new JSONObject().toString());
		}
		
		if (values.containsKey(AppsContract.Apps.STATUS) == false) {
			values.put(AppsContract.Apps.STATUS, 0);
		}

		// TODO Add other defaults

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId = db.insert(APPS_TABLE_NAME, null, values);

		if (rowId > 0) {
			Uri appUri = ContentUris.withAppendedId(AppsContract.Apps.CONTENT_URI,
					rowId);
			getContext().getContentResolver().notifyChange(appUri, null);
			return appUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case APPS:
			count = db.delete(APPS_TABLE_NAME, where, whereArgs);
			break;

		case APP_ID:
			String appId = uri.getPathSegments().get(1);
			count = db.delete(APPS_TABLE_NAME,
					Apps._ID + "=" + appId
							+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where,
			String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case APPS:
			count = db.update(APPS_TABLE_NAME, values, where, whereArgs);
			break;

		case APP_ID:
			String appId = uri.getPathSegments().get(1);

//			values.put(AppsContract.Apps.MODIFIED_DATE,
//					Long.valueOf(System.currentTimeMillis()));

			count = db.update(APPS_TABLE_NAME, values, Apps._ID + "=" + appId
					+ (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
					whereArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(Apps.AUTHORITY, "apps", APPS);
		sUriMatcher.addURI(Apps.AUTHORITY, "apps/#", APP_ID);
		sUriMatcher.addURI(Apps.AUTHORITY, "live_folders/apps", LIVE_FOLDER_APPS);

		sAppsProjectionMap = new HashMap<String, String>();
		sAppsProjectionMap.put(Apps._ID, Apps._ID);
		sAppsProjectionMap.put(Apps.ORIGIN, Apps.ORIGIN);
		sAppsProjectionMap.put(Apps.MANIFEST, Apps.MANIFEST);
		sAppsProjectionMap.put(Apps.MANIFEST_URL, Apps.MANIFEST_URL);
		sAppsProjectionMap.put(Apps.NAME, Apps.NAME);
		sAppsProjectionMap.put(Apps.DESCRIPTION, Apps.DESCRIPTION);
		sAppsProjectionMap.put(Apps.ICON, Apps.ICON);
		sAppsProjectionMap.put(Apps.INSTALL_DATA, Apps.INSTALL_DATA);
		sAppsProjectionMap.put(Apps.INSTALL_ORIGIN, Apps.INSTALL_ORIGIN);
		sAppsProjectionMap.put(Apps.INSTALL_TIME, Apps.INSTALL_TIME);
		sAppsProjectionMap.put(Apps.STATUS, Apps.STATUS);
		sAppsProjectionMap.put(Apps.SYNCED_DATE, Apps.SYNCED_DATE);
		sAppsProjectionMap.put(Apps.CREATED_DATE, Apps.CREATED_DATE);
		sAppsProjectionMap.put(Apps.MODIFIED_DATE, Apps.MODIFIED_DATE);

		// Support for Live Folders.
		sLiveFolderProjectionMap = new HashMap<String, String>();
		sLiveFolderProjectionMap.put(LiveFolders._ID, Apps._ID + " AS "
				+ LiveFolders._ID);
		sLiveFolderProjectionMap.put(LiveFolders.NAME, Apps.NAME + " AS "
				+ LiveFolders.NAME);
		sLiveFolderProjectionMap.put(LiveFolders.DESCRIPTION, Apps.DESCRIPTION
				+ " AS " + LiveFolders.DESCRIPTION);
		sLiveFolderProjectionMap.put(LiveFolders.ICON_BITMAP, Apps.ICON + " AS "
				+ LiveFolders.ICON_BITMAP);

		// Add more columns here for more robust Live Folders.
	}
}
