package org.mozilla.labs.Soup.app;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class AppActivity extends SoupActivity {

	private static final String TAG = "AppActivity";

	private String currentUri;

	protected void onResolveIntent() {

		final Intent intent = getIntent();
		final String action = intent.getAction();

		Log.d(TAG, "onResolveIntent " + intent);

		if (ACTION_WEBAPP.equals(action)) {
			if (!intent.hasExtra("uri")) {
				Toast.makeText(this, "Could not find app", Toast.LENGTH_SHORT).show();

				Intent redirect = new Intent(this, LauncherActivity.class);
				startActivity(redirect);
				return;
			}

			final String uri = intent.getStringExtra("uri");

			boolean didInitialize = onCreateLayout();

			if (!didInitialize) {
				if (uri.equals(currentUri)) {
					Log.d(TAG, "onResolveIntent skipped " + uri);
					return;
				}

				// Hide old app
				appView.setVisibility(View.INVISIBLE);
			}

			Log.d(TAG, "onResolveIntent loading " + uri);

			currentUri = uri;
			super.loadUrl(uri);
		}

	}

	public void onDestroy() {
		super.onDestroy();

		Toast.makeText(this, "Closing app", Toast.LENGTH_SHORT).show();
	}

}
