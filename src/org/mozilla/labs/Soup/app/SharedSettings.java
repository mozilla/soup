package org.mozilla.labs.Soup.app;

import org.mozilla.labs.Soup.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SharedSettings extends PreferenceActivity {

	// The name of the SharedPreferences file we'll store preferences in.
	public static final String PREFS_NAME = "moz.mozilla.labs.Soup";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getPreferenceManager().setSharedPreferencesName(PREFS_NAME);
		addPreferencesFromResource(R.xml.preferences);
	}

}
