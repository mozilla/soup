package org.mozilla.labs;

import android.util.Log;
import android.os.Bundle;
import android.view.View;
import android.app.Activity;
import android.widget.Button;
import android.webkit.WebView;
import android.content.Intent;
import android.graphics.Bitmap;
import org.mozilla.labs.SoupWebView;
import android.graphics.BitmapFactory;
import android.view.View.OnClickListener;

public class Soup extends Activity implements OnClickListener {
	public static final String ACTION_WEBAPP = "org.mozilla.labs.WEBAPP";
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.loading);
    	Log.i("onCreate", "Application launched");
    	
    	Intent intent = getIntent();
    	if (intent != null) {
    		Log.i("onNewIntent", "Got intent! " + intent);
            if (ACTION_WEBAPP.equals(intent.getAction())) {
                String uri = intent.getStringExtra("uri");
                Log.i("onNewIntent", "Got URI " + uri);
                
                WebView webview = new SoupWebView(this);
                webview.loadUrl(uri);
                setContentView(webview);
                return;
            }
    	}
    	
    	setContentView(R.layout.main);
    	
    	Button app1 = (Button)findViewById(R.id.grantland_install);
    	app1.setOnClickListener(this);
    	Button app2 = (Button)findViewById(R.id.angrybirds_install);
    	app2.setOnClickListener(this);
    	Button app3 = (Button)findViewById(R.id.evernote_install);
    	app3.setOnClickListener(this);
    	Button app4 = (Button)findViewById(R.id.linkedin_install);
    	app4.setOnClickListener(this);
    	Button app5 = (Button)findViewById(R.id.etherpal_install);
    	app5.setOnClickListener(this);
    	Button app6 = (Button)findViewById(R.id.newyorktimes_install);
    	app6.setOnClickListener(this);
    	Button app7 = (Button)findViewById(R.id.roundball_install);
    	app7.setOnClickListener(this);
    	Button app8 = (Button)findViewById(R.id.bostonglobe_install);
    	app8.setOnClickListener(this);
    }
    
    public void onClick(View v) {
    	Bitmap icon;
    	
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
        Intent shortcutIntent = new Intent(this, Soup.class);
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
