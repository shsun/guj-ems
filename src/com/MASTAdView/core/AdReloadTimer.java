//
// Copyright (C) 2011, 2012 Mocean Mobile. All Rights Reserved. 
//
package com.MASTAdView.core;

import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;

import com.MASTAdView.MASTAdConstants;
import com.MASTAdView.MASTAdLog;


final public class AdReloadTimer
{
	final private AdViewContainer adContainer;
	final private MASTAdLog adLog;
	
	private int adReloadPeriod = MASTAdConstants.AD_RELOAD_PERIOD;
	
	private TimerTask reloadTask;
	private Timer reloadTimer;
	
	
	public AdReloadTimer(Context context, AdViewContainer container, MASTAdLog logger)
	{
		adContainer = container;
		adLog = logger;
	}
	
	
	synchronized public void setAdReloadPeriod(int value)
	{
		if (value < 1000)
		{
			adReloadPeriod = value * 1000;
		}
		else
		{
			adReloadPeriod = value;
		}
	}
	
	
	synchronized public int getAdReloadPeriod()
	{
		return adReloadPeriod;
	}
	
	
	synchronized public void startTimer()
	{
		try
		{
			if (reloadTimer == null)
			{
				reloadTimer = new Timer();
			}
			
			cancelTask(); // cancel existing task, if any...
			TimerTask newReloadTask = new TimerTask()
			{
				@Override
				public void run()
				{
					adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdReloadTimer", "refresh from timer task");
					adContainer.StartLoadContent(); // trigger reload of existing site/zone
				}
			};
			
			if (adReloadPeriod > 0) 
			{
				adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdReloadTimer - start: ", String.valueOf(adReloadPeriod/1000));					
				reloadTimer.schedule(newReloadTask, adReloadPeriod);
			}
			else if (adReloadPeriod < 0)
			{
				reloadTimer.schedule(newReloadTask, MASTAdConstants.AD_RELOAD_PERIOD);
				adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdReloadTimer", String.valueOf(MASTAdConstants.AD_RELOAD_PERIOD/1000)+" default");
			}
			else
			{
				adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdReloadTimer", "stopped");
			}
			
			reloadTask = newReloadTask; 
		}
		catch (Exception e)
		{
			adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdReloadTimer", e.getMessage());
		}
	}
	
	
	synchronized public void stopTimer(boolean remove)
	{
		if (reloadTimer != null)
		{
			try
			{
				adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdReloadTimer", "timer stopped");
				reloadTimer.cancel();
				if (remove)
				{
					reloadTimer = null;
				}
			}
			catch (Exception e)
			{
				adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "AdReloadTimer", e.getMessage());				
			}
		}
	}

	
	synchronized public void cancelTask()
	{
		if (reloadTask != null)
		{
			adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdReloadTimer", "timer task cancelled");
			reloadTask.cancel();
			reloadTask = null;
		}
	}
}
