//
// Copyright (C) 2011, 2012 Mocean Mobile. All Rights Reserved. 
//
package com.MASTAdView.core;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.MASTAdView.MASTAdDelegate;
import com.MASTAdView.MASTAdLog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

final public class DeviceFeatures
{
	final private Context context;
	final AdViewContainer adContainer;
	
	private Boolean cacheSmsSupport 		= null;
	private Boolean cachePhoneSupport 		= null;
	//private Boolean cacheEmailSupport 		= null;
	private Boolean cacheCalendarSupport 	= null;
	private Boolean cachePictureSupport 	= null;
	private Boolean cacheVideoSupport 		= null;
	
	
	public DeviceFeatures(Context context, AdViewContainer container)
	{
		this.context = context;
		adContainer = container;
	}
	
	
	public boolean isSupported(MraidInterface.FEATURES feature)
	{
		switch(feature)
		{
		case SMS:
			return smsSupport();
		case TEL:
			return phoneSupport();
		//case EMAIL:
			//return emailSupport();
		case CALENDAR:
			return calendarSupport();
		case STORE_PICTURE:
			return storePictureSupport();
		case INLINE_VIDEO:
			return inlineVideoSupport();
		}

		return false;
	}
	
	
	// Allow app developer to override system if desired
	/*
	public void setSupported(MraidInterface.FEATURES feature, Boolean value)
	{
		switch(feature)
		{
		case SMS:
			cacheSmsSupport = value;
		case PHONE:
			cachePhoneSupport = value;
		//case EMAIL:
			//return emailSupport();
		case CALENDAR:
			cacheCalendarSupport = value;
		case STORE_PICTURE:
			cachePictureSupport = value;
		case INLINE_VIDEO:
			cacheVideoSupport = value;
		}
	}
	*/
	
	
	public boolean smsSupport()
	{
		MASTAdDelegate delegate = adContainer.getAdDelegate();
		if (delegate != null)
		{
			MASTAdDelegate.FeatureSupportHandler handler = delegate.getFeatureSupportHandler();
			
			if (handler != null)
			{
				Boolean result = handler.shouldSupportSMS();
				if (result != null)
				{
					return result.booleanValue();
				}
			}
		}
		
		if (cacheSmsSupport == null)
		{
			cacheSmsSupport = context.checkCallingOrSelfPermission(android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
		}
		
		return cacheSmsSupport;
	}
	
	
	public boolean phoneSupport()
	{
		MASTAdDelegate delegate = adContainer.getAdDelegate();
		if (delegate != null)
		{
			MASTAdDelegate.FeatureSupportHandler handler = delegate.getFeatureSupportHandler();
			
			if (handler != null)
			{
				Boolean result = handler.shouldSupportPhone();
				if (result != null)
				{
					return result.booleanValue();
				}
			}
		}
		
		if (cachePhoneSupport == null)
		{
			cachePhoneSupport = context.checkCallingOrSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
		}
		
		return cachePhoneSupport;
	}
	
	
	public boolean calendarSupport()
	{
		MASTAdDelegate delegate = adContainer.getAdDelegate();
		if (delegate != null)
		{
			MASTAdDelegate.FeatureSupportHandler handler = delegate.getFeatureSupportHandler();
			
			if (handler != null)
			{
				Boolean result = handler.shouldSupportCalendar();
				if (result != null)
				{
					return result.booleanValue();
				}
			}
		}
		
		if (cacheCalendarSupport == null)
		{
			cacheCalendarSupport =
					((context.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) &&
					 (context.checkCallingOrSelfPermission(android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED));
		}
		
		return cacheCalendarSupport;
	}
	
	
	public String createCalendarInteractive(String description, String location,  String summary, String start, String end)
	{
		try
		{
			Intent intent = new Intent(Intent.ACTION_EDIT); 
			intent.setType("vnd.android.cursor.item/event"); 
			intent.putExtra("dtstart", parseDateString(start)); // Events.DTSTART from API 14
			intent.putExtra("dtend", parseDateString(end));		// Events.DTEND from API 14   
			intent.putExtra("title", summary);					// Events.TITLE from API 14
			intent.putExtra("eventLocation", location);			// Events.EVENT_LOCATION from API 14
			intent.putExtra("descriptoin", description);		// Events.DESCRIPTION from API 14
			
			context.startActivity(intent);
		}
		catch (Exception ex)
		{
			String error = "Error creating calendar: " + ex.getMessage();
			MASTAdLog logger = new MASTAdLog(null);
			logger.log(MASTAdLog.LOG_LEVEL_ERROR, "DeviceFeatures", error);
			return error;
		}
			
		return null;
	}
	
	
	private SimpleDateFormat dateFormat = null;
	
	
	// Parse date strings
	synchronized private Long parseDateString(String input)
	{
		try
		{
			if (dateFormat == null)
			{
				// java.text.DateFormat df = DateFormat.getLongDateFormat(context);
				
				// spec format sample: '2011-03-24T09:00-08:00'
				dateFormat = new SimpleDateFormat("yyyy-MM-FF'T'HH:mmZ");
			}
			Date myDate = dateFormat.parse(input);
			return myDate.getTime();
		}
		catch(Exception ex)
		{
			MASTAdLog logger = new MASTAdLog(null);
			logger.log(MASTAdLog.LOG_LEVEL_ERROR, "DeviceFeatures exception parsing date", ex.getMessage());
		}
		
		return System.currentTimeMillis();
	}
	
		
	public boolean storePictureSupport()
	{
		MASTAdDelegate delegate = adContainer.getAdDelegate();
		if (delegate != null)
		{
			MASTAdDelegate.FeatureSupportHandler handler = delegate.getFeatureSupportHandler();
			
			if (handler != null)
			{
				Boolean result = handler.shouldSupportStorePicture();
				if (result != null)
				{
					return result.booleanValue();
				}
			}
		}
		
		if (cachePictureSupport == null)
		{
			cachePictureSupport = true;
		}

		return cachePictureSupport;
	}
	
	
	public boolean inlineVideoSupport()
	{
		MASTAdDelegate delegate = adContainer.getAdDelegate();
		if (delegate != null)
		{
			MASTAdDelegate.FeatureSupportHandler handler = delegate.getFeatureSupportHandler();
			
			if (handler != null)
			{
				Boolean result = handler.shouldShouldPlayVideo();
				if (result != null)
				{
					return result.booleanValue();
				}
			}
		}
		
		if (cacheVideoSupport == null)
		{
			// Honeycomb and later OS versions can playback inline with hardware support
			final int honeycomb = 11; // Build.VERSION_CODES.HONEYCOMB if using API 3.0 or above
			if ((Build.VERSION.SDK_INT >= honeycomb) && (context instanceof Activity))
			{
				// inline can work, must turn on hardware acceleration for web views, but can only
				// do this if our context is an activity which allows access to the window properties
				
				
				
				// XXX true when ready, currently this causes hw acceleration to be enabled in the web view, which cases some problems
				cacheVideoSupport = false;
				
				
				
			}
			else
			{
				cacheVideoSupport = false; // no inline video?
			}
		}

		return cacheVideoSupport;
	}
	
	
	public String playVideo(String videoUrl)
	{
		// Launch the play video intent and let android use the preferred player for this device/user
		Uri uri = Uri.parse(videoUrl);
		Intent i = new Intent(Intent.ACTION_VIEW, uri);
		if (i.resolveActivity(context.getPackageManager()) == null)
		{
			String error = "No video playback handler found, skipping..."; // XXX string
			MASTAdLog logger = new MASTAdLog(null);
			logger.log(MASTAdLog.LOG_LEVEL_ERROR, "DeviceFeatures", error);			
			return error;
		}
		
		context.startActivity(i);
		return null;
	}
}
