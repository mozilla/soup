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
import org.mozilla.labs.Soup.app.AppActivity;
import org.mozilla.labs.Soup.http.ImageFactory;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
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
     * Special value for SyncColumns indicating that an entry has never been
     * updated, or doesn't exist yet.
     */
    public static final long UPDATED_NEVER = -2;

    /**
     * Special value for SyncColumns indicating that the last update time is
     * unknown, usually when inserted from a local file source.
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
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single
         * note.
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

        public final static String[] APP_PROJECTION = new String[] {
                Apps._ID, Apps.ORIGIN, Apps.MANIFEST, Apps.MANIFEST_URL, Apps.INSTALL_DATA,
                Apps.INSTALL_ORIGIN, Apps.INSTALL_TIME, Apps.MODIFIED_DATE, Apps.STATUS
        };

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
                app.put("manifest_url", cur.getString(cur.getColumnIndex(Apps.MANIFEST_URL)));
                app.put("install_data", cur.getString(cur.getColumnIndex(Apps.INSTALL_DATA)));
                app.put("install_origin", cur.getString(cur.getColumnIndex(Apps.INSTALL_ORIGIN)));
                app.put("install_time",
                        Long.valueOf(cur.getLong(cur.getColumnIndex(Apps.INSTALL_TIME)) / 1000));

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

        public static ContentValues toContentValues(Context ctx, final JSONObject app, Boolean install) {

            String origin = app.optString("origin");
            JSONObject manifest = app.optJSONObject("manifest");

            if (origin == null) {
                Log.w(TAG, "Origin was null");
                return null;
            }

            ContentValues values = new ContentValues();
            
            Bitmap bitmap = null;

            if (manifest != null) {
                values.put(Apps.NAME, manifest.optString("name"));
                values.put(Apps.DESCRIPTION, manifest.optString("description"));

                bitmap = Apps.fetchIconByApp(origin, manifest);

                if (bitmap != null) {
                    values.put(Apps.ICON, ImageFactory.bitmapToBytes(bitmap));
                }

                values.put(Apps.MANIFEST_URL, app.optString("manifest_url"));
                values.put(Apps.MANIFEST, manifest.toString());
            }

            values.put(Apps.ORIGIN, origin);

            long installTime = app.optLong("install_time");
            if (installTime < 1) {
                installTime = Long.valueOf(System.currentTimeMillis() / 1000);
            }
            values.put(Apps.INSTALL_TIME, installTime * 1000);

            if (app.optBoolean("deleted")) {
                values.put(Apps.STATUS, STATUS_ENUM.DELETED.ordinal());
            } else {
                values.put(Apps.STATUS, STATUS_ENUM.OK.ordinal());
            }

            JSONObject installData = null;
            try {
                installData = new JSONObject(app.optString("install_data"));
            } catch (Exception e) {
            }

            if (installData != null) {
                values.put(Apps.INSTALL_DATA, installData.toString());
                if (installData.has("receipt")) {
                    values.put(Apps.INSTALL_RECEIPT, installData.optString("receipt"));
                }
            } else {
                values.put(Apps.INSTALL_DATA, new JSONObject().toString());
            }
            
            if (manifest != null && install) {
                
                String launchUri = origin;
                if (manifest.has("launch_path")) {
                    launchUri += manifest.optString("launch_path");
                }

                Intent shortcutIntent = new Intent(ctx, AppActivity.class);
                shortcutIntent.setAction(AppActivity.ACTION_WEBAPP);
                shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                shortcutIntent.putExtra("uri", launchUri);

                final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

                if (prefs.getBoolean("install_shortcut", true)) {
                    Log.d(TAG, "Install creates shortcut");

                    Intent intent = new Intent();
                    intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                    intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
                    intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, manifest.optString("name", "No Name"));
                    if (bitmap != null) {
                        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
                    }
                    // Disallow the creation of duplicate
                    // shortcuts (i.e. same
                    // url, same title, but different screen
                    // position).
                    intent.putExtra("duplicate", false);

                    ctx.sendBroadcast(intent);
                }
            }

            return values;
        }

        public static Cursor findAppByOrigin(Context ctx, String origin) {
            return findAppByOrigin(ctx, origin, false);
        }

        public static Cursor findAppByOrigin(Context ctx, String origin, Boolean installOrigin) {

            String field = Apps.ORIGIN;
            if (installOrigin) {
                field = Apps.INSTALL_ORIGIN;
            }

            Cursor cur = ctx.getContentResolver().query(Apps.CONTENT_URI, Apps.APP_PROJECTION,
                    field + " = ?", new String[] {
                        origin
                    }, Apps.DEFAULT_SORT_ORDER);

            if (cur.moveToFirst() == false) {
                cur.close();
                return null;
            }

            return cur;
        }

        public static Bitmap fetchIconByApp(String origin, JSONObject manifest) {

            JSONObject icons = manifest.optJSONObject("icons");

            if (icons == null || icons.length() == 0) {
                return null;
            }

            JSONArray sizes = icons.names();

            List<Integer> sizesSort = new ArrayList<Integer>();
            for (int i = 0, l = sizes.length(); i < l; i++) {
                sizesSort.add(sizes.optInt(i));
            }
            String max = Collections.max(sizesSort).toString();

            String iconUrl = origin + icons.optString(max);

            Log.d(TAG, "Fetching icon " + max + ": " + iconUrl);

            return ImageFactory.getResizedImage(iconUrl, 72, 72);
        }

    }

}
