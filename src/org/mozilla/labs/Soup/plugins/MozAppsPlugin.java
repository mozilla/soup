
package org.mozilla.labs.Soup.plugins;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.labs.Soup.R;
import org.mozilla.labs.Soup.app.AppActivity;
import org.mozilla.labs.Soup.app.SoupActivity;
import org.mozilla.labs.Soup.app.SoupApplication;
import org.mozilla.labs.Soup.http.HttpFactory;
import org.mozilla.labs.Soup.http.ImageFactory;
import org.mozilla.labs.Soup.provider.AppsContract.Apps;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class MozAppsPlugin extends Plugin {

    private static final String TAG = "MozAppsPlugin";

    /*
     * (non-Javadoc)
     * @see com.phonegap.api.Plugin#execute(java.lang.String,
     * org.json.JSONArray, java.lang.String)
     */
    @Override
    public PluginResult execute(String action, JSONArray data, String callback) {
        Log.d(TAG, "Called " + action + ": " + data);

        Uri originUri = Uri.parse(data.optString(0, webView.getUrl()));
        String origin = originUri.getScheme() + "://" + originUri.getAuthority();

        Cursor cur = null;

        try {

            if (action.equals("install")) {

                cur = Apps.findAppByOrigin(ctx, origin, false);

                int installed = 0;

                if (cur != null) {

                    installed = cur.getInt(cur.getColumnIndex(Apps._ID));
                    cur.close();

                }

                return install(callback, originUri.toString(), data.optJSONObject(1), origin,
                        installed);

            } else if (action.equals("getSelf")) {

                cur = Apps.findAppByOrigin(ctx, origin, false);

                if (cur != null) {
                    JSONObject app = Apps.toJSONObject(cur);

                    cur.close();

                    if (app != null) {
                        return new PluginResult(Status.OK, app);
                    }

                    return new PluginResult(Status.ERROR);
                }

                return new PluginResult(Status.OK);

            } else if (action.equals("getInstalled")) {

                cur = Apps.findAppByOrigin(ctx, origin, true);
                JSONArray list = new JSONArray();

                if (cur != null) {

                    while (cur.isAfterLast() == false) {
                        JSONObject app = Apps.toJSONObject(cur);

                        if (app != null) {
                            list.put(app);
                        }

                        cur.moveToNext();
                    }

                    cur.close();
                }

                return new PluginResult(Status.OK, list);
            }

        } catch (Exception e) {
            Log.w(TAG, action + " failed", e);
            return new PluginResult(Status.JSON_EXCEPTION);
        }

        return new PluginResult(Status.INVALID_ACTION);
    }

    /**
     * Identifies if action to be executed returns a value and should be run
     * synchronously.
     * 
     * @param action The action to execute
     * @return T=returns value
     */
    public boolean isSynch(String action) {
        if (action.equals("install")) {
            return true;
        }

        return false;
    }

    public synchronized PluginResult install(final String callbackId, final String manifestUri,
            final JSONObject install_data, final String origin, final int installed)
            throws Exception {

        ctx.runOnUiThread(new Runnable() {

            public void run() {

                ctx.showDialog(SoupActivity.DIALOG_LOADING_ID);

                final Uri installOriginUri = Uri.parse(webView.getUrl());

                new Thread(new Runnable() {
                    public void run() {

                        Looper.prepare();

                        // TODO: More error codes (JSON vs IO)
                        JSONObject manifest = HttpFactory.getManifest(ctx, manifestUri);

                        Log.d(TAG, "Parsed manifest: " + manifest);

                        if (manifest == null) {
                            ctx.dismissDialog(SoupActivity.DIALOG_LOADING_ID);

                            JSONObject errorEvent = null;
                            try {
                                errorEvent = new JSONObject().put("message", "NETWORK_ERROR");
                            } catch (JSONException e) {
                            }

                            Toast.makeText(ctx,
                                    "Could not reach " + Uri.parse(manifestUri).getHost(),
                                    Toast.LENGTH_SHORT).show();

                            error(new PluginResult(Status.ERROR, errorEvent), callbackId);

                            Looper.loop();

                            return;
                        }

                        final ContentValues values = new ContentValues();

                        final String name = manifest.optString("name", "No Name");
                        final String description = manifest.optString("description", "");
                        values.put(Apps.NAME, name);
                        values.put(Apps.DESCRIPTION, description);

                        final Bitmap bitmap = Apps.fetchIconByApp(origin, manifest);

                        if (bitmap != null) {
                            values.put(Apps.ICON, ImageFactory.bitmapToBytes(bitmap));
                        } else {
                            Log.w(TAG, "Could not load icon");
                        }

                        values.put(Apps.ORIGIN, origin);
                        values.put(Apps.MANIFEST_URL, manifestUri);
                        values.put(Apps.MANIFEST, manifest.toString());

                        // TODO: Fails for iframes
                        String installOrigin = installOriginUri.getScheme() + "://"
                                + installOriginUri.getAuthority();

                        values.put(Apps.INSTALL_ORIGIN, installOrigin);

                        if (install_data != null) {
                            values.put(Apps.INSTALL_DATA, install_data.toString());

                            if (install_data.has("receipt")) {
                                values.put(Apps.INSTALL_RECEIPT, install_data.optString("receipt"));
                            }
                        }

                        final String launchUri = origin + manifest.optString("launch_path", "/");

                        SharedPreferences prefs = PreferenceManager
                                .getDefaultSharedPreferences(ctx);

                        final boolean[] appSettings = new boolean[] {
                                prefs.getBoolean("install_shortcut", true),
                                prefs.getBoolean("install_launch", true)
                        };

                        ctx.dismissDialog(SoupActivity.DIALOG_LOADING_ID);

                        AlertDialog.Builder installDlg = new AlertDialog.Builder(ctx);

                        String title = "Install " + name + "?";

                        if (installed != 0) {
                            title = "Update " + name + "?";
                        }

                        installDlg
                                .setTitle(title)
                                .setMultiChoiceItems(R.array.install_dialog_array, appSettings,
                                        new DialogInterface.OnMultiChoiceClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                    int whichButton, boolean isChecked) {
                                                appSettings[whichButton] = isChecked;
                                            }
                                        }).setCancelable(true);

                        if (bitmap != null) {
                            installDlg.setIcon(new BitmapDrawable(bitmap));
                        }

                        installDlg.setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        JSONObject errorEvent = null;
                                        try {
                                            errorEvent = new JSONObject().put("message", "DENIED")
                                                    .put("message", "denied");
                                        } catch (JSONException e) {
                                        }
                                        error(new PluginResult(Status.ERROR, errorEvent),
                                                callbackId);

                                    }
                                }).setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                        Uri uri = null;

                                        if (installed != 0) {
                                            values.put(Apps.STATUS, Apps.STATUS_ENUM.OK.ordinal());
                                        }

                                        try {
                                            if (installed != 0) {

                                                uri = ContentUris.withAppendedId(Apps.CONTENT_URI,
                                                        installed);
                                                ctx.getContentResolver().update(uri, values, null,
                                                        null);

                                            } else {

                                                uri = ctx.getContentResolver().insert(
                                                        Apps.CONTENT_URI, values);

                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "Installation failed for " + values, e);

                                            Toast.makeText(ctx,
                                                    "Installation flopped. Please try again!",
                                                    Toast.LENGTH_SHORT);
                                        }

                                        if (uri == null) {
                                            JSONObject errorEvent = null;
                                            try {
                                                errorEvent = new JSONObject().put("message",
                                                        "DENIED");
                                            } catch (JSONException e) {
                                            }
                                            error(new PluginResult(Status.ERROR, errorEvent),
                                                    callbackId);

                                            return;
                                        }

                                        success(new PluginResult(Status.OK, 0), callbackId);

                                        Intent shortcutIntent = new Intent(ctx, AppActivity.class);
                                        shortcutIntent.setAction(AppActivity.ACTION_WEBAPP);
                                        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        shortcutIntent.putExtra("uri", launchUri);

                                        // TODO: Move one more place to sync
                                        ((SoupApplication)ctx.getApplication()).triggerSync();

                                        if (appSettings[0]) {
                                            Log.d(TAG, "Install creates shortcut");

                                            Intent intent = new Intent();
                                            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
                                            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,
                                                    shortcutIntent);
                                            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
                                            if (bitmap != null) {
                                                intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
                                            }
                                            // Disallow the creation of
                                            // duplicate
                                            // shortcuts (i.e. same
                                            // url, same title, but different
                                            // screen
                                            // position).
                                            intent.putExtra("duplicate", false);

                                            ctx.sendBroadcast(intent);
                                        }

                                        if (appSettings[1]) {
                                            Log.d(TAG, "Install launches app");

                                            ctx.startActivity(shortcutIntent);
                                        }

                                    }
                                });

                        installDlg.create();
                        installDlg.show();

                        Looper.loop();

                    }
                }).start();
            }
        });

        PluginResult result = new PluginResult(Status.NO_RESULT);
        result.setKeepCallback(true);
        return result;
    }

}
