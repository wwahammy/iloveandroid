/**
 *   This file is part of I Love Android.
 *
 *   I Love Android is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   I Love Android is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with I Love Android.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.googlecode.iloveandroid;

import java.io.*;
import java.util.*;

import com.googlecode.iloveandroid.R;

import twitter4j.*;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;

public class LoveWidget extends AppWidgetProvider
{
	
	@Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // To prevent any ANR timeouts, we perform the update in a service
		Log.d("LoveWidget.onUpdate", "before servicestart");
        context.startService(new Intent(context, UpdateService.class));
        Log.d("LoveWidget.onUpdate", "After servicestart");
    }
	
	
	public static class UpdateService extends Service 
	{
		Timer t = null;
		@Override
	    public void onStart(Intent intent, int startId) 
		{
			Log.d("UpdateService.onStart", "begin");
			t = new Timer();
			TimedTask task = new TimedTask(this);
			task.run();
			
			t.scheduleAtFixedRate(task, 6000, 6000);
			
			Log.d("UpdateService.onStart", "end");
		}
		
		public void onDestroy()
		{
			Log.d("UpdateService.onDestroy", "begin");
			t.cancel();
			t = null;
			Log.d("UpdateService.onDestroy", "end");
		}
		
		public boolean onUnbind(Intent intent)
		{
			Log.d("UpdateService.onUnbind", "begin");
			stopSelf();
			t.cancel();
			t = null;
			Log.d("UpdateService.onUnbind", "end");
			return false;
		}
		
		
		@Override
		public IBinder onBind(Intent arg0)
		{
			
			return null;
		}
		
	}
}











