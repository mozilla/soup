
package org.mozilla.labs.Soup.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Observable;
import java.util.Observer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.labs.Soup.app.AppActivity;
import org.mozilla.labs.Soup.app.SoupApplication;
import org.mozilla.labs.Soup.provider.AppsContract.Apps;

import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class MozAppsMgmtPlugin extends Plugin implements Observer {

    private static final String TAG = "MozAppsMgmtPlugin";

    private HashMap<Long, String> watchList = new HashMap<Long, String>();

    private JSONObject allApps = new JSONObject();

    private boolean watchAdded = false;

    private long watchUid = 1;

    /*
     * (non-Javadoc)
     * @see com.phonegap.api.Plugin#execute(java.lang.String,
     * org.json.JSONArray, java.lang.String)
     */
    @Override
    public PluginResult execute(String action, JSONArray data, String callbackId) {
        Log.d(TAG, "Called " + action + ": " + data);

        try {

            if (action.equals("getAll")) {

                return getAll(false);

            } else if (action.equals("launch")) {

                return launch(data.optString(0));

            } else if (action.equals("watchUpdates")) {

                return watchUpdates(callbackId);

            } else if (action.equals("clearWatch")) {

                return clearWatch(callbackId, data.optLong(0));

            } else if (action.equals("sync")) {

                return sync(callbackId);

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
        if (action.equals("watchUpdates") || action.equals("sync")) {
            return true;
        }

        return false;
    }

    private PluginResult launch(String query) {

        String[] projection = new String[] {
                Apps.ORIGIN, Apps.MANIFEST, Apps.INSTALL_DATA, Apps.INSTALL_ORIGIN,
                Apps.INSTALL_TIME
        };

        Cursor cur = ctx.managedQuery(Apps.CONTENT_URI, projection, Apps.ORIGIN + " = ?",
                new String[] {
                    query
                }, Apps.DEFAULT_SORT_ORDER);

        if (cur.moveToFirst() == false) {
            Log.w(TAG, "Could not find " + query);

            return new PluginResult(Status.ERROR);
        }

        JSONObject manifest;
        try {
            manifest = new JSONObject(cur.getString(cur.getColumnIndex(Apps.MANIFEST)));
        } catch (JSONException e) {
            return new PluginResult(Status.ERROR);
        }

        String origin = cur.getString(cur.getColumnIndex(Apps.ORIGIN));
        String uri = origin + manifest.optString("launch_path");
        final String name = manifest.optString("name");

        final Intent shortcutIntent = new Intent(this.ctx, AppActivity.class);
        shortcutIntent.setAction(AppActivity.ACTION_WEBAPP);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.putExtra("uri", uri);

        ctx.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(ctx, "Launching " + name, Toast.LENGTH_SHORT).show();

                ctx.startActivity(shortcutIntent);
            }
        });

        return new PluginResult(Status.OK);
    }

    private PluginResult getAll(boolean update) {

        ArrayList<String> processed = new ArrayList<String>();

        JSONObject installed = new JSONObject();

        if (update) {
            Log.d(TAG, "getAll update with memory " + allApps.length());
        }

        Cursor cur = ctx.getContentResolver().query(Apps.CONTENT_URI, Apps.APP_PROJECTION, null,
                null, Apps.DEFAULT_SORT_ORDER);

        cur.moveToFirst();

        JSONArray list = new JSONArray();

        while (cur.isAfterLast() == false) {
            JSONObject app = Apps.toJSONObject(cur);

            if (app != null) {
                String origin = app.optString("origin");

                if (!update || !allApps.has(origin)) {
                    try {
                        allApps.put(origin, app);
                        installed.put(origin, app);

                        if (update) {
                            Log.d(TAG, "getAll update added " + origin);
                        }
                    } catch (JSONException e) {
                    }
                }

                processed.add(origin);

                list.put(app);
            }

            cur.moveToNext();
        }

        cur.close();

        if (update) {
            
            // dirty object clone
            JSONObject uninstalled = new JSONObject();
            try {
                uninstalled = new JSONObject(allApps.toString());
            } catch (JSONException e1) {
            }

            Iterator<String> iterator = processed.iterator();

            while (iterator.hasNext()) {
                uninstalled.remove(iterator.next());
            }

            JSONArray installedList = new JSONArray();
            JSONArray uninstalledList = new JSONArray();
            try {
                if (installed.length() > 0) {
                    installedList = installed.toJSONArray(installed.names());
                }

                if (uninstalled.length() > 0) {
                    uninstalledList = uninstalled.toJSONArray(uninstalled.names());
                }
            } catch (JSONException e) {
            }

            JSONArray args = new JSONArray().put(installedList).put(uninstalledList);

            if (installedList.length() > 0 || uninstalledList.length() > 0) {
                Log.d(TAG, "watchUpdate: " + args + ", all apps is now " + allApps.length());

                for (HashMap.Entry<Long, String> entry : watchList.entrySet()) {
                    success(new PluginResult(Status.OK, args), entry.getValue());
                }
            }
            return new PluginResult(Status.NO_RESULT);
        } else {
            // TODO: Better place to trigger updates
            ((SoupApplication)ctx.getApplication()).triggerSync();

            return new PluginResult(Status.OK, list);
        }
    }

    private PluginResult watchUpdates(String callbackId) {

        if (!watchAdded) {
            watchAdded = true;
            ((SoupApplication)ctx.getApplication()).syncManager.addObserver(this);
        }

        long watchId = watchUid++;

        watchList.put(watchId, callbackId);

        PluginResult result = new PluginResult(Status.NO_RESULT, watchId);
        result.setKeepCallback(true);
        return result;
    }

    private PluginResult clearWatch(String callbackId, long watchId) {
        if (watchList.containsKey(watchId)) {
            watchList.remove(watchId);
        }

        return new PluginResult(Status.NO_RESULT);
    }

    private PluginResult sync(String callbackId) {
        ((SoupApplication)ctx.getApplication()).triggerSync();

        return new PluginResult(Status.NO_RESULT);
    }

    public void update(Observable app, Object updated) {
        Integer updatedInt = (Integer)updated;
        if (updatedInt > 1) {
            Log.d(TAG, "update returned " + updatedInt);

            getAll(true);
        }

    }

}
