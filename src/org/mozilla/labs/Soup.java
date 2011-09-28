package org.mozilla.labs;

import android.util.Log;
import android.view.View;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View.OnClickListener;

public class Soup extends Activity implements OnClickListener {
	public static final String ACTION_WEBAPP = "org.mozilla.labs.WEBAPP";
	
    @Override
    public void onResume() {
    	super.onResume();
    	Log.i("onResume", "Application (re)started");
    	setContentView(R.layout.main);
    }
    
    public void onClick(View v) {
    	Bitmap icon;
    	Log.i("onClick", "on view " + v);
    	
    	switch (v.getId()) {
    	case R.id.grantland_install:
    		icon = BitmapFactory.decodeResource(getResources(), R.drawable.grantland48);
    		addToHomeScreen("Grantland", "http://grantland.com/", icon);
    		break;
    	case R.id.newyorktimes_install:
    		icon = BitmapFactory.decodeResource(getResources(), R.drawable.newyorktimes48);
    		addToHomeScreen("NY Times", "http://mobile.nytimes.com/", icon);
    		break;
    	case R.id.roundball_install:
    		icon = BitmapFactory.decodeResource(getResources(), R.drawable.roundball48);
    		addToHomeScreen("Roundball", "http://www.limejs.com/static/roundball/index.html", icon);
    		break;
    	case R.id.etherpal_install:
    		icon = BitmapFactory.decodeResource(getResources(), R.drawable.etherpal48);
    		addToHomeScreen("Etherpal", "http://etherpal.org/", icon);
    		break;
    	case R.id.angrybirds_install:
    		icon = BitmapFactory.decodeResource(getResources(), R.drawable.angrybirds48);
    		addToHomeScreen("Angry Birds", "http://angrybirds.com/", icon);
    		break;
    	case R.id.evernote_install:
    		icon = BitmapFactory.decodeResource(getResources(), R.drawable.evernote48);
    		addToHomeScreen("Evernote", "http://evernote.com/", icon);
    		break;
    	case R.id.linkedin_install:
    		icon = BitmapFactory.decodeResource(getResources(), R.drawable.linkedin48);
    		addToHomeScreen("LinkedIN", "http://touch.www.linkedin.com/", icon);
    		break;
    	case R.id.bostonglobe_install:
    		icon = BitmapFactory.decodeResource(getResources(), R.drawable.bostonglobe48);
    		addToHomeScreen("BostonGlobe", "http://bostonglobe.com/", icon);
    		break;
    	}
    }
    
    protected void addToHomeScreen(String title, String uri, Bitmap icon) {
    	Log.i("addToHomeScreen", "Adding " + title + " at " + uri);
        Intent shortcutIntent = new Intent(this, SoupWeb.class);
        shortcutIntent.setAction(ACTION_WEBAPP);
        shortcutIntent.putExtra("uri", uri);
        
        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
        intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
        sendBroadcast(intent);
    }
}
