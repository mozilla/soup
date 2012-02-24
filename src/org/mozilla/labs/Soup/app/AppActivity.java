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

                    saveState(current);

                    Log.d(TAG, "onResolveIntent saving restore " + currentIntent.getDataString());

                    app.saveInstance(currentIntent, current);
                }


                Bundle state = app.restoreInstance(intent);

                if (state != null) {

                    Log.d(TAG, "onResolveIntent restoring " + uri);

                    restoreState(state, uri);

                    Log.d(TAG, "Url after new state: " + appView.getUrl());

                    // Remember state intent and uri
                    appViewUrl = currentUri = uri;
                    currentIntent = getIntent();

                    clearHistory();

                    return;
                }

                appView.clearView();

				// Hide old app
                // appView.setVisibility(View.VISIBLE);
			}

			Log.d(TAG, "onResolveIntent loading " + uri);

            // Remember state intent and uri
            appViewUrl = currentUri = uri;
            currentIntent = getIntent();

			super.loadUrl(uri);

            clearHistory();
		}

	}

    private void saveState(Bundle state) {

        // appView.saveState(state);

        String url = appView.getUrl();
        if (TextUtils.isEmpty(url)) {
            url = appViewUrl;
        }

        // appView.getUrl might be already null here
        state.putString("appView.url", url);
        state.putInt("appView.scroll_x", appView.getScrollX());
        state.putInt("appView.scroll_y", appView.getScrollY());

        Log.d(TAG, "saveState: " + getIntent().getDataString() + " with " + url);

        ((SoupApplication)getApplication()).saveInstance(currentIntent, state);

    }

    private void restoreState(Bundle state, String uri) {

        appView.clearView();
        
        // appView.restoreState(state);

        Log.d(TAG, "restoreState: " + uri + " vs " + state.getString("appView.url"));
        
        if (!TextUtils.isEmpty(state.getString("appView.url"))) {
            uri = state.getString("appView.url");
        }

        super.loadUrl(uri);

        appView.scrollTo(state.getInt("appView.scroll_x"), state.getInt("appView.scroll_y"));

    }

    // @Override
    // public void onSaveInstanceState(Bundle state) {
    //
    // if (currentIntent != getIntent()) {
    // return;
    // }
    //
    // // super.onSaveInstanceState(state);
    //
    // saveState(state);
    // }


    public void onDestroy() {
		super.onDestroy();

        // Toast.makeText(this, "App closed", Toast.LENGTH_SHORT).show();
	}

}
