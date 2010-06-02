package com.googlecode.iloveandroid;

public class CacheFailureException extends Exception
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
