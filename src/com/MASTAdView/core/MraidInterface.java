package com.MASTAdView.core;

import java.util.Iterator;
import java.util.List;

import org.apache.http.NameValuePair;
import org.json.JSONObject;

import com.MASTAdView.MASTAdLog;

import android.content.Context;


// The MraidInterface class provides the java app with an interface to manipulate the environment
// for rich media ads per the MRAID 2.0 specification.
final public class MraidInterface
{
	// Features that mraid ads can take advantage of, if this instance supports them
	public enum FEATURES
		{ SMS, TEL, CALENDAR, STORE_PICTURE, INLINE_VIDEO }
	
	// View states for an ad
	public enum STATES
		{ LOADING, DEFAULT, EXPANDED, RESIZED,  HIDDEN }

	// Events that app can fire to notify javascript of "things"
	public enum EVENTS
		{ READY, ERROR, STATE_CHANGE, VIEWABLE_CHANGE, CALENDAR_EVENT_ADDED, PICTURE_ADDED, SIZE_CHANGE }
	
	// Where an ad can be placed; inline (banner) or interstitial (full screen, transitions)
	public enum PLACEMENT_TYPES
		{ INLINE, INTERSTITIAL }
	
	// Properties that can be set/queried when using the expand method
	public enum EXPAND_PROPERTIES
		{ WIDTH, HEIGHT, USE_CUSTOM_CLOSE, IS_MODAL }

	// Options for forcing screen orientation on an expand
	public enum FORCE_ORIENTATION_PROPERTIES
		{ PORTRAIT, LANDSCAPE, NONE }
	
	// Orientation properties that can be set before / during expand/interstitial ad display
	public enum ORIENTATION_PROPERTIES
		{ ALLOW_ORIENTATION_CHANGE, FORCE_ORIENTATION }
	
	// Custom close button locations for a resize
	public enum RESIZE_CUSTOM_CLOSE_POSITION
		{ TOP_LEFT, TOP_RIGHT, TOP_CENTER, CENTER, BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM_CENTER }
	
	// Properties that can be set/queried when using the resize method
	public enum RESIZE_PROPERTIES
		{ WIDTH, HEIGHT, CUSTOM_CLOSE_POSITION, OFFSET_X, OFFSET_Y, ALLOW_OFF_SCREEN }
	
	// Current position properties
	public enum CURRENT_POSITION
		{ X, Y, WIDTH, HEIGHT }
	
	// Max ad view size properties
	public enum MAX_SIZE
		{ WIDTH, HEIGHT }
	
	// Default ad view position properties
	public enum DEFAULT_POSITION
		{ X, Y, WIDTH, HEIGHT }
	
	// Screen size properties
	public enum SCREEN_SIZE
		{ WIDTH, HEIGHT }

	// Calendar properties as specified by W3C used when creating a calendar event
	public enum CALENDAR_EVENT_PARAMETERS
		{ DESCRIPTION, LOCATION, SUMMARY, START, END }

	
	// "action" values to use when firing error events for MRAID
	public static final String MRAID_ERROR_ACTION_RESIZE		= "resize";
	public static final String MRAID_ERROR_ACTION_CLOSE			= "close";
	public static final String MRAID_ERROR_ACTION_HIDE			= "hide";
	public static final String MRAID_ERROR_ACTION_EXPAND		= "expand";
	public static final String MRAID_ERROR_ACTION_OPEN			= "open";
	public static final String MRAID_ERROR_ACTION_PLAYVIDEO		= "playVideo";
	public static final String MRAID_ERROR_ACTION_CREATE_EVENT	= "createCalendarEvent";
	public static final String MRAID_ERROR_ACTION_SET_ORIENTATION_PROPERTIES = "setOrientationProperties";
	
	private PLACEMENT_TYPES adPlacementType;
	final private AdViewContainer adView;
	final private AdWebView webView;
	final private Context context;
	private STATES adState;
	private DeviceFeatures deviceFeatures;
	
	
	public MraidInterface(AdViewContainer container, AdWebView webView)
	{
		adView 			= container;
		this.webView 	= webView; 
		context 		= adView.getContext();
		adState 		= STATES.LOADING;
		adPlacementType = MraidInterface.PLACEMENT_TYPES.INLINE;
	}
	

	//
	// Set supported features
	//
	
	
	synchronized public void setDeviceFeatures()
	{
		deviceFeatures = new DeviceFeatures(context, adView);

		String name;
		for (MraidInterface.FEATURES feature : MraidInterface.FEATURES.values())
		{ 
			name = MraidInterface.get_FEATURES_name(feature);
			webView.injectJavaScript("mraid.setSupports(\"" + name + "\", " + deviceFeatures.isSupported(feature) + ");");
		} 	
	}

	
	synchronized public DeviceFeatures getDeviceFeatures()
	{
		return deviceFeatures;
	}
	
	
	//
	// MRAID State
	//
	
	
	public STATES getState()
	{
		synchronized (this)
		{
			return adState;
		}
	}
	
	
	private void setStateInternal(STATES state)
	{
		synchronized (this)
		{
			adState = state;
		}
	}
	
	
	//
	// MRAID event methods that app can invoke
	//
	
	
	// Tell ad that an SDK side error has occurred; message describes error, action optionally indicates name of action causing error
	public void fireErrorEvent(String message, String action)
	{
		//String name = get_EVENTS_name(EVENTS.ERROR);
		adView.getLog().log(MASTAdLog.LOG_LEVEL_DEBUG, "MraidInterface.errorEvent", message + ":" + action);
		webView.injectJavaScript("mraid.fireErrorEvent(\"" + message + "\",\"" + action + "\");");
	}
	
		
	// Tell ad that environment is ready, and change state from loading to default
	public void fireReadyEvent()
	{
		// latest spec has switched the order so that state change goes before ready event...
		setState(STATES.DEFAULT);
		
		String name = get_EVENTS_name(EVENTS.READY);
		webView.injectJavaScript("mraid.fireEvent(\"" + name + "\");");
	}
	
		
	// Tell ad that view size has changed, e.g. after an orientation change
	public void fireSizeChangeEvent(int width, int height)
	{
		//String name = get_EVENTS_name(EVENTS.SIZE_CHANGE);
		width = AdSizeUtilities.devicePixelToMraidPoint(width, context);
		height = AdSizeUtilities.devicePixelToMraidPoint(height, context);
		webView.injectJavaScript("mraid.fireSizeChangeEvent(" + width + ", " + height + ");");
	}
		
	
	// Tell ad that a picture was added
	public void firePictureAddedEvent(Boolean success)
	{
		String name = get_EVENTS_name(EVENTS.PICTURE_ADDED);
		webView.injectJavaScript("mraid.fireAddedEvent(\"" + name + "\"," + success.toString() + ");");
	}
	
	
	// Tell ad that a picture was added
	public void fireCalendarAddedEvent(Boolean success)
	{
		String name = get_EVENTS_name(EVENTS.CALENDAR_EVENT_ADDED);
		webView.injectJavaScript("mraid.fireAddedEvent(\"" + name + "\"," + success.toString() + ");");
	}
	
	
	//
	// Mraid methods for use by app
	//
	
	
	public void setPlacementType(PLACEMENT_TYPES type)
	{
		synchronized (this)
		{
			adPlacementType = type;
		}
		String name = get_PLACEMENT_TYPES_name(type);
		webView.injectJavaScript("mraid.setPlacementType(\"" + name + "\");");
	}
		
	
	public PLACEMENT_TYPES getPlacementType()
	{
		PLACEMENT_TYPES t;
		
		synchronized (this)
		{
			t = adPlacementType;
		}
		
		return t;
	}
	
	
	public void setState(STATES toState)
	{
		String stateName = get_STATES_name(toState);
		webView.injectJavaScript("mraid.setState(\"" + stateName + "\");");
		setStateInternal(toState);
	}
	
	
	public void setViewable(Boolean isViewable)
	{
		String stateName = isViewable.toString();
		webView.injectJavaScript("mraid.setViewable(\"" + stateName + "\");");
	}

	
	synchronized public void setOrientationProperties(List<NameValuePair> list)
	{
		setPropertiesFromList("Orientation", list);
	}
	
	
	synchronized public void setExpandProperties(List<NameValuePair> list)
	{
		setPropertiesFromList("Expand", list);
	}
	
	
	synchronized public void setResizeProperties(List<NameValuePair> list)
	{
		setPropertiesFromList("Resize", list);
	}
	
	
	synchronized public void setCurrentPosition(int x, int y, int width, int height)
	{
		x = AdSizeUtilities.devicePixelToMraidPoint(x, context);
		y = AdSizeUtilities.devicePixelToMraidPoint(y, context);
		width = AdSizeUtilities.devicePixelToMraidPoint(width, context);
		height = AdSizeUtilities.devicePixelToMraidPoint(height, context);
		
		try
		{
			JSONObject position = new JSONObject();
			position.put(MraidInterface.get_CURRENT_POSITION_name(MraidInterface.CURRENT_POSITION.X), "" + x);
			position.put(MraidInterface.get_CURRENT_POSITION_name(MraidInterface.CURRENT_POSITION.Y), "" + y);
			position.put(MraidInterface.get_CURRENT_POSITION_name(MraidInterface.CURRENT_POSITION.WIDTH), "" + width);
			position.put(MraidInterface.get_CURRENT_POSITION_name(MraidInterface.CURRENT_POSITION.HEIGHT), "" + height);
			
			webView.injectJavaScript("mraid.setCurrentPosition(" + position.toString() + ");");
		}
		catch (Exception ex)
		{
			adView.getLog().log(MASTAdLog.LOG_LEVEL_ERROR, "JavascriptInterface setCurrentPosition - exception", ex.getMessage()); 
		}		
	}
	
	
	
	
	/*
	synchronized public void setOrientation(int value)
	{
		webView.injectJavaScript("mraid.setOrientation(" + value + ");");
	}
	*/
	
	
	//
	// Utilities
	//
	
	
	private void setPropertiesFromList(String name, List<NameValuePair> list)
	{
		if ((list == null) || (list.isEmpty()))
		{
			return;
		}
		
		// Create JSON object containing data
		JSONObject json = new JSONObject();
		
		Iterator<NameValuePair> i = list.iterator();
		NameValuePair nvp;
		while (i.hasNext())
		{
			nvp = i.next();
			if ((nvp != null) && (nvp.getName() != null))
			{
				try
				{
					json.put(nvp.getName(), nvp.getValue());
				}
				catch(Exception ex)
				{
					adView.getLog().log(MASTAdLog.LOG_LEVEL_ERROR, "JavascriptInterface setPropertiesFromList - exception", ex.getMessage());
					return;
				}
			}
		}
		
		String function = "mraid.set" + name + "Properties(";
		webView.injectJavaScript(function + json.toString() + ");"); // the JSON string format "just works" for objects in javascript. yay!
	}
	
	
	public void close()
	{
		//System.out.println("MraidInterface: close");
		webView.injectJavaScript("mraid.close();");
	}
	
	
	//
	// Utility methods to convert enum values to strings matchng expected names on the javascript layer
	//
	
	
	// For a given value, return name expected by javascript library
	public static String get_FEATURES_name(FEATURES f)
	{
		switch(f)
		{
		case STORE_PICTURE:
			return "storePicture";
		case INLINE_VIDEO:
			return "inlineVideo";
		default:
			return f.toString().toLowerCase();	
		}
	}
	
	
	// For a given value, return name expected by javascript library
	public static String get_STATES_name(STATES s)
	{
		return s.toString().toLowerCase();
	}
	
	
	public static String get_EVENTS_name(EVENTS e)
	{
		switch(e)
		{
		case STATE_CHANGE:
			return "stateChange";
		case VIEWABLE_CHANGE:
			return "viewableChange";
		case CALENDAR_EVENT_ADDED:
			return "calendarEventAdded";
		case PICTURE_ADDED:
			return "pictureAdded";
		case SIZE_CHANGE:
			return "sizeChange";
		default:
			return e.toString().toLowerCase();
		}
	}
	
	
	public static String get_PLACEMENT_TYPES_name(PLACEMENT_TYPES p)
	{
		return p.toString().toLowerCase();
	}

	
	public static String get_FORCE_ORIENTATION_PROPERTIES_name(FORCE_ORIENTATION_PROPERTIES e)
	{
		return e.toString().toLowerCase();
	}

	
	public static String get_ORIENTATION_PROPERTIES_name(ORIENTATION_PROPERTIES o)
	{
		switch(o)
		{
		case ALLOW_ORIENTATION_CHANGE:
			return "allowOrientationChange";
		case FORCE_ORIENTATION:
			return "forceOrientation";
		default:
				return o.toString().toLowerCase();
		}
	}
	
	public static FORCE_ORIENTATION_PROPERTIES get_FORCE_ORIENTATION_PROPERTIES_by_name(String forceOrientationPropertiesName)
	{
		if (forceOrientationPropertiesName != null)
		{
			for (FORCE_ORIENTATION_PROPERTIES p : FORCE_ORIENTATION_PROPERTIES.values())
			{
				if (get_FORCE_ORIENTATION_PROPERTIES_name(p).equals(forceOrientationPropertiesName))
					return p;
			}
		}
		
		return FORCE_ORIENTATION_PROPERTIES.NONE;
	}	
	
	public static String get_EXPAND_PROPERTIES_name(EXPAND_PROPERTIES e)
	{
		switch(e)
		{
		case USE_CUSTOM_CLOSE:
			return "useCustomClose";
		case IS_MODAL:
			return "isModal";
		default:
				return e.toString().toLowerCase();
		}
	}
	
	
	public static RESIZE_CUSTOM_CLOSE_POSITION get_RESIZE_CUSTOM_CLOSE_POSITION_by_name(String positionName)
	{
		if (positionName != null)
		{
			for (RESIZE_CUSTOM_CLOSE_POSITION p : RESIZE_CUSTOM_CLOSE_POSITION.values())
			{
				if (get_RESIZE_CUSTOM_CLOSE_POSITION_name(p).equals(positionName))
					return p;
			}
		}
		
		return RESIZE_CUSTOM_CLOSE_POSITION.TOP_RIGHT; // default position, or null for error?
	}
	
	
	public static String get_RESIZE_CUSTOM_CLOSE_POSITION_name(RESIZE_CUSTOM_CLOSE_POSITION r)
	{
		return r.toString().toLowerCase().replace("_", "-");
	}
	
	
	public static String get_RESIZE_PROPERTIES_name(RESIZE_PROPERTIES r)
	{
		switch(r)
		{
		case CUSTOM_CLOSE_POSITION:
			return "customClosePosition";
		case OFFSET_X:
			return "offsetX";
		case OFFSET_Y:
			return "offsetY";
		case ALLOW_OFF_SCREEN:
			return "allowOffscreen";
		default:
			return r.toString().toLowerCase();
		}
	}
	
	
	public static String get_CURRENT_POSITION_name(CURRENT_POSITION c)
	{
		return c.toString().toLowerCase();
	}
	
	
	public static String get_MAX_SIZE_name(MAX_SIZE m)
	{
		return m.toString().toLowerCase();
	}
	
	
	public static String get_DEFAULT_POSITION_name(DEFAULT_POSITION d)
	{
		return d.toString().toLowerCase();
	}
	
	
	public static String get_SCREEN_SIZE_name(SCREEN_SIZE s)
	{
		return s.toString().toLowerCase();
	}
	
	
	public static String get_CALENDAR_EVENT_PARAMETERS_name(CALENDAR_EVENT_PARAMETERS c)
	{
		return c.toString().toLowerCase();
	}
}
