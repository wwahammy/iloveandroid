package com.googlecode.iloveandroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import twitter4j.QueryResult;
import android.content.Context;
import android.text.format.Time;
import android.util.Log;

public class CacheFile
{
	private String cacheDir  = null;
	private String filename = null;
	private QueryResult qr = null;
	private int nextElementPos = -1;
	private Time timestamp = null;
	
	public CacheFile(String cacheDir, String filename)
	{
		this.cacheDir = cacheDir;
		this.filename = filename;
	}
	
	CacheFile(String cacheDir, String filename, QueryResult qr, int nextElementPos)
	{
		this.cacheDir = cacheDir;
		this.filename = filename;
		this.qr = qr;
		this.nextElementPos = nextElementPos;
	}
	
	CacheFile(String cacheDir, String filename, QueryResult qr, int nextElementPos, Time timestamp)
	{
		this.cacheDir = cacheDir;
		this.filename = filename;
		this.qr = qr;
		this.nextElementPos = nextElementPos;
		this.timestamp = timestamp;
	}
	
	public QueryResult getQueryResult() throws CacheFailureException
	{
		if(qr ==null)
			loadFromFile();
		
		return qr;
	}
	
	public int getNextElementPos() throws CacheFailureException
	{
		if(qr ==null)
			loadFromFile();
		
		//Log.d("CacheFile.getNextElementPos()", nextElementPos + "");
		return nextElementPos;
	}
	
	public Time getTimestamp() throws CacheFailureException
	{
		if(qr ==null)
			loadFromFile();
		
		return timestamp;
	}
	
	
	private synchronized void loadFromFile() throws CacheFailureException
	{
		try
		{
			ObjectInputStream input = 
				new ObjectInputStream(
						new QuietBufferedInputStream(
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
	
	
	public synchronized void saveToCache() throws CacheFailureException
	{
		try
		{
			ObjectOutputStream output = 
				new ObjectOutputStream(
						new QuietBufferedOutputStream(
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
	
	public synchronized boolean cacheExists() throws CacheFailureException
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
		String fullname = cacheDir + "/" + filename;
		//Log.d("CacheFile.fullFilename()", "The file name given is " +fullname);
		return fullname;
	}
	
	
}