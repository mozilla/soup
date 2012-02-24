package org.mozilla.labs.Soup.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

public class AppActivity extends SoupActivity {

	private static final String TAG = "AppActivity";

    private String currentUri = null;

    private Intent currentIntent = null;

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

                SoupApplication app = (SoupApplication)getApplication();

                if (currentIntent != null) {
                    Bundle current = new Bundle();

                    // super.onSaveInstanceState(current);
                    appView.saveState(current);

                    Log.d(TAG, "Saving state before restoring! " + currentIntent.getDataString());

                    app.saveInstance(currentIntent, current);
                }


                Bundle state = app.restoreInstance(intent);

                if (state != null) {

                    Log.d(TAG, "Restored state! " + getIntent().getDataString());
                    Log.d(TAG, "State: " + state);

                    appView.clearView();

                    appView.restoreState(state);

                    Log.d(TAG, "Url after new state: " + appView.getUrl());

                    currentUri = uri;
                    clearHistory();

                    return;
                }

                appView.clearView();

				// Hide old app
                // appView.setVisibility(View.VISIBLE);
			}

			Log.d(TAG, "onResolveIntent loading " + uri);

            currentUri = uri;
            currentIntent = getIntent();

			super.loadUrl(uri);

            clearHistory();
		}

	}

    @Override
    public void onSaveInstanceState(Bundle outState) {

        if (currentIntent != getIntent()) {
            return;
        }

        // super.onSaveInstanceState(outState);

        appView.saveState(outState);

        Log.d(TAG, "Saving state! " + getIntent().getDataString());

        ((SoupApplication)getApplication()).saveInstance(getIntent(), outState);
    }

	public void onDestroy() {
		super.onDestroy();

        // Toast.makeText(this, "App closed", Toast.LENGTH_SHORT).show();
	}

}
