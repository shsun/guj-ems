//
// Copyright (C) 2011, 2012 Mocean Mobile. All Rights Reserved. 
//
package com.MASTAdView.core;


import android.os.Bundle;
import android.os.Handler;
import android.os.Message;


final public class AdMessageHandler extends Handler
{
	// Messages sent to and processed by handler
	public static final int	MESSAGE_RESIZE 		= 1000;
	public static final int	MESSAGE_CLOSE 		= 1001;
	public static final int	MESSAGE_HIDE 		= 1002;
	//public static final int	MESSAGE_SHOW 		= 1003;
	public static final int	MESSAGE_EXPAND 		= 1004;
	public static final int	MESSAGE_ANIMATE 	= 1005;
	public static final int	MESSAGE_OPEN 		= 1006;
	public static final int	MESSAGE_PLAY_VIDEO 	= 1007;
	public static final int MESSAGE_CREATE_EVENT = 1008;
	public static final int	MESSAGE_RAISE_ERROR = 1009;
	public static final int MESSAGE_ORIENTATION_PROPERTIES = 1010;

	// public static final int	MESSAGE_PLAY_AUDIO 	= xxxx;
	
	
	// Keys for information passed around in data object
	public static final String ERROR_MESSAGE	= "error.Message";
	public static final String ERROR_ACTION 	= "error.Action";
	public static final String RESIZE_HEIGHT 	= "resize.Height";
	public static final String RESIZE_WIDTH 	= "resize.Width";
	public static final String EXPAND_URL 		= "expand.Url";
	public static final String OPEN_URL 		= "open.Url";
	public static final String PLAYBACK_URL 	= "playback.Url";
		
	
	final private AdViewContainer adView;
	private MraidInterface mraidInterface = null;
	
	
	public AdMessageHandler(AdViewContainer parent)
	{
		super();
		adView = parent;
	}
	
	
	// Handle messages asking functions to be performed on the UI thread; primarily used by JavaScript interface
	// so that operations such as open/expand/resize can be performed on the UI thread. Can also be used by other
	// background threads to run code on UI thread when needed.
	@Override
	synchronized public void handleMessage(Message msg)
	{
		String error = null;
		Bundle data = msg.getData();
		
		if (mraidInterface == null)
		{
			mraidInterface = adView.getAdWebView().getMraidInterface();
		}
		
		switch (msg.what)
		{
			case MESSAGE_RESIZE:
			{
				error = adView.resize(data); 
				if (error != null)
				{
					mraidInterface.fireErrorEvent(error, MraidInterface.MRAID_ERROR_ACTION_RESIZE);
				}
				break;
			}				
			case MESSAGE_CLOSE:
			{
				error = adView.close(data);
				if (error != null)
				{
					mraidInterface.fireErrorEvent(error, MraidInterface.MRAID_ERROR_ACTION_CLOSE);
				}
				break;
			}
			case MESSAGE_HIDE:
			{
				error = adView.hide(data);
				if (error != null)
				{
					mraidInterface.fireErrorEvent(error, MraidInterface.MRAID_ERROR_ACTION_HIDE); 
				}
				break;
			}
			/*
			case MESSAGE_SHOW:
			{
				error = adView.show(data);
				if (error != null)
				{
					mraidInterface.fireErrorEvent(error, "show"); 
				}
				break;
			}
			*/
			case MESSAGE_EXPAND:
			{
				error = adView.expand(data);
				if (error != null)
				{
					mraidInterface.fireErrorEvent(error, MraidInterface.MRAID_ERROR_ACTION_EXPAND); 
				}
				break;
			}
			case MESSAGE_OPEN:
			{
				error = adView.open(data);
				if (error != null)
				{
					mraidInterface.fireErrorEvent(error, MraidInterface.MRAID_ERROR_ACTION_OPEN); 
				}
				break;
			}
			case MESSAGE_PLAY_VIDEO:
			{
				error = adView.playVideo(data);
				if (error != null)
				{
					mraidInterface.fireErrorEvent(error, MraidInterface.MRAID_ERROR_ACTION_PLAYVIDEO); 
				}
				break;
			}
			case MESSAGE_CREATE_EVENT:
			{
				error = adView.createCalendarEvent(data);
				if (error != null)
				{
					mraidInterface.fireErrorEvent(error, MraidInterface.MRAID_ERROR_ACTION_CREATE_EVENT); 
				}
				break;
			}
			case MESSAGE_RAISE_ERROR:
			{
				String errorMessage = data.getString(ERROR_MESSAGE);
				String action = data.getString(ERROR_ACTION);
				mraidInterface.fireErrorEvent(errorMessage, action);
				break;
			}
			case MESSAGE_ORIENTATION_PROPERTIES:
			{
				error = adView.updateOrientationProperties(data);
				if (error != null)
				{
					mraidInterface.fireErrorEvent(error, MraidInterface.MRAID_ERROR_ACTION_SET_ORIENTATION_PROPERTIES);
				}
				break;
			}
		}
		
		super.handleMessage(msg);
	}
}
