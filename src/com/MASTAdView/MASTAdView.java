//
// Copyright (C) 2011, 2012 Mocean Mobile. All Rights Reserved. 
//
package com.MASTAdView;


import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.MASTAdView.core.AdViewContainer;
import com.MASTAdView.core.AdWebView;


/**
 * Ad view object, acting as a container for an ad view control. This is the main object developers
 * use when integrating the SDK into an application. This class (and the related public classes
 * in this documentation set) make up the public interfaces which developers can safely interact with.
 * Classes and methods not defined in this documentation may change.
 * <P>
 * The ad publisher site and zone must be configured to fetch and display an ad. Ads can range from
 * simple text up to rich media ads with MRAID 1 or 2 javascript content. 
 * <P>
 * A variety of optional properties can be set that assist the back-end in delivering content targeted to the app user.
 * These includes the publisher site and zone. These and other named parameters are managed via the MASTAdRequest object.
 * See the documentation for MASTAdRequest for more information about these parameters, which are passed to the back-end
 * when requesting ad content.
 * <P>
 * Related to this are several properties which control on-device ad behavior, such as enabling automatic timed based ad
 * updates (via the setUpdateTime() method), controlling diagnostic logging (via the setLogLevel() method), controlling
 * use of GPS location detection to serve ads in some proximity to the user (via the setLocationDetection() method), and
 * other described in this documentation.
 * <P>
 * In addition, a number of properties control the display and behavior of ad content after it has been delivered to
 * the SDK. These parameters are manipulated through methods in this class itself; examples are the showCloseButton()
 * method and others documented here. Finally, the developer can obtain references to the actual text, image or web
 * view object that will display ads with the getAdTextView(), getAdImageView() or getAdWebView() methods. With these
 * objects the full range of standard Android view properties are available for manipulation (for advanced developers only!)
 */
public class MASTAdView extends AdViewContainer
{
	/**
	 * Create ad view/container for displaying ads obtained through this SDK. This is the normal signature
	 * a developer uses when setting up inline (banner) ad views using a code-only approach. Use the update()
	 * method to fetch ad content after the view is configured.
	 * @param context - The reference to the context of Activity; NOTE a context object will work for most purposes, but some device specific tasks (such as orientation or location handling) only work if this is an actual activity reference.
	 * @param site - The id of the publisher site (presets the standard ad request site property).
	 * @param zone - The id of the zone of publisher site  (presets the standard ad request zone property).
	 */
	public MASTAdView(Context context, Integer site, Integer zone)
	{
		super(context, site, zone);
	}

	
	/**
	 * Create ad view/container for displaying ads obtained through this SDK. This is the normal signature
	 * a developer uses when setting up interstitial (full screen transitional) ad views using a code-only
	 * approach. Use the update() method to fetch ad content after the view is configured, and then the
	 * showInterstitial() method to display the ad.
	 * @param context - The reference to the context of Activity; NOTE a context object will work for most purposes, but some device specific tasks (such as orientation or location handling) only work if this is an actual activity reference.
	 * @param site - The id of the publisher site (presets the standard ad request site property).
	 * @param zone - The id of the zone of publisher site  (presets the standard ad request zone property).
	 * @param isInterstitial - Flag indicating this will be used for an interstitial ad placement (if true).
	 */
	public MASTAdView(Context context, Integer site, Integer zone, boolean isInterstitial)
	{
		super(context, site, zone, isInterstitial);
	}
	
	
	/**
	 * Create ad view/container for displaying ads obtained through this SDK.
	 * This signature is used when creating an ad view from an XML template.
	 * It is not necessary to manually invoke update() in this case, the SDK performs an implicit update for ad views created via an XML layout.
	 * @param context The reference to the context of Activity; NOTE a context object will work for most purposes, but some device specific tasks (such as orientation or location handling) only work if this is an actual activity reference.
	 * @param attrs XML layout attribute parameter
	 * @param defStyle XML layout default style parameter
	 */
	public MASTAdView(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
	}

	
	/**
	 * Create ad view/container for displaying ads obtained through this SDK.
	 * This signature is used when creating an ad view from an XML template.
	 * It is not necessary to manually invoke update() in this case, the SDK performs an implicit update for ad views created via an XML layout.
	 * @param context The reference to the context of Activity; NOTE a context object will work for most purposes, but some device specific tasks (such as orientation or location handling) only work if this is an actual activity reference.
	 * @param attrs XML layout attribute parameter
	 */
	public MASTAdView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
	}


	/**
	 * Create ad view/container for displaying ads obtained through this SDK.
	 * This signature is used when creating an ad view from an XML template.
	 * It is not necessary to manually invoke update() in this case, the SDK performs an implicit update for ad views created via an XML layout.
	 * @param context The reference to the context of Activity; NOTE a context object will work for most purposes, but some device specific tasks (such as orientation or location handling) only work if this is an actual activity reference.
	 */
	public MASTAdView(Context context)
	{
		super(context);
	}

	
	/**
	 * Request new ad content from the back-end immediately, canceling any outstanding request for content
	 * from this ad view. The network transactions will be performed in a background (non-UI) thread to
	 * prevent slowing down or locking up the application UI, and once new content is available the display
	 * of the ad will automatically run on the UI thread again.
	 */
	public void update()
	{
		super.update();
	}

	
	/**
	 * Request new ad content from the back-end immediately, canceling any outstanding request for content
	 * from this ad view, and set a refresh interval (in seconds) for fetching new ads if the ad view remains idle.
	 * The network transactions will be performed in a background (non-UI) thread to prevent slowing down or locking up
	 * the application UI, and once new content is available the display of the ad will automatically run on the UI thread again.
	 * @param interval Ad refresh interval (in seconds), same as invoking setUpdateTime().
	 */
	public void updateWithInterval(int interval)
	{
		super.setUpdateTime(interval);
		super.update();
	}

	
	/**
	 * Set ad refresh interval (in seconds). Once an ad has finished loading, the timer starts
	 * and a new ad will be loaded after this amount of time has elapsed. Default 120 seconds.
	 * If 0, ads are not updated automatically (use the update() method for a manual update.)
	 * If less than 0, the default value will be used instead.
	 */
	public void setUpdateTime(int updateTime)
	{
		super.setUpdateTime(updateTime);
	}
	
	
	/**
	 *  Show interstitial ad, which appears as a full screen and will popup over top of the current application activity.
	 *  A standard close button will be included as specified by the MRAID standard.
	 *  NOTE: you do not need to add the view to a layout when used for interstitial ads.
	 *  NOTE: can only be used for ad views created with the isInterstitial property set to true.
	 */
	public void showInterstitial()
	{
		super.showInterstitial(0);
	}
	
	
	/**
	 *  Show interstitial ad, which appears as a full screen and will popup over top of the current application activity,
	 *  and then automatically close the interstitial ad after duration seconds have elapsed.
	 *  A standard close button will be included as specified by the MRAID standard.
	 *  NOTE: you do not need to add the view to a layout when used for interstitial ads.
	 *  NOTE: can only be used for ad views created with the isInterstitial property set to true.
	 */
	public void showInterstitial(int withDuration)
	{
		super.showInterstitial(withDuration);
	}
	
	
	/**
	 * Close an interstitial ad view.
	 * NOTE: can only be used for ad views created with the isInterstitial property set to true.
	 */
	public void closeInterstitial()
	{
		super.closeInterstitial();
	}
	

	/**
	 * Get reference to the web view object used when displaying rich media ads. Advanced developers can use this to
	 * customize the display and behavior of that control with the full range of standard view attributes supported
	 * by the Android system. Do not alter this object unless you know what you are doing!
	 */
	public AdWebView getAdWebView()
	{
		return super.getAdWebView();
	}
	
	
	/**
	 * Get reference to the view object used when displaying image only ads. Advanced developers can use this to
	 * customize the display and behavior of that control with the full range of standard view attributes supported
	 * by the Android system. Do not alter this object unless you know what you are doing!
	 */
	public View getAdImageView()
	{
		return super.getAdImageView();
	}
	
	
	/**
	 * Get reference to the text view object used when displaying text only ads. Advanced developers can use this to
	 * customize the display and behavior of that control with the full range of standard view attributes supported
	 * by the Android system. Do not alter this object unless you know what you are doing!
	 */
	public TextView getAdTextView()
	{
		return super.getAdTextView();
	}
	
	
	/**
	 * Get the user-agent string that will be sent to back-end ad server and to tracking URL servers to report when an ad
	 * has been displayed. This is normally the same string used by the underlying web browser on the device.
	 */
	public String getUserAgent()
	{
		return super.getUserAgent();
	}

	
	/**
	 * Get reference to diagnostics logging object used internal to the SDK.
	 * @see MASTAdLog See the MASTAdLog class for more information about logging. 
	 */
	public MASTAdLog getAdLog()
	{
		return super.getLog();
	}
	
	
	/**
	 * Get ad server request object which allows customizing parameters (such as the site or zone)
	 * that will be sent to the back-end ad server when fetching ads; for example, to set the
	 * zone, use: "getAdRequest().setProperty(MASTAdRequest.parameter_zone, myNewZone);"
	 * @see MASTAdRequest See the MASTAdRequest class for more information about working
	 * with this object, and the available parameters which you can customize. 
	 * @return Object encapsulating parameter sent to back-end when requesting an ad.
	 */
	public MASTAdRequest getAdRequest()
	{
		return super.getAdRequest();
	}

	
	/**
	 * Get ad delegate object which developers can use to extend and/or interact with
	 * feature and functions of the SDK ad handling, such as ad download callbacks,
	 * rich media event notifications, etc.
	 * @see MASTAdDelegate See the MASTAdDelegate class for more information.
	 * @return Object encapsulating various delegate interfaces and get/set methods.
	 */
	public MASTAdDelegate getAdDelegate()
	{
		return super.getAdDelegate();
	}

	
	/**
	 * Get URL string used to request the last ad from back-end.
	 */
	public String getLastRequest()
	{
		return super.getLastRequest();
	}
	
	
	/**
	 * Return response data (body) returned for the last ad received from the back-end.
	 */
	public String getLastResponse()
	{
		return super.getLastResponse();
	}
	
	
	/**
	 * Turn location detection on or off. If enabled, the device GPS location capabilities will be used to obtain
	 * a position fix at least one. Ongoing location updates may continue depending on the minimum wait and minimum
	 * distance settings. If user-specified latitude and/or longitude values have been set, and location detection
	 * is enabled, the detected location will override preset values.
	 * @param detect If true, location detection is enabled.
	 * @param minWaitMillis Set minimum time between location updates from the GPS system, in milliseconds; default 0 (no recurring updates.)
	 * @param minMoveMeters Set minimum distance (in meters) location must change for app to be notified of new location from the GPS system. Use 0 (the default) if no distance based updates are desired.
	 */
	public void setLocationDetection(boolean detect, Integer minWaitMillis, Float minMoveMeters)
	{
		super.setLocationDetection(detect, minWaitMillis, minMoveMeters);
	}

	
	/**
	 * Turn location detection on or off. If enabled, the device GPS location capabilities will be used to obtain
	 * a position fix at least one. Ongoing location updates may continue, based on default minimum wait and minimum
	 * distance settings. If user-specified latitude and/or longitude values have been set, and location detection
	 * is enabled, the detected location will override preset values.
	 * @param detect If true, location detection is enabled.
	 */
	public void setLocationDetection(boolean detect)
	{
		super.setLocationDetection(detect, MASTAdConstants.DEFAULT_LOCATION_REPEAT_WAIT, MASTAdConstants.DEFAULT_LOCATION_REPEAT_DISTANCE);
	}
	
	
	/**
	 * Return current location detection state.
	 * NOTE: This may be flase even if previously enabled, if device permissions/features do not support location detection.
	 */
	public boolean getLocationDetection()
	{
		return super.getLocationDetection();
	}
	

	/**
	 * Provide a custom close button setup by the application for display on ad views in place of the default button
	 * created by the SDK.
	 */
	public void setCustomCloseButton(Button closeButton)
	{
		super.setCustomCloseButton(closeButton);
	}

	
	/**
	 * Get reference to any configured custom close button previously setup by the application developer.
	 * @return Button custom close button object
	 */
	public Button getCustomCloseButton()
	{
		return super.getCustomCloseButton();
	}
	
	
	/**
	 * Control if a close button will be displayed on a banner ad, or not.
	 * Default is false. The setting applies for all subsequent updates.
	 */
	public void showCloseButton(boolean flag)
	{
		super.showCloseButton(flag, 0);
	}
	
	
	/**
	 * Control if a close button will be displayed on a banner ad or not, and specify a
	 * delay (in seconds) before which the button becomes visible. 
	 * Default is false, and no delay. The setting applies for all subsequent updates.
	 */
	public void showCloseButton(int afterDelay)
	{
		super.showCloseButton(true, afterDelay);
	}
	
	
	/**
	 * When URLs are opened as a result of a banner clikc (or the rich media open method) should an internal
	 * SDK browser be used (true) or the default browser application configured on the device (false).
	 */
	public void setUseInternalBrowser(boolean flag)
	{
		super.setUseInternalBrowser(flag);
	}
	
	
	/**
	 * Get current use internal browser setting.
	 * @return True if intenral browser is to be used, false if default device browser application.
	 */
	public boolean getUseInternalBrowser()
	{
		return super.getUseInternalBrowser();
		
	}
	
	
	/**
	 * Clear current ad content and close any expanded/resize views.
	 */
	public void removeContent()
	{
		super.removeContent();
	}
	
	
	/**
	 * Clear current ad content and reset all settings to default. Afer this, the ad view is essentially "new" and must be configured again before use.
	 */
	public void reset()
	{
		super.reset();
	}
	
	
	/**
	 * Set (optional) image resource which will be shown during ad loading if there is no ad content already visible.
	 * @param resource Resource identifier for default image.
	 */
	public void setDefaultImageResource(Integer resource)
	{
		super.setDefaultImageResource(resource);
	}
	
	
	/**
	 * Get any default image resource, if one has been configured.
	 * @return Default image resource id, or NULL if none has been configured by the application.
	 */
	public Integer getDefaultImageResource()
	{
		return super.getDefaultImageResource();
	}
}
