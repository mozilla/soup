package org.mozilla.labs.Soup.app;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class AppActivity extends SoupActivity {

	private static final String TAG = "AppActivity";

	private String currentUri;

	protected void onResolveIntent() {

		final Intent intent = getIntent();
		final String action = intent.getAction();

		Log.d(TAG, "onResolveIntent " + intent);

		if (ACTION_WEBAPP.equals(action)) {

            String uri = intent.getDataString();

            if (TextUtils.isEmpty(uri)) {
                Toast.makeText(this, "App not found", Toast.LENGTH_SHORT).show();

				Intent redirect = new Intent(this, LauncherActivity.class);
				startActivity(redirect);
				return;
			}

			boolean didInitialize = onCreateLayout();

			if (!didInitialize) {
				if (uri.equals(currentUri)) {
					Log.d(TAG, "onResolveIntent skipped " + uri);
					return;
				}

                appView.clearView();

				// Hide old app
                // appView.setVisibility(View.VISIBLE);
			}


			Log.d(TAG, "onResolveIntent loading " + uri);

			currentUri = uri;
			super.loadUrl(uri);

            clearHistory();
		}

	}

	public void onDestroy() {
		super.onDestroy();

        // Toast.makeText(this, "App closed", Toast.LENGTH_SHORT).show();
	}

}
