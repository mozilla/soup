
package org.mozilla.labs.Soup.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class StoreActivity extends SoupActivity {

    private static final String TAG = "StoreActivity";

    protected void onResolveIntent() {

        Log.d(TAG, "onResolveIntent");

        final Intent intent = getIntent();

        // Init web views

        // Allow overriding the Store with an new landing page
        String uri = intent.getDataString();

        if (TextUtils.isEmpty(uri)) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

            uri = prefs.getString("dev_store", "https://apps-preview.mozilla.org");
        }

        if (!onCreateLayout()) { // Webkit existed

            // Update when a new store URL is set
            String currentUri = appView.getUrl();

            if (currentUri != null
                    && Uri.parse(currentUri).getAuthority().equals(Uri.parse(uri).getAuthority())) {
                return;
            }
        }

        // only set URL for fresh views
        super.loadUrl(uri);

    }
}
