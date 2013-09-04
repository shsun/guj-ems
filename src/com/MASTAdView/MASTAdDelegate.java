//
// Copyright (C) 2011, 2012 Mocean Mobile. All Rights Reserved. 
//
package com.MASTAdView;

import java.util.HashMap;

/**
 * The delegate class defines interfaces developers can use to extend and/or interact with
 * feature and functions of the SDK ad handling, including:
 * <UL>
 * <LI> Ad download events and errors
 * <LI> Rich media events (MRAID open, expand, etc.)
 * <LI> Activities affected the ad view (clicks, resize, etc.)
 * <LI> Third party ad events (client side hand-off for third party SDKs)
 * <LI> Feature support decisions (SMS, phone, etc.)
 * <LI> Log message events
 * </UL>
 */
final public class MASTAdDelegate
{
	private AdDownloadEventHandler		adDownloadEventHandler 			= null;
	private RichmediaEventHandler		richmediaEventHandler 			= null;
	private AdActivityEventHandler		adActivityEventHandler 			= null;
	private ThirdPartyEventHandler		thirdPartyEventHandler 			= null;
	private FeatureSupportHandler		featureSupportHandler			= null;
	private LogEventHandler				logEventHandler					= null;
	
	
	/**
	 * Set handler for ad download events.
	 * @param adDownload Object implementing AdDownloadEventHandler interface
	 * @see AdDownloadEventHandler
	 */
	synchronized public void setAdDownloadHandler(AdDownloadEventHandler handler)
	{
		adDownloadEventHandler = handler;
	}
	
	
	/**
	 * Get reference to registered ad download event handler.
	 */
	synchronized public AdDownloadEventHandler getAdDownloadHandler()
	{
		return adDownloadEventHandler;
	}
	
	
	/**
	 * Setup a listener to be notified when rich media (MRAID) events occur, as described in the
	 * MRAID event interface.
	 * @param handler Object implementing the RichmediaEventHandler interface.
	 * @see MraidEventHandler
	 */
	synchronized public void setRichmediaEventHandler(RichmediaEventHandler handler)
	{
		richmediaEventHandler = handler;
	}
	
	
	/**
	 * Get reference to registered rich media event handler.
	 */
	synchronized public RichmediaEventHandler getRichmediaEventHandler()
	{
		return richmediaEventHandler;
	}
	
	
	/**
	 * Setup handler for ad window activity, as described in the interface definition below.
	 * @param handler Object implementing the AdActivityEventHandler interface.
	 * @see AdActivityEventHandler
	 */
	synchronized public void setAdActivityEventHandler(AdActivityEventHandler handler)
	{
		adActivityEventHandler = handler;
	}
	
	
	/**
	 * Get reference to registered ad window activity event handler.
	 */
	synchronized public AdActivityEventHandler getAdActivityEventHandler()
	{
		return adActivityEventHandler;
	}
	

	/**
	 * Set custom third party ad request handler.
	 * @param ThirdPartyEventHandler Object implementing the ThirdPartyEventHandler interface.
	 * @see ThirdPartyEventHandler 
	 */
	synchronized public void setThirdPartyRequestHandler(ThirdPartyEventHandler handler)
	{
		thirdPartyEventHandler = handler;
	}
	
	
	/**
	 * Get reference to third party ad request handler.
	 */
	synchronized public ThirdPartyEventHandler getThirdPartyRequestHandler()
	{
		return thirdPartyEventHandler;
	}
	
	
	/**
	 * Set custom log event handler.
	 * @param ThirdPartyEventHandler Object implementing the LogEventHandler interface.
	 * @see LogEventHandler 
	 */
	synchronized public void setLogEventHandler(LogEventHandler handler)
	{
		logEventHandler = handler;
	}
	
	
	/**
	 * Get reference to log event handler.
	 */
	synchronized public LogEventHandler getLogEventHandler()
	{
		return logEventHandler;
	}

	
	/**
	 * Setup handler for custom device feature support handler.
	 * @param Object implementing the FeatureSupportHandler interface.
	 * @see FeatureSupportHandler 
	 */
	synchronized public void setFeatureSupportHandler(FeatureSupportHandler handler)
	{
		featureSupportHandler = handler;
	}
	
	
	/**
	 * Get reference to device feature support handler.
	 */
	synchronized public FeatureSupportHandler getFeatureSupportHandler()
	{
		return featureSupportHandler;
	}
	
	
	/**
	 * The interface for ad download events. During normal processing begin() and end()
	 * will be invoked as the ad download progresses, and then onAdViewable() when the ad content
	 * is inserted into a view for display. If an error occurs, the error() method will be invoked.
	 * See the sample application source code for example source for this use case.
	 */
	public interface AdDownloadEventHandler
	{
		/**
		 * This event is fired before ad download begins.
		 * @param sender The ad view from which the event originates.
		 */
		public void onDownloadBegin(MASTAdView sender);
		
		/**
		 * This event is fired after ad content has been fully downloaded & parsed, and is ready to be
		 * inserted into a view for display. It will NOT be invoked if an error occurs.
		 * @param sender The ad view from which the event originates. 
		 */
		public void onDownloadEnd(MASTAdView sender);
		
		/**
		 * This event is triggered when ad content is inserted into an ad view for display, following
		 * a successful download of ad content, or in the case of an error, if the SDK is configured to
		 * re-use the last ad and a previous ad is available.
		 * @param sender
		 */
		public void onAdViewable(MASTAdView sender);
		
		/**
		 * This event is fired in the case of a failure to download content.
		 * @param sender The ad view from which the event originates.
		 * @param error String error message. For example, if the server does not return an ad,
		 * the error method will be invoked and the error string will contain the message defined
		 * in Constants.STR_EMPTY_SERVER_RESPONSE. 
		 */
		public void onDownloadError(MASTAdView sender, String error);
	}


	/**
	 * Interface for rich media (MRIAD) event handler, invoked when MRAID events
	 * such as resize, expand, close, etc. occur.
	 */
	public interface RichmediaEventHandler 
	{
		/**
		 * The onRichmediaEvent() method is invoked when events (such as resize, close, etc.)
		 * as defined in the MRAID standard, occur. The SDK responds to these events as needed,
		 * but the application can perform logging or other application side processing as desired.
		 * @param sender The ad view from which the event originates.
		 * @param name Event name (such as "close").
		 * @param params Any parameters associated with the event.
		 * <P>
		 * A sample which executes a custom method when an MRAID event fires is as follows:
		 * <pre>
		 * class UserOnRichmediaHandler implements RichmediaEventHandler {
		 *     public void onRichmediaEvent(MASTAdView sender, String name, String params) {
		 *         // This class method is responsible for invoking a handler to run
		 *         // code on the UI thread that displays information about the event.
		 *         updateUi("mraid event: " + name + ": " + params);
		 *     }	
		 * }
		 *   
		 * adView.getAdDelegate().setRichmediaEventHandler(new UserOnRichmediaHandler());
		 * </pre>
		 * <P>
		 * See the sample application source code for complete source for this use case.
		 */
		public void onRichmediaEvent(MASTAdView sender, String name, String params);
	}

	
	/**
	 * Interface which supports custom callback to be invoked when ad view is attached to
	 * or detached from an activity (window), and when a banner ad view "expands" in any
	 * way (MRAID expand, resize, or interstitial display), and closes back again.
	 */
	public interface AdActivityEventHandler 
	{
		/**
		 * This is called when the view is attached to a window. See the underlying web view
		 * onAttachedToWindow documentation for more information.
		 * @param sender The ad view from which the event originated is passed as a parameter.
		 */
		public void onAdAttachedToActivity(MASTAdView sender);
		
		/**
		 * This is called when the view is detached to a window. See the underlying web view
		 * onDetachedToWindow documentation for more information.
		 * @param sender The ad view from which the event originated is passed as a parameter.
		 */
		public void onAdDetachedFromActivity(MASTAdView sender);

		/**
		 * Invoked when a URL is being loaded in the ad view. See the documentation for the
		 * underlying web view shouldOverrideUrlLoading () method for more information.
		 * @param sender The ad view from which the event originates.
		 * @param url URL to be loaded in ad view.
		 * @return Boolean true if caller has completely handled the click event and wants
		 * to skip the default SDK click processing; return false if the caller has only
		 * implemented a "side-effect" such as logging, and wants the default SDK logic
		 * to continue. 
		 * <P>
		 * Sample code which shows a custom click listener is as follows:
		 * <pre>
		 * class UserAdActivityEventHandler implements AdActivityEventHandler {
		 *     ... other required methods ...
		 *     public boolean onAdClicked(MASTAdView arg0, String arg1) {
		 *         // This class method is responsible for invoking a handler to run
		 *         // code on the UI thread that displays information about the event.
		 *         updateUi("Click url = "+ arg1);
		 *         return false; // SDK should continue processing click
		 *     }
		 * }
		 * 
		 * adView.getAdDelegate().setAdActivityHandler(new UserAdActivityEventHandler());
		 * </pre>
		 * <P>
		 * See the sample application source code for complete source for this use case.
		 */
		public boolean onAdClicked(MASTAdView sender, String url);
		
		/**
		 * This is called when a banner ad expands to larger size using the MRAID expand method.
		 * @param sender The ad view from which the event originated is passed as a parameter.
		 * @param height Height of "expanded" view.
		 * @param width Width of "expanded" view.
		 */
		public void onAdExpanded(MASTAdView sender, int height, int width); // drop width/height?
		
		/**
		 * This is called when a banner ad resizes to a larger size using the MRAID resize method.
		 * @param sender The ad view from which the event originated is passed as a parameter.
		 * @param height Height of "expanded" view.
		 * @param width Width of "expanded" view.
		 */
		public void onAdResized(MASTAdView sender, int height, int width);
		
		/**
		 * This is called when an expanded view is closed, returning to banner state, or
		 * "gone" for interstitial ads, after the close button is pressed.
		 * @param sender The ad view from which the event originated is passed as a parameter.
		 */
		public void onAdCollapsed(MASTAdView sender);
	}
	
	
	/**
	 * Interface for third party ad requests, with an onThirdPartyEvent() method invoked when
	 * client-side third party ad requests are received by the SDK. In this case the only action
	 * taken by the SDK is to notify the app via this callback; the remaining work for integrating
	 * and displaying ads with a third party SDK is the application developers responsibility.
	 */
	public interface ThirdPartyEventHandler 
	{
		/**
		 * Invoked when third party ad requests are being handled.
		 * @param sender The originating ad view where the event was triggered.
		 * @param params Parameters for the event. The parameters will include
		 * name/value pairs for the "type", "campaign" and "track_url" standard 
		 * items, plus any additional data received in the ad response.
		 * <P>
		 * A sample which executes a custom method when a third party request occurs is as follows:
		 * <pre>
		 * class UserOnThirdPartyRequest implements ThirdPartyEventHandler {
		 *     public void onThirdPartyEvent(MASTAdView arg0, HashMap<String, String> arg1) {
		 *         // This class method is responsible for invoking a handler to run
		 *         // code on the UI thread that displays information about the request.
		 *         updateUi(arg1.toString());
		 *     }	
		 * }
		 *   
		 * adserverView.getAdDelegate().setThirdPartyRequestHandler(new UserOnThirdPartyRequest());
		 * </pre>
		 * <P>
		 * See the sample application source code for complete source for this use case.
		 */
		public void onThirdPartyEvent(MASTAdView sender, HashMap<String,String> params);
	}
	
	
	/**
	 * Interface for log event support allowing an application developer to augment or replace the
	 * logging behavior of the SDK. The default behavior is to log error or information events to the
	 * standard Android logcat system, and keep a small in-memory buffer of those messages which can
	 * be retrieved and processed via the application developer. If additional logic is desired, use
	 * this interface and take action when the onLogEvent() method is invoked.
	 */
	public interface LogEventHandler
	{
		/**
		 * This method is invoked when a log message is generated by the SDK. If this method returns
		 * true, the SDK assumes the application developer has fully handled all desired logging and the
		 * default logcat behavior will be skipped. If the method returns false, the SDK will still log
		 * messages to the Android log.
		 * @param eventType Type of log event, one of MASTAdLog.LOG_LEVEL_DEBUG or MASTAdLog.LOG_LEVEL_ERROR 
		 * @param message Text of message to be logged
		 * @return boolean true if logging handled by app, false if SDK should log the event
		 * <P>
		 * A sample which shows how to implement a custom log handler replacing the default SDK behavior is:
		 * <pre>
		 * class UserOnLogEventHandler implements LogEventHandler {
		 *     public void onLogEvent(int eventType, String message) {
		 *         applicationLogger(eventType, message);
		 *         return true;
		 *     }	
		 * }
		 *   
		 * adserverView.getAdDelegate().setLogEventHandler(new UserOnLogEventHandler());
		 * </pre>
		 */
		public boolean onLogEvent(int eventType, String message);
	}
	
	
	/**
	 * Interface allowing application developers to control which device features are exposed to rich media
	 * ads. By default the SDK considers hardware availability and OS level permissions to determine which
	 * features should be reported as available to rich media ads. Using this interface, the application
	 * can override this process and force features to be reported as available, or not.
	 */
	public interface FeatureSupportHandler
	{
		/**
		 * Should sending SMS messages be reported as a supported feature?
		 * @return Boolean true if this feature should be reported as a supported feature,
		 * Boolean false if it should not, or NULL if the normal SDK hardware/permission check
		 * should be performed. 
		 */
		public Boolean shouldSupportSMS();
		
		/**
		 * Should placing phone calls be reported as a supported feature?
		 * @return Boolean true if this feature should be reported as a supported feature,
		 * Boolean false if it should not, or NULL if the normal SDK hardware/permission check
		 * should be performed.
		 */
		public Boolean shouldSupportPhone();
		
		/**
		 * Should creating calendar entries by reported as a supported feature?
		 * @return Boolean true if this feature should be reported as a supported feature,
		 * Boolean false if it should not, or NULL if the normal SDK hardware/permission check
		 * should be performed.
		 */
		public Boolean shouldSupportCalendar();
		
		/**
		 * Should storing pictures to the camera roll be reported as a supported feature?
		 * @return Boolean true if this feature should be reported as a supported feature,
		 * Boolean false if it should not, or NULL if the normal SDK hardware/permission check
		 * should be performed.
		 */
		public Boolean shouldSupportStorePicture();
		
		/**
		 * Should playing video be reported as a supported feature?
		 * @return Boolean true if this feature should be reported as a supported feature,
		 * Boolean false if it should not, or NULL if the normal SDK hardware/permission check
		 * should be performed.
		 */
		public Boolean shouldShouldPlayVideo();
		
		/**
		 * Invoked when an ad intends to store a picture to the device camera role. Return boolean
		 * true indicating the user has approved storing the picture, or false otherwise.
		 * NOTE: the application developer is responsible for displaying user dialog, and associated
		 * details such as running UI code on a UI thread if needed.
		 * @param sender The originating ad view where the event was triggered.
		 * @param url String URL of image that will be downloaded and stored, if approved.
		 * @return True to allow picture storage, false otherwise.
		 */
		public Boolean shouldStorePicture(MASTAdView sender, String url);
		
		/**
		 * Invoked when an ad intends to create an event in the users' calendar. Return boolean
		 * true indicating the user has approved creating the event, or false otherwise.
		 * NOTE: the application developer is responsible for displaying user dialog, and associated
		 * details such as running UI code on a UI thread if needed.
		 * @param sender The originating ad view where the event was triggered.
		 * @param calendarProperties Complex string describing specifics of the calendar event.
		 * @return True to allow picture storage, false otherwise.
		 */
		public Boolean shouldAddCalendarEntry(MASTAdView sender, String calendarProperties);
	}
}
