package org.mozilla.labs.Soup.app;

import java.io.File;
import java.util.Observable;

import org.mozilla.labs.Soup.R;
import org.mozilla.labs.Soup.service.DetachableResultReceiver;
import org.mozilla.labs.Soup.service.SyncService;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class SoupApplication extends Application {

	private static final String TAG = "SoupApplication";

	public SyncManager syncManager;

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
		syncManager.terminate();
	}

	public class SyncManager extends Observable implements
			DetachableResultReceiver.Receiver {

		private DetachableResultReceiver receiver;
		private boolean syncRunning;

		public SyncManager() {
			receiver = new DetachableResultReceiver(new Handler());
			receiver.setReceiver(this);
		}

		public void terminate() {
			if (syncRunning) {
				final Intent intent = new Intent(SoupApplication.this,
						SyncService.class);
				stopService(intent);
			}

		}

		public void startSync() {
			if (syncRunning)
				return;

			syncRunning = true;

			setChanged();
			notifyObservers(0);

			final Intent intent = new Intent(SoupApplication.this,
					SyncService.class);
			intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, receiver);
			startService(intent);
		}

		public void onReceiveResult(int resultCode, Bundle resultData) {
			// TODO: Review IBinder onBind so every activity can bind the service?
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

	
	public void clearData(Activity activity) {
		// TODO: Find a clean way to reset

		// Buggy:
//		Log.d(TAG, "Deleting webview.db " + deleteDatabase("webview.db"));
//		Log.d(TAG, "Deleting webviewCache.db "
//				+ deleteDatabase("webviewCache.db"));
		
		Log.d(TAG, "Deleting apps.db " + deleteDatabase("apps.db"));

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		prefs.edit().clear().commit();

		PreferenceManager.setDefaultValues(this, R.xml.preferences, true);

		deleteCache();
		
		Toast.makeText(activity, "Personal data cleared!", Toast.LENGTH_SHORT);

		Intent intent = new Intent(this, LauncherActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		activity.startActivity(intent);
		
		activity.finish();
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
