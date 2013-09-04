
// Copyright (C) 2011, 2012 Mocean Mobile. All Rights Reserved. 
//
package com.MASTAdView;

import java.util.Vector;

import android.util.Log;

import com.MASTAdView.core.AdViewContainer;

/**
 * The SDK is instrumented with log() calls to record errors, warnings and server traffic
 * for diagnostic purposes. A small buffer of log events are kept in a memory buffer, and
 * events are also sent to the system log (accessible with the logcat interface.) When
 * reporting problem it is helpful to capture this diagnostic information to aid with
 * debugging and resolution.
 */
final public class MASTAdLog
{	
	/**
	 * Log level value to turn logging off. The default.
	 */
	public static final int LOG_LEVEL_NONE = 0;
	
	/**
	 * Log level value to log errors only.
	 */
	public static final int LOG_LEVEL_ERROR = 1;
	
	/**
	 * Log level value to log errors and debug information
	 */
	public static final int LOG_LEVEL_DEBUG = 2;
	
	private int CurrentLogLevel = 0;
	private AdViewContainer adView = null;
	
	private static int DefaultLevel = LOG_LEVEL_NONE;
	
	// Maximum number of messages to keep in memory for review access through the app;
	// keep this small by default to conserve memory usage. When debugging apps, callers
	// can increase this value.
	private static int maximumInMemoryLogCount = 100;
	
	// Store all diag info in private internal vector of strings
    private static Vector<String> inMemoryLog = null;
    
    
    /**
     * Set default log level to one of the log level values defined in he MASTAdLog class
     * (corresponding to errors only, or everything including server traffic.)
     * 
     * @param logLevel Int log level to set as the default value (initially NONE).
     */
    synchronized public static void setDefaultLogLevel(int logLevel)
	{
		DefaultLevel = logLevel;
	}
	
    
	/**
	 * Construct logging object.
	 * @param adView Base view object associated with logging
	 */
	public MASTAdLog(AdViewContainer adView)
	{
		this.adView =  adView;
		setLogLevel(DefaultLevel);
	}
	
	
	/**
	 * Log a message
	 * @param Level Int logging level, from 0 (none) to 2 (errors, warnings and server traffic)
	 * @param tag String log message tag
	 * @param msg String log message detail
	 */
	public void log(int Level, String tag, String msg)
	{	
		if (msg == null)
		{
			return;
		}
		
		if (tag == null)
		{
			tag = "";
		}
		
		String resultTag;
		if (adView != null)
		{
			resultTag = "["+Integer.toHexString(adView.hashCode())+"]"+ tag;
		}
		else
		{
			resultTag = "[ default ]"+ tag;
		}
		
		// Notify app if delegate is defined
		if (adView != null)
		{
			MASTAdDelegate delegate = adView.getAdDelegate();
			if (delegate != null)
			{
		        MASTAdDelegate.LogEventHandler logHandler = delegate.getLogEventHandler(); 
				if (logHandler != null)
				{
					boolean logEvent = logHandler.onLogEvent(Level, resultTag + msg);
					if (!logEvent)
					{
						// at least write to console for emulator/debugger
						System.out.print(resultTag);
						System.out.println(msg); 
						return;
					}
				}
			}
		}
		
		if (Level <= getLogLevel())
		{
			switch(Level)
			{
			case LOG_LEVEL_ERROR: Log.e(resultTag, msg); break;
			default:
				Log.i(resultTag, msg);
			}
			
			logInternal(msg);
		}
	}

	
	/**
	 * Get the current level for events that will be added to the log.
	 * 
	 * @return log level for events to be recorded
	 */
	synchronized public int getLogLevel()
	{
		return CurrentLogLevel;
	}
	
	
	/**
	 * Set log level to one of the log level values defined in he MASTAdLog class
     * (corresponding to errors, errors + warnings, or everything including server traffic.)
     * The SDK is instrumented with diagnostics logging that can assist with troubleshooting
     * integration problems. Log messages are sent to the system logging interface (viewable
     * with logcat) and an in-memory log of recent messages is stored for easy access.
     * @see MASTAdLog See the MASTAdLog class for more information about logging.
	 * @param logLevel Int log level to control which messages will be sent to the logs.
	 */
	synchronized public void setLogLevel(int logLevel)
	{
		CurrentLogLevel = logLevel;
		switch(logLevel)
		{
		case LOG_LEVEL_ERROR:
			log(logLevel, "SetLogLevel", "LOG_LEVEL_ERROR");
			break;
		case LOG_LEVEL_DEBUG:
			log(logLevel, "SetLogLevel", "LOG_LEVEL_DEBUG");
			break;
		default:
			log(logLevel, "SetLogLevel", "LOG_LEVEL_NONE");
		}
	}
	

	synchronized private void logInternal(String message)
	{
		if (inMemoryLog == null)
		{
			inMemoryLog = new Vector<String>();
		}
		
		if (message != null)
        {
			inMemoryLog.addElement(message);
        }
		
		int currentSize = inMemoryLog.size();
        while (currentSize >= maximumInMemoryLogCount)
        {
            // Need to clean up some old messages first.
        	inMemoryLog.removeElementAt(0);
        	currentSize--;
        }
	}

	
	/**
	 * Get maximum number of messages to keep in memory
	 * @return Current count setting
	 */
	synchronized public static int getMaximumlogCount()
	{
		return maximumInMemoryLogCount;
	}
	
	/**
	 * Set maximum number of messages to keep in memory
	 * @param value Maximum message count
	 */
	synchronized public static void setMaximumLogCount(int value)
	{
		maximumInMemoryLogCount = value;
	}
	
	/**
	 * Get reference to in-memory log informatoin
	 * @return Vector of log message strings
	 */
	synchronized public static Vector<String> getInternalLogs()
	{
		return inMemoryLog;
	}

	/**
	 * Clear internal log messages
	 */
	synchronized public static void clearInternalLogs()
	{
		if (inMemoryLog != null)
		{
			inMemoryLog.clear();
		}
	}
}
