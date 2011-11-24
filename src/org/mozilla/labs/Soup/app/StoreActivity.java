package org.mozilla.labs.Soup.app;

import java.net.URI;
import java.net.URISyntaxException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class StoreActivity extends SoupActivity {

	private static final String TAG = "StoreActivity";

	protected void onResolveIntent() {

		Log.d(TAG, "onResolveIntent");

		final Intent intent = getIntent();

		// if (Intent.ACTION_VIEW.equals(action)) {
		// Toast.makeText(StoreActivity.this, "TODO: Open app store detail view", Toast.LENGTH_SHORT).show();
		// }

		// Init web views

		// Allow overriding the Store with an new landing page
		String uri = intent.getStringExtra("uri");

		if (TextUtils.isEmpty(uri)) {
			SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

			uri = settings.getString("dev_store", "https://apps-preview.mozilla.org");
		}

		if (!onCreateLayout()) { // Webkit existed
			
			// Update when a new store URL is set
			try {
				String currentUri = appView.getUrl();

				if (currentUri != null && new URI(currentUri).getAuthority().equals(new URI(uri).getAuthority())) {
					return;
				}
			} catch (URISyntaxException e) {
			}
		}

		// only set URL for fresh views
		super.loadUrl(uri);

	}
}
