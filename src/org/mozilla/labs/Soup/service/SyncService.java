package org.mozilla.labs.Soup.service;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.labs.Soup.app.AppActivity;
import org.mozilla.labs.Soup.provider.AppsContract.Apps;

import android.R;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
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

	private ContentResolver resolver;

	private NotificationManager mNM;
	private int NOTIFY_ID = 1001;

	public SyncService() {
		super(TAG);

		// mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		resolver = getContentResolver();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "onHandleIntent " + intent);

		// showNotification();

		final ResultReceiver receiver = intent
				.getParcelableExtra(EXTRA_STATUS_RECEIVER);
		if (receiver != null)
			receiver.send(STATUS_RUNNING, Bundle.EMPTY);

		final Context ctx = this;
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(ctx);
		final int localSince = prefs.getInt("sync_since", 0);

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

			JSONObject response = SoupClient.getAllApps(this, localSince);

			Log.d(TAG, "List: " + response);

			if (response == null) {
				throw new Exception("Empty server response");
			}

			JSONArray responseList = response.optJSONArray("applications");
			if (responseList == null) {
				responseList = new JSONArray();
			}

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
					if (localValue.optLong("last_modified") > serverValue.optLong("last_modified")) {
						toServerList.put(origin, localValue);
					} else {
						toLocalList.put(origin, serverValue);
					}
				} else {
					toLocalList.put(origin, serverValue);
				}
			}
			
			for (HashMap.Entry<String, JSONObject> entry : localList.entrySet()) {
				String origin = entry.getKey();
				JSONObject localValue = entry.getValue();
				
				if (!serverList.containsKey(origin)) {
					toServerList.put(origin, localValue);
				}
			}
			
			Log.d(TAG, "To Server: " + toServerList.keySet().toArray());
			Log.d(TAG, "To Local: " + toServerList.keySet().toArray());

		} catch (Exception e) {
			Log.e(TAG, "Problem while syncing", e);

			if (receiver != null) {
				// Pass back error to surface listener
				final Bundle bundle = new Bundle();
				bundle.putString(Intent.EXTRA_TEXT, e.toString());
				receiver.send(STATUS_ERROR, bundle);
			}
		}

		// hideNotification();

		// Announce success to any surface listener
		Log.d(TAG, "Sync finished");

		if (receiver != null)
			receiver.send(STATUS_FINISHED, Bundle.EMPTY);

		stopSelf();
	}

	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(R.drawable.stat_notify_sync,
				"Soup is syncing", System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, AppActivity.class), 0);

		// Set the info for the views that show in the notification panel.
		notification
				.setLatestEventInfo(this, "Syncing", "More info", contentIntent);

		// Send the notification.
		// We use a string id because it is a unique number. We use it later to cancel.
		mNM.notify(NOTIFY_ID, notification);
	}

	private void hideNotification() {
		mNM.cancel(NOTIFY_ID);
	}

}
