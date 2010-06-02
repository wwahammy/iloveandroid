package com.googlecode.iloveandroid;

import java.io.File;
import java.util.TimerTask;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.format.Time;
import android.util.Log;
import android.widget.RemoteViews;

public class TimedTask extends TimerTask
{
	private static Time lastRun = null;
	public static final int RESULTS_PER_PAGE = 25;
	Context c = null;

	TimedTask(Context c)
	{
		this.c = c;
	}

	@Override
	public void run()
	{
		// Log.d("TimedTask", "started run");

		Time lastRunPlus4Sec = new Time();

		if (lastRun != null)
			lastRunPlus4Sec.set(lastRun.toMillis(false) + 4000);

		Time now = new Time();
		now.setToNow();

		if (lastRun == null || now.after(lastRunPlus4Sec))
		{
			RemoteViews updateViews = createUpdate(c);
			if (updateViews != null)
			{
				ComponentName thisWidget = new ComponentName(c,
						LoveWidget.class);
				AppWidgetManager man = AppWidgetManager.getInstance(c);
				man.updateAppWidget(thisWidget, updateViews);
			}

			now.setToNow();
			lastRun = now;
		}
		// Log.d("TimedTask", "ended run");

	}

	private synchronized RemoteViews createUpdate(Context context)
	{
		RemoteViews out = null;
		try
		{
			String cacheDir = context.getCacheDir().getAbsolutePath();
			// Log.d("TimedTask.createUpdate", "dir path is " + cacheDir);
			CacheFile cf = new CacheFile(cacheDir, "tweet.cache");
			QueryResult qr = null;
			int nextElemPos = -1;
			Time timestamp = null;

			boolean validCache = false;
			try
			{
				if (cf.cacheExists())
				{
					qr = cf.getQueryResult();
					nextElemPos = cf.getNextElementPos();

					// check if its been too long
					timestamp = cf.getTimestamp();
					Time plus15min = new Time();
					plus15min.set(timestamp.toMillis(false) + 900000);

					Time now = new Time();
					now.setToNow();
					if (now.after(plus15min))
					{
						qr = null;
						timestamp = null;
						// Log.d("TimedTask.createUpdate befpre cleanCache",
						// "dir path is " + cacheDir);
						cleanCache(cacheDir);
					} else
					{
						validCache = true;
					}
				}
			} catch (CacheFailureException e)
			{
				Log.d("TimedTask.createUpdate", "", e);
				qr = null;
				cleanCache(cacheDir);
			}

			if (qr == null)
			{
				// Log.d("TimedTask.createUpdate", "qr == null");
				TwitterFactory fact = new TwitterFactory();
				Twitter twit = fact.getInstance();
				Query q = new Query("#android");
				q.setRpp(RESULTS_PER_PAGE);

				qr = twit.search(q);
				nextElemPos = 0;

			}

			if (validCache)
				cf = new CacheFile(cacheDir, "tweet.cache", qr,
						(nextElemPos + 1) % qr.getTweets().size(), timestamp);
			else
				cf = new CacheFile(cacheDir, "tweet.cache", qr,
						(nextElemPos + 1) % qr.getTweets().size());

			cf.saveToCache();

			ImageThread t = new ImageThread(cacheDir, qr);
			t.run();

			Tweet currentTweet = qr.getTweets().get(nextElemPos);

			out = new RemoteViews(context.getPackageName(),
					R.layout.widget_layout);
			out.setTextViewText(R.id.Username, qr.getTweets().get(nextElemPos)
					.getFromUser());
			Intent usernameIntent = new Intent(Intent.ACTION_VIEW, Uri
					.parse("http://twitter.com/" + currentTweet.getFromUser()));
			PendingIntent userPIntent = PendingIntent.getActivity(context, 0,
					usernameIntent, 0);
			out.setOnClickPendingIntent(R.id.Username, userPIntent);

			out.setTextViewText(R.id.Contents, currentTweet.getText());
			Intent contentsIntent = new Intent(Intent.ACTION_VIEW, Uri
					.parse("http://twitter.com/" + currentTweet.getFromUser()
							+ "/status/" + currentTweet.getId()));
			PendingIntent contentsPIntent = PendingIntent.getActivity(context,
					0, contentsIntent, 0);
			out.setOnClickPendingIntent(R.id.Contents, contentsPIntent);

			File image = new File(cacheDir + "/" + currentTweet.getFromUser());
			if (image.exists())
			{

				Bitmap bitm = BitmapFactory.decodeFile(image.getAbsolutePath());
				out.setImageViewBitmap(R.id.UserPhoto, Bitmap
						.createScaledBitmap(bitm, 48, 48, false));
				out.setOnClickPendingIntent(R.id.UserPhoto, userPIntent);
			} else
			{
				Log.d("TimedTask.createUpdate", "Image doesn't exist");
			}

		} catch (Exception e)
		{
			Log.d("TimedTask.createUpdate", "really bad", e);
		}

		return out;
	}

	private void cleanCache(String cacheDir)
	{
		File cache = new File(cacheDir);
		for (File f : cache.listFiles())
		{
			if (f.getName() != "tweet.cache")
				f.delete();
		}
	}

}
