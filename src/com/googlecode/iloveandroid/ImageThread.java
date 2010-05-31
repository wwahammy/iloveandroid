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
import java.net.*;
import java.util.*;

import android.util.*;

import twitter4j.*;

public class ImageThread extends Thread
{
	QueryResult qr = null;
	String cacheDir = null;
	public ImageThread(String cacheDir, QueryResult qr)
	{
		this.qr = qr;
		this.cacheDir = cacheDir;
	}
	
	@Override
	public void run()
	{
		HttpURLConnection conn = null;
		BufferedOutputStream out = null;
		try
		{
			List<Tweet> tweets = qr.getTweets();
			for(Tweet t : tweets)
			{
				File cacheFile = new File(cacheDir + "/" + t.getFromUser());
				if (!cacheFile.exists())
				{	
					URL u = new URL(t.getProfileImageUrl());
				    conn = (HttpURLConnection) u.openConnection();
				    conn.setRequestMethod("GET");
				    conn.setDoOutput(true);
				    conn.connect();
				    
				    out = new BufferedOutputStream(new FileOutputStream(cacheFile));
				    InputStream in = conn.getInputStream();
				    
				    byte[] buffer = new byte[1024];
				    int len1 = 0;
				    while ( (len1 = in.read(buffer)) != -1 ) {
				      out.write(buffer,0, len1);
				    }
				    
				    in.close();
				    conn.disconnect();
				    out.close();
				}
				out = null;
			}
		}
		catch(Exception e)
		{
			Log.d("ImageThread.run", "", e);
			if (conn != null)
				conn.disconnect();
			if (out != null)
				try
				{
					out.close();
				} catch (IOException e1)
				{
					
					Log.d("ImageThread.run", "", e1);
				}
			
		}
		
		
	}
	
	

}
