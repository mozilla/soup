package org.mozilla.labs.Soup;

import java.io.InputStream;
import java.net.URL;

import org.json.JSONArray;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;
import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;
import com.phonegap.api.PluginResult.Status;

public class HomeScreenPlugin extends Plugin {
	public static final String ACTION = "add";
	
	private Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
		int width = bm.getWidth();
		int height = bm.getHeight();

		float scaleWidth = ((float) newWidth) / width;
		float scaleHeight = ((float) newHeight) / height;

		// create a matrix for the manipulation
		Matrix matrix = new Matrix();

		// resize the bit map
		matrix.postScale(scaleWidth, scaleHeight);

		// recreate the new Bitmap
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
		return resizedBitmap;
	}

	/* (non-Javadoc)
	 * @see com.phonegap.api.Plugin#execute(java.lang.String, org.json.JSONArray, java.lang.String)
	 */
	@Override
	public PluginResult execute(String action, JSONArray data, String callback) {
		Log.d("HomeScreenPlugin", "Called with " + action);
		PluginResult result = null;
		
		if (ACTION.equals(action)) {
			try {
				String uri = data.getString(0);
				String title = data.getString(1);
				String icon = data.getString(2);
				
				Bitmap bitmap = BitmapFactory.decodeStream((InputStream)new URL(icon).getContent());
				bitmap = getResizedBitmap(bitmap, 72, 72);
				
				Intent shortcutIntent = new Intent(this.ctx, SoupActivity.class);
		        shortcutIntent.setAction(SoupActivity.ACTION_WEBAPP);
		        shortcutIntent.putExtra("uri", uri);
		        
		        Intent intent = new Intent();
		        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title);
		        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
		        intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
		        this.ctx.sendBroadcast(intent);
		        
		        result = new PluginResult(Status.OK);
			} catch (Exception e) {
				Log.d("HomeScreenPlugin Exception", e.getMessage() + " " + e.toString());
				result = new PluginResult(Status.JSON_EXCEPTION);
			}
		} else {
			result = new PluginResult(Status.INVALID_ACTION);
		}
		
		return result;
	}
}
