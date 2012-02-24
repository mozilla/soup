
package org.mozilla.labs.Soup.app;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;

import org.mozilla.labs.Soup.R;
import org.mozilla.labs.Soup.service.DetachableResultReceiver;
import org.mozilla.labs.Soup.service.SyncService;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

public class SoupApplication extends Application {

    private static final String TAG = "SoupApplication";

    public SyncManager syncManager;

    protected ArrayList<SoupActivity> activities = new ArrayList<SoupActivity>();

    protected HashMap<String, Bundle> bundles = new HashMap<String, Bundle>();

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        /*
         * This populates the default values from the preferences XML file.
         */
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Boolean welcomed = prefs.getBoolean("welcomed", false);

        if (!welcomed) {

            // prefs.edit().putBoolean("welcomed", true).commit();
            //
            // Intent shortcutIntent =
            // LiveFolderActivity.createLiveFolder(this);
            //
            // Intent intent = new Intent();
            // intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            // intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            //
            // intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
            // getString(R.string.app_name_live));
            // intent.putExtra(Intent.EXTRA_SHORTCUT_ICON,
            // Intent.ShortcutIconResource.fromContext(this,
            // R.drawable.ic_launcher_rt));

            // intent.putExtra("duplicate", false);
            //
            // sendBroadcast(intent);
        }

        syncManager = new SyncManager();
    }

    public boolean isDebuggable() {
        return (0 != (getApplicationInfo().flags &= ApplicationInfo.FLAG_DEBUGGABLE));
    }

    @Override
    public void onTerminate() {
        Log.d(TAG, "TERMINATED");

        syncManager.terminate();
    }

    public void saveInstance(Intent intent, Bundle instance) {
        bundles.put(intent.getDataString(), instance);
    }

    public Bundle restoreInstance(Intent intent) {
        return bundles.get(intent.getDataString());
    }

    public void registerActivity(SoupActivity activity) {

        if (activities.contains(activity)) {
            return;
        }

        activities.add(activity);

        /*
         * Log.d(TAG, "New activity"); for (SoupActivity act : activities) {
         * Log.d(TAG, "registeredActivity: " + act.getTitle()); } if
         * (activity.getClass() == AppActivity.class) { Intent intent =
         * activity.getIntent(); for (SoupActivity value : activities) { Intent
         * valueIntent = value.getIntent(); if (valueIntent == null ||
         * !value.getIntent().getData().equals(intent.getData())) { break; }
         * Log.d(TAG, "Found instance of Intent " + intent); activity.finish();
         * // Matched previous app intent.setFlags(intent.getFlags() &
         * Intent.FLAG_ACTIVITY_MULTIPLE_TASK); startActivity(intent); return; }
         * Log.d(TAG, "Added Intent to list " + intent); apps.add(intent); }
         */

    }

    public void unregisterActivity(SoupActivity activity) {
        activities.remove(activity);
    }

    public void finishAllActivities(boolean clear) {
        Log.d(TAG, "Finishing " + activities.size() + " activities");

        for (int i = 0, l = activities.size(); i < l; i++) {
            SoupActivity activity = activities.get(i);

            if (clear) {
                activity.clearCache();
            }

            activity.finish();
        }
    }

    public class SyncManager extends Observable implements DetachableResultReceiver.Receiver {

        private DetachableResultReceiver receiver;

        private boolean syncRunning;

        public SyncManager() {
            receiver = new DetachableResultReceiver(new Handler());
            receiver.setReceiver(this);
        }

        public void terminate() {
            if (syncRunning) {
                final Intent intent = new Intent(SoupApplication.this, SyncService.class);
                stopService(intent);
            }

        }

        public void startSync() {
            if (syncRunning)
                return;

            syncRunning = true;

            setChanged();
            notifyObservers(0);

            final Intent intent = new Intent(SoupApplication.this, SyncService.class);
            intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, receiver);
            startService(intent);
        }

        public void onReceiveResult(int resultCode, Bundle resultData) {
            // TODO: Review IBinder onBind so every activity can bind the
            // service?
            // @see RemoteService.java from android-8

            switch (resultCode) {
                case SyncService.STATUS_RUNNING:
                    // show progress
                    break;
                case SyncService.STATUS_FINISHED:
                    syncRunning = false;

                    int installed = resultData.getInt(SyncService.EXTRA_STATUS_INSTALLED);
                    int updated = resultData.getInt(SyncService.EXTRA_STATUS_UPDATED);

                    setChanged();
                    notifyObservers(installed + updated);

                    break;
                case SyncService.STATUS_ERROR:

                    // handle the error;
                    break;
            }
        }

    }

    public void triggerSync() {
        syncManager.startSync();
    }

    public void clearData(SoupActivity activity) {

        activity.clearCache();

        Log.d(TAG, "Deleting apps.db: " + deleteDatabase("apps.db"));

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Map<String, ?> previous = prefs.getAll();

        prefs.edit().clear().commit();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

        String[] devPrefs = {
                "dev_store", "dev_identity", "dev_sync"
        };
        for (String pref : devPrefs) {
            prefs.edit().putString(pref, (String)previous.get(pref));
        }

        deleteCache();

        Toast.makeText(activity, "Restarting application.", Toast.LENGTH_SHORT);

        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();
        cookieManager.removeSessionCookie();

        finishAllActivities(true);

        syncManager.terminate();

        Intent i = getBaseContext().getPackageManager().getLaunchIntentForPackage(
                getBaseContext().getPackageName());

        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private void deleteCache() {
        try {
            File dir = getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not delete cache", e);
        }

        try {
            File dir = getExternalCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not delete external cache", e);
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String file : children) {
                if (!deleteDir(new File(dir, file))) {
                    return false;
                }
            }
        }

        return dir.delete();
    }

}
