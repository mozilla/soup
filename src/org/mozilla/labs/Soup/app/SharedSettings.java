package org.mozilla.labs.Soup.app;

import org.mozilla.labs.Soup.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;

public class SharedSettings extends PreferenceActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
		
		 // List identities
//        ListPreference listPref = new ListPreference(this);
//        listPref.setKey("keyName"); //Refer to get the pref value
//        listPref.setEntries("Array of values");
//        listPref.setEntryValues("Array of item value");
//        listPref.setDialogTitle("Dialog Title"); 
//        listPref.setTitle("Title");
//        listPref.setSummary("Summary");
//        dialogBasedPrefCat.addPreference(listPref); Adding under the category
	}

}
