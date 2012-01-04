
package org.mozilla.labs.Soup.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Observable;

import org.mozilla.labs.Soup.R;
import org.mozilla.labs.Soup.service.DetachableResultReceiver;
import org.mozilla.labs.Soup.service.SyncService;

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
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

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        /*
         * This populates the default values from the preferences XML file.
         */
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        syncManager = new SyncManager();
    }

    @Override
    public void onTerminate() {
        Log.d(TAG, "TERMINATED");

        syncManager.terminate();
    }

    public void registerActivity(SoupActivity activity) {
        if (!activities.contains(activity)) {
            activities.add(activity);
        }
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
        prefs.edit().clear().commit();

        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

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
