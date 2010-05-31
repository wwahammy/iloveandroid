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

class TimedTask extends TimerTask
{
	private static Time lastRun = null;
	Context c = null;
	TimedTask(Context c )
	{
		this.c = c;
	}
	
	@Override
	public void run()
	{
		Log.d("TimedTask", "started run");
		
		Time lastRunPlus4Sec = new Time();
		
		if (lastRun != null)
			lastRunPlus4Sec.set(lastRun.toMillis(false)+ 4000);
		
		Time now = new Time();
		now.setToNow();
		
		if (lastRun == null || now.after(lastRunPlus4Sec))
		{
			RemoteViews updateViews = createUpdate(c);
			if (updateViews != null)
			{
				ComponentName thisWidget = new ComponentName(c, LoveWidget.class);
				AppWidgetManager man = AppWidgetManager.getInstance(c);
				man.updateAppWidget(thisWidget, updateViews);
			}
			
			now.setToNow();
			lastRun = now;
		}
		Log.d("TimedTask", "ended run");
		
	}
	
	private RemoteViews createUpdate(Context context)
	{
		RemoteViews out = null;
		CacheFile cf = new CacheFile(context, "tweet.cache");
		QueryResult qr = null;
		int nextElemPos = -1;
		Time timestamp = null;
		try
		{
			boolean validCache = false;
			try
			{
				if (cf.cacheExists())
				{
					qr = cf.getQueryResult();
					nextElemPos = cf.getNextElementPos();
					
					//check if its been too long
					timestamp = cf.getTimestamp();
					Time plus15min = new Time();
					plus15min.set(timestamp.toMillis(false) + 900000);
					
					Time now = new Time();
					now.setToNow();
					if (now.after(plus15min))
					{
						qr = null;
						timestamp = null;
					}
					else
					{
						validCache = true;
					}
				}
			}
			catch(CacheFailureException e)
			{
				Log.d("TimedTask.createUpdate", "", e);
				qr = null;
			}
			
			if (qr == null)
			{
				Log.d("TimedTask.createUpdate", "qr == null");
				TwitterFactory fact = new TwitterFactory();
				Twitter twit = fact.getInstance();
				Query q = new Query("#android");
				
				qr = twit.search(q);
				nextElemPos = 0;
				
			}
			
			if (validCache)
				cf = new CacheFile(context, "tweet.cache", qr, (nextElemPos+1)%qr.getTweets().size(), timestamp);
			else
				cf = new CacheFile(context, "tweet.cache", qr, (nextElemPos+1)%qr.getTweets().size());
			
			cf.saveToCache();
			
			ImageThread t = new ImageThread(c.getCacheDir().getCanonicalPath(), qr);
			t.run();
			
			
			Tweet currentTweet = qr.getTweets().get(nextElemPos);
			
			out = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			out.setTextViewText(R.id.Username, qr.getTweets().get(nextElemPos).getFromUser());
			Intent usernameIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com/" + currentTweet.getFromUser()));
			PendingIntent userPIntent = PendingIntent.getActivity(context, 0, usernameIntent, 0);
			out.setOnClickPendingIntent(R.id.Username, userPIntent);
			
			out.setTextViewText(R.id.Contents, currentTweet.getText());
			Intent contentsIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://twitter.com/" + currentTweet.getFromUser() + "/status/" + currentTweet.getId()));
			PendingIntent contentsPIntent = PendingIntent.getActivity(context, 0, contentsIntent, 0);
			out.setOnClickPendingIntent(R.id.Contents, contentsPIntent);
			
			File image = new File(c.getCacheDir().getCanonicalPath() + "/" + currentTweet.getFromUser());
			if (image.exists())
			{
				
				Bitmap bitm = BitmapFactory.decodeFile(image.getCanonicalPath());
				out.setImageViewBitmap(R.id.UserPhoto, Bitmap.createScaledBitmap(bitm, 48, 48, false));
				out.setOnClickPendingIntent(R.id.UserPhoto, userPIntent);
			}
			else
			{
				Log.d("TimedTask.createUpdate", "Image doesn't exist");
			}
			
		}
		catch (Exception e)
		{
			Log.d("TimedTask.createUpdate", "really bad",e);
		}
		
		return out;
	}			
	
}

class CacheFile
{
	private Context c = null;
	private String filename = null;
	private QueryResult qr = null;
	private int nextElementPos = -1;
	private Time timestamp = null;
	
	CacheFile(Context c, String filename)
	{
		this.c = c;
		this.filename = filename;
	}
	
	CacheFile(Context c, String filename, QueryResult qr, int nextElementPos)
	{
		this.c = c;
		this.filename = filename;
		this.qr = qr;
		this.nextElementPos = nextElementPos;
	}
	
	CacheFile(Context c, String filename, QueryResult qr, int nextElementPos, Time timestamp)
	{
		this.c = c;
		this.filename = filename;
		this.qr = qr;
		this.nextElementPos = nextElementPos;
		this.timestamp = timestamp;
	}
	
	QueryResult getQueryResult() throws CacheFailureException
	{
		if(qr ==null)
			loadFromFile();
		
		return qr;
	}
	
	int getNextElementPos() throws CacheFailureException
	{
		if(qr ==null)
			loadFromFile();
		
		Log.d("CacheFile.getNextElementPos()", nextElementPos + "");
		return nextElementPos;
	}
	
	Time getTimestamp() throws CacheFailureException
	{
		if(qr ==null)
			loadFromFile();
		
		return timestamp;
	}
	
	
	private void loadFromFile() throws CacheFailureException
	{
		try
		{
			ObjectInputStream input = 
				new ObjectInputStream(
						new BufferedInputStream(
								new FileInputStream(fullFilename())));
			//first object is the query result
			qr = (QueryResult)input.readObject();
			//next is a int saying what list item should be read next
			nextElementPos = input.readInt();
			//last is a timestamp
			timestamp = new Time();
			timestamp.set(input.readLong());				
			
			input.close();
		}
		catch(Exception e)
		{
			throw new CacheFailureException("Line 252", e);
		}
		
	}
	
	
	void saveToCache() throws CacheFailureException
	{
		try
		{
			ObjectOutputStream output = 
				new ObjectOutputStream(
						new BufferedOutputStream(
								new FileOutputStream(fullFilename())));
			//first object is the query result
			output.writeObject(qr);
			//next is a int saying what list item should be read next
			output.writeInt(nextElementPos);
			//next is a timestamp
			if (timestamp == null)
			{	
				Time t = new Time();
				t.setToNow();
				output.writeLong(t.toMillis(false));
				this.timestamp = t;
			}
			else
			{
				output.writeLong(timestamp.toMillis(false));
			}
			output.flush();
			output.close();
		}
		catch(Exception e)
		{
			throw new CacheFailureException("Line 283", e);
		}
	}
	
	boolean cacheExists() throws CacheFailureException
	{
		try
		{
			return new File(fullFilename()).exists();
		}
		catch(Exception e)
		{
			throw new CacheFailureException("Line 295", e);
		}
	
	}
	
	
	private String fullFilename() throws IOException
	{
		return c.getCacheDir().getCanonicalPath() + "/" + filename;
	}
	
	
}

class CacheFailureException extends Exception
{

	public CacheFailureException()
	{
		super();
	}
	
	public CacheFailureException(String string)
	{
		super(string);
	}

	public CacheFailureException(String string, Exception e)
	{
		super(string, e);
	}

	private static final long serialVersionUID = -4074369188741393131L;
	
}










