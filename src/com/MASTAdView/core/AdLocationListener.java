//
// Copyright (C) 2011, 2012 Mocean Mobile. All Rights Reserved. 
//
package com.MASTAdView.core;


import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;

import com.MASTAdView.MASTAdConstants;
import com.MASTAdView.MASTAdLog;


public class AdLocationListener implements LocationListener
{
	final private LocationManager mLocationManager;
	final private String mProvider;
	private long mInterval;
	private float mDistance;
	final private Looper listenerLooper;
	final private MASTAdLog adLog;
	
	
	public void fail(String message)
	{
		// NA, override in derived class
	}
	
	public void success(Location location)
	{
		// NA, override in derived class
	}
	
	public AdLocationListener(Context c, Integer interval, Float distance, String provider, MASTAdLog logger)
	{
		mLocationManager = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
		mProvider = provider;
		setRestrictions(interval, distance);
		listenerLooper = null;
		adLog = logger;
	}
	
	public AdLocationListener(Context c, Integer interval, Float distance, String provider, Looper looper, MASTAdLog logger)
	{
		mLocationManager = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
		mProvider = provider;
		setRestrictions(interval, distance);
		listenerLooper = looper;
		adLog = logger;
	}
	
	private void setRestrictions(Integer interval, Float distance)
	{
		if ((interval != null) && (interval >= 0))
		{
			mInterval = interval;
		}
		else
		{
			mInterval = MASTAdConstants.DEFAULT_LOCATION_REPEAT_WAIT;
		}
		
		if ((distance != null) && (distance >= 0.0))
		{
			mDistance = distance;
		}
		else
		{
			mDistance = MASTAdConstants.DEFAULT_LOCATION_REPEAT_DISTANCE;
		}
	}

	public boolean isAvailable()
	{
		try
		{
			boolean isEnabled = mLocationManager.isProviderEnabled(mProvider);
			return isEnabled;
		}
		catch(Exception ex)
		{
			adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "AdLocationListener.isAvailable - exception", ex.getMessage());
		}
		
    	return false;
	}
    	
	public void onProviderDisabled(String provider)
	{
		fail("Location provider is disabled");
	}

	public void onStatusChanged(String provider, int status, Bundle extras)
	{
		if(status == LocationProvider.OUT_OF_SERVICE)
		{
			fail("Location provider is out of service");
		}
	}

	public void onLocationChanged(Location location)
	{
		if ((mInterval <= 0) && (mDistance <= 0.0f))
		{
			// Legacy behavior: grab initial location fix and continue using it, no updates
			mLocationManager.removeUpdates(this);
			if (adLog != null)
			{
				adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdLocationListener", "Listener stopped after update.");
			}
		}
		
		adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdLocationListener", "Location change called.");
		
		success(location);
	}

	public void stop()
	{
		try
		{
			mLocationManager.removeUpdates(this);
			adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdLocationListener", "Listener stopped");
		}
		catch(Exception ex)
		{
			adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "AdLocationListener.stop - exception", ex.getMessage());
		}
	}

	@Override
	public void onProviderEnabled(String provider)
	{
	}

	public void start()
	{
		//mLocMan.requestLocationUpdates(mProvider, 0, 0, this);
		adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdLocationListener", "Listener started.");
		
		if (listenerLooper != null)
		{
			mLocationManager.requestLocationUpdates(mProvider, mInterval, mDistance, this, listenerLooper);
		}
		else
		{
			mLocationManager.requestLocationUpdates(mProvider, mInterval, mDistance, this);
		}
	}
}
