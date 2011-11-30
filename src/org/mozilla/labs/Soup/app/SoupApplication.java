package org.mozilla.labs.Soup.app;

import java.util.Observable;

import org.mozilla.labs.Soup.R;
import org.mozilla.labs.Soup.service.DetachableResultReceiver;
import org.mozilla.labs.Soup.service.SyncService;

import android.app.Application;
import android.content.Intent;
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
		 * 
		 * TODO: Actually set defaults in preferences.xml
		 */
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

		syncManager = new SyncManager(this);
	}

	@Override
	public void onTerminate() {
	}

	public class SyncManager extends Observable implements
			DetachableResultReceiver.Receiver {

		private Application ctx;

		private DetachableResultReceiver receiver;
		private boolean syncRunning;

		public SyncManager(Application context) {
			ctx = context;

			receiver = new DetachableResultReceiver(new Handler());
			receiver.setReceiver(this);
		}

		public void startSync() {
			if (syncRunning)
				return;

			syncRunning = true;

			setChanged();
			notifyObservers(0);

			final Intent intent = new Intent(Intent.ACTION_SYNC, null, ctx,
					SyncService.class);
			intent.putExtra(SyncService.EXTRA_STATUS_RECEIVER, receiver);
			startService(intent);
		}

		public void onReceiveResult(int resultCode, Bundle resultData) {
			switch (resultCode) {
			case SyncService.STATUS_RUNNING:
				// show progress
				break;
			case SyncService.STATUS_FINISHED:
				syncRunning = false;

				int installed = resultData.getInt(SyncService.EXTRA_STATUS_INSTALLED);
				int updated = resultData.getInt(SyncService.EXTRA_STATUS_UPDATED);
				int uploaded = resultData.getInt(SyncService.EXTRA_STATUS_UPLOADED);

				if (installed > 0) {
					Toast.makeText(ctx, "Installed " + installed + " apps",
							Toast.LENGTH_SHORT).show();
				} else if (updated > 0) {
					Toast.makeText(ctx, "Updated " + updated + " apps",
							Toast.LENGTH_SHORT).show();
				} else if (uploaded > 0) {
					Toast.makeText(ctx, "Uploaded " + uploaded + " app(s)",
							Toast.LENGTH_SHORT).show();
				} else {
					// Toast.makeText(ctx, "Nothing to sync", Toast.LENGTH_SHORT).show();
				}

				setChanged();
				notifyObservers(installed + updated);

				// List results = resultData.getParcelableList("results");
				// do something interesting
				// hide progress
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

}
