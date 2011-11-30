package org.mozilla.labs.Soup.service;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mozilla.labs.Soup.app.AppActivity;
import org.mozilla.labs.Soup.provider.AppsContract.Apps;

import android.R;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;

public class SyncService extends IntentService {

	private static final String TAG = "SyncService";

	public static final String EXTRA_STATUS_RECEIVER = "org.mozilla.labs.soup.extra.STATUS_RECEIVER";

	public static final int STATUS_RUNNING = 0x1;
	public static final int STATUS_ERROR = 0x2;
	public static final int STATUS_FINISHED = 0x3;
	
	public static final String EXTRA_STATUS_INSTALLED = "org.mozilla.labs.soup.extra.STATUS_INSTALLED";
	public static final String EXTRA_STATUS_UPDATED = "org.mozilla.labs.soup.extra.STATUS_UPDATED";
	public static final String EXTRA_STATUS_UPLOADED = "org.mozilla.labs.soup.extra.STATUS_UPLOADED";

	private ContentResolver resolver;

	private NotificationManager mNM;
	private int NOTIFY_ID = 1001;

	public SyncService() {
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		resolver = getContentResolver();
		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent " + intent);

		showNotification();

		final ResultReceiver receiver = intent
				.getParcelableExtra(EXTRA_STATUS_RECEIVER);
		if (receiver != null)
			receiver.send(STATUS_RUNNING, Bundle.EMPTY);

		final Context ctx = this;
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		final long localSince = prefs.getLong("sync_since", 0);

		Log.d(TAG, "Sync since " + localSince);
		
		int uploaded = 0;
		int updated = 0;
		int installed = 0;
		
		try {

			/**
			 * Local list
			 */

			Cursor cur = resolver.query(Apps.CONTENT_URI, Apps.APP_PROJECTION, null,
					null, Apps.DEFAULT_SORT_ORDER);

			cur.moveToFirst();

			HashMap<String, JSONObject> localList = new HashMap<String, JSONObject>();

			while (cur.isAfterLast() == false) {
				JSONObject app = Apps.toJSONObject(cur, true);

				if (app != null) {
					localList.put(app.optString("origin"), app);
				}

				cur.moveToNext();
			}

			cur.close();

			/**
			 * Server list
			 */
			
			if (!SoupClient.authorize(this)) {
				new Exception("User not authorized.");
			}

			JSONObject response = SoupClient.getAllApps(this, localSince);

			Log.d(TAG, "List: " + response);

			if (response == null) {
				throw new Exception("Empty server response");
			}

			JSONArray responseList = response.optJSONArray("applications");
			if (responseList == null) {
				responseList = new JSONArray();
			}
			
			// TODO: Handle incomplete
			
			long until = response.optLong("until");

			HashMap<String, JSONObject> serverList = new HashMap<String, JSONObject>();

			for (int i = 0, l = responseList.length(); i < l; i++) {
				JSONObject app = responseList.getJSONObject(i);

				serverList.put(app.optString("origin"), app);
			}

			/**
			 * Sync the 2 lists
			 */

			HashMap<String, JSONObject> toServerList = new HashMap<String, JSONObject>();
			HashMap<String, JSONObject> toLocalList = new HashMap<String, JSONObject>();

			for (HashMap.Entry<String, JSONObject> entry : serverList.entrySet()) {
				String origin = entry.getKey();
				JSONObject serverValue = entry.getValue();
				
				if (localList.containsKey(origin)) {
					JSONObject localValue = localList.get(origin);
					
					long localDate = localValue.optLong("last_modified");
					long serverDate = serverValue.optLong("last_modified");

					if (localDate > serverDate) {
						Log.d(TAG, "to server: " + origin + ", " + localDate + " > " + serverDate);
						toServerList.put(origin, localValue);
					} else if (localDate < serverDate) {
						Log.d(TAG, "to local: " + origin + ", " + localDate + " < " + serverDate);
						toLocalList.put(origin, serverValue);
					}
				} else {
					Log.d(TAG, "to local: " + origin);
					toLocalList.put(origin, serverValue);
				}
			}
			
			for (HashMap.Entry<String, JSONObject> entry : localList.entrySet()) {
				String origin = entry.getKey();
				JSONObject localValue = entry.getValue();
				
				long localDate = localValue.optLong("last_modified");
				
				if (!serverList.containsKey(origin) && localDate > localSince) {
					Log.d(TAG, "to server: " + origin + ", " + localDate + " > " + localSince);
					toServerList.put(origin, localValue);
				}
			}
			
			/**
			 * Iterate sync result
			 */
			
			// Update server values
			
			JSONArray serverUpdates = new JSONArray();
			
			for (HashMap.Entry<String, JSONObject> entry : toServerList.entrySet()) {
				JSONObject localValue = entry.getValue();
				
				serverUpdates.put(localValue);
				uploaded++;
			}
			
			long updatedUntil = until;
			
			if (serverUpdates.length() > 0) {
				updatedUntil = SoupClient.updateApps(ctx, serverUpdates, until);
				
				if (updatedUntil < 1) {
					new Exception("Update failed for " + serverUpdates);
				}
			}
			
			for (HashMap.Entry<String, JSONObject> entry : toServerList.entrySet()) {
				String origin = entry.getKey();
				
				Cursor existing = Apps.findAppByOrigin(this, origin);
				
				ContentValues values = new ContentValues();
				values.put(Apps.MODIFIED_DATE, updatedUntil);
				
				Uri appUri = Uri.withAppendedPath(Apps.CONTENT_URI, existing.getString(existing.getColumnIndex(Apps._ID)));
				getContentResolver().update(appUri, values, null, null);
			}
			
			// Update local values
			
			for (HashMap.Entry<String, JSONObject> entry : toLocalList.entrySet()) {
				String origin = entry.getKey();
				JSONObject serverValue = entry.getValue();
				
				Cursor existing = Apps.findAppByOrigin(this, origin);
				
				ContentValues values = Apps.toContentValues(serverValue);
				
				// TODO: Set better updatedUntil (get latest date from sync server)
				values.put(Apps.MODIFIED_DATE, updatedUntil);
				
				if (existing == null) {
					installed++;
					getContentResolver().insert(Apps.CONTENT_URI, values);
				} else {
					updated++;
					Uri appUri = Uri.withAppendedPath(Apps.CONTENT_URI, existing.getString(existing.getColumnIndex(Apps._ID)));
					
					getContentResolver().update(appUri, values, null, null);
				}
				
			}
			
			prefs.edit().putLong("sync_since", updatedUntil).commit();
			
			Log.d(TAG, "Sync until " + updatedUntil);

		} catch (Exception e) {
			Log.e(TAG, "Sync unsuccessful", e);

			if (receiver != null) {
				// Pass back error to surface listener
				final Bundle bundle = new Bundle();
				bundle.putString(Intent.EXTRA_TEXT, e.toString());
				receiver.send(STATUS_ERROR, bundle);
			}
		}

		hideNotification();

		// Announce success to any surface listener
		if (receiver != null) {
			final Bundle bundle = new Bundle();
			bundle.putInt(EXTRA_STATUS_UPLOADED, uploaded);
			bundle.putInt(EXTRA_STATUS_INSTALLED, installed);
			bundle.putInt(EXTRA_STATUS_UPDATED, updated);
			
			receiver.send(STATUS_FINISHED, bundle);
		}

		stopSelf();
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		if (mNM == null) {
			return;
		}
		
		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.stat_notify_sync,
				"Soup is syncing", System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, AppActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification
				.setLatestEventInfo(this, "Soup Apps", "Synchronizing updates", contentIntent);

		// Send the notification.
		// We use a string id because it is a unique number. We use it later to cancel.
		mNM.notify(NOTIFY_ID, notification);
	}

	private void hideNotification() {
		if (mNM == null) {
			return;
		}
		
		mNM.cancel(NOTIFY_ID);
	}

}
