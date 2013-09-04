//
// Copyright (C) 2011, 2012 Mocean Mobile. All Rights Reserved. 
//
package com.MASTAdView.core;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.MASTAdView.MASTAdConstants;
import com.MASTAdView.MASTAdDelegate;
import com.MASTAdView.MASTAdLog;
import com.MASTAdView.MASTAdView;
import com.MASTAdView.core.AdDialogFactory.DialogOptions;


final public class AdSizeUtilities
{
	final private int								CloseControlSize = 50;
	
	final private AdViewContainer 					parentContainer;
	final private MASTAdLog 						adLog;
	volatile private DisplayMetrics					metrics;
	
	volatile private AdWebView 						expandedAdView = null;
	
	final private AdDialogFactory					adDialogFactory;
	final private Context							context;
	
	private AdClickHandler 							adClickHandler = null;

	// Properties used to resize and restore ad view
	private View 									resizeOldContent = null;
	private ViewGroup 								resizeDecorView = null;
	private int 									resizeOldContentIndex = 0;
	private Button 									resizeCloseButton = null;
	
	// Expand and pre-expand properties
	private int 										preExpandRequestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
	private boolean 									expandAllowOrientationChange = true;
	private MraidInterface.FORCE_ORIENTATION_PROPERTIES expandForceOrientation = MraidInterface.FORCE_ORIENTATION_PROPERTIES.NONE;
	
	public AdSizeUtilities(AdViewContainer adContainer, DisplayMetrics metrics)
	{
		parentContainer = adContainer;
		this.metrics = metrics;
	
		adLog = parentContainer.getLog();
		context = adContainer.getContext();
		
		// Setup ad dialog factory for interstitial/exapnded/open use
		adDialogFactory = new AdDialogFactory(context, parentContainer);
		
		adClickHandler = new AdClickHandler(parentContainer);
	}

	
	public void setMetrics(DisplayMetrics metrics)
	{
		this.metrics = metrics;
	}
	
	
	// Get content from provided URL; NOTE: This runs in the current thread context;
	// never call this from the UI thread!!!
	private StringBuffer fetchUrl(String url)
	{
		StringBuffer responseValue = new StringBuffer();
		
		try
		{
			DefaultHttpClient client = new DefaultHttpClient();
			HttpGet get = new HttpGet(url);
			get.addHeader("Connection","close");
			
			HttpResponse response = client.execute(get);
			if(response.getStatusLine().getStatusCode() == 200)
			{
				HttpEntity entity = response.getEntity();
				responseValue.append(EntityUtils.toString(entity, "UTF-8"));
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "fetchUrl", e.getMessage());
			responseValue.setLength(0);
		}
	
		return responseValue;
	}
	
	
	private void expandedCallback(int height, int width)
	{
		MASTAdDelegate delegate = parentContainer.getAdDelegate();
		if (delegate != null)
		{
			MASTAdDelegate.AdActivityEventHandler activityHandler = delegate.getAdActivityEventHandler();
			if (activityHandler != null)
			{
				activityHandler.onAdExpanded((MASTAdView)parentContainer, height, width);
			}
		}
	}
	
	
	// Display URL in a (non-MRAID) web view after fetching content using background thread
	synchronized public String openInBackgroundThread(final AdDialogFactory.DialogOptions options, final String url)
	{
		//final StringBuffer responseValue = new StringBuffer();
		
		Thread fetchUrl = new Thread()
		{
			public void run()
			{
				adClickHandler.openUrlForBrowsing(parentContainer.getContext(), url);
			}
		};
		fetchUrl.setName("[AdSizeUtilities] openInBackgroundThread");
		fetchUrl.start();
		
		return null;
	}

	
	synchronized public String startExpand(Bundle data, AdReloadTimer timer)
	{
		// Get all expand parameters from data bundle
		Integer toWidth 		   = null;
		Integer toHeight 		   = null;
		Boolean customClose		   = null;
		//Boolean isModal			   = null; // this is read only, always true per spec
		String  forceOrientation   = null;
		String  url				   = null;
		try
		{
			String value = data.getString(MraidInterface.get_EXPAND_PROPERTIES_name(MraidInterface.EXPAND_PROPERTIES.WIDTH));
			if (value != null)
			{
				toWidth = Integer.parseInt(value);
			}
			else
			{
				toWidth = metrics.widthPixels;
			}
			
			value = data.getString(MraidInterface.get_EXPAND_PROPERTIES_name(MraidInterface.EXPAND_PROPERTIES.HEIGHT));
			if (value != null)
			{
				toHeight = Integer.parseInt(value);
			}
			else
			{
				toHeight = metrics.heightPixels;
			}	
			
			value = data.getString(MraidInterface.get_EXPAND_PROPERTIES_name(MraidInterface.EXPAND_PROPERTIES.USE_CUSTOM_CLOSE));
			if ((value != null) && (value.equalsIgnoreCase("true")))
			{
				customClose = true;
			}
			else
			{
				customClose = false; // default
			}
			
			value = data.getString(MraidInterface.get_ORIENTATION_PROPERTIES_name(MraidInterface.ORIENTATION_PROPERTIES.ALLOW_ORIENTATION_CHANGE));
			if ((value != null) && (value.equalsIgnoreCase("false")))
			{
				expandAllowOrientationChange = false;
			}
			else
			{
				expandAllowOrientationChange = true; // default
			}
			
			forceOrientation = data.getString(MraidInterface.get_ORIENTATION_PROPERTIES_name(MraidInterface.ORIENTATION_PROPERTIES.FORCE_ORIENTATION));
			if (forceOrientation == null)
			{
				expandForceOrientation = MraidInterface.FORCE_ORIENTATION_PROPERTIES.NONE;
			}
			else
			{
				expandForceOrientation = MraidInterface.get_FORCE_ORIENTATION_PROPERTIES_by_name(forceOrientation);
			}
			
			url = data.getString(AdMessageHandler.EXPAND_URL);
		}
		catch(Exception ex)
		{
			return MASTAdConstants.STR_RICHMEDIA_ERROR_EXPAND; // XXX new, more specific error
		}
	
		// Limit expand size to device width/height at most
		if ((toWidth < 0) || (toWidth > metrics.widthPixels))
		{
			toWidth = metrics.widthPixels; 
		}
		if ((toHeight < 0) || (toHeight > metrics.heightPixels))
		{
			toHeight = metrics.heightPixels;
		}
		//System.out.println("startExpand: to h/w = " + toHeight + "/" + toWidth);
		
		preExpandRequestedOrientation = ((Activity)context).getRequestedOrientation();

		// Pass options for dialog through to creator
		AdDialogFactory.DialogOptions options = new AdDialogFactory.DialogOptions();
		options.backgroundColor = Color.BLACK; // XXX setting?
		options.customClose = customClose;
		options.height = toHeight;
		options.width = toWidth;
		
		options.dismissRunnable = new Runnable()
		{
			public void run()
			{
				parentContainer.getAdWebView().getMraidInterface().close();
			}
		};
		
		timer.stopTimer(false); // stop ad refresh timer		
		
		parentContainer.getAdWebView().getMraidInterface().setState(MraidInterface.STATES.EXPANDED);
		
		if ((url == null) || (url.length() < 1) || (url.equalsIgnoreCase("undefined")))
		{
			// We are using existing ad view / content, safe to do this on UI thread
			return expandInUIThread(options);
		}
		else
		{
			// Two part creative, need to fetch new data, must use non-UI thread for that;
			// after data available will call back to finish on ui thread via handler.
			return expandInBackgroundThread(options, url);
		}	
	}
	
	
	synchronized public String setOrientationProperties(Bundle data)
	{
		try
		{
			String value = data.getString(MraidInterface.get_ORIENTATION_PROPERTIES_name(MraidInterface.ORIENTATION_PROPERTIES.ALLOW_ORIENTATION_CHANGE));
			if ((value != null) && (value.equalsIgnoreCase("false")))
			{
				expandAllowOrientationChange = false;
			}
			else
			{
				expandAllowOrientationChange = true; // default
			}
			
			String forceOrientation = data.getString(MraidInterface.get_ORIENTATION_PROPERTIES_name(MraidInterface.ORIENTATION_PROPERTIES.FORCE_ORIENTATION));
			if (forceOrientation == null)
			{
				expandForceOrientation = MraidInterface.FORCE_ORIENTATION_PROPERTIES.NONE;
			}
			else
			{
				expandForceOrientation = MraidInterface.get_FORCE_ORIENTATION_PROPERTIES_by_name(forceOrientation);
			}
		}
		catch(Exception ex)
		{
			return MASTAdConstants.STR_RICHMEDIA_ERROR_EXPAND; // XXX new, more specific error
		}
		
		updateOrientation();
		
		return null;
	}
	
	
	// MRAID expand for one-part creative where we use existing ad web view / content (so all done in UI thread)
	private String expandInUIThread(AdDialogFactory.DialogOptions options)
	{	
		expandedCallback(options.height, options.width);
		
		expandedAdView = null;
		Dialog dialog = adDialogFactory.createDialog(parentContainer.getAdWebView(), options);
		dialog.show();
	
		// Apply orientation options
		updateOrientation();
		
		return null;
	}

	// MRAID expand for two-part creative; fetch content from URL and display in new (MRAID) ad web view
	private String expandInBackgroundThread(final AdDialogFactory.DialogOptions options, final String url)
	{
		/*boolean dontLoad = false;
		if (URL == null || URL.equals("undefined")) {
			URL = getUrl();
			dontLoad = true;
		}*/

		
		//System.out.println("Expand: two part creative, next url=" + url);

		final StringBuffer responseValue = new StringBuffer();
		
		Thread fetchUrl = new Thread()
		{
			public void run()
			{
				responseValue.append(fetchUrl(url));
				if (responseValue.length() > 0)
				{
					final String dataOut = parentContainer.setupViewport(false, responseValue.toString());
					
					// Now that we have the data, get back on UI thread to display it...
					parentContainer.getHandler().post(new Runnable()
					{
						public void run()
						{
							try
							{
								expandedAdView = parentContainer.createWebView(context);
								expandedAdView.setVisibility(View.VISIBLE);
								//expandedAdView.loadDataWithBaseURL(null, responseValue.toString(), "text/html", "UTF-8", null);
								expandedAdView.loadDataWithBaseURL(null, dataOut, "text/html", "UTF-8", null);
								
								// Base ad view state already set to expanded, but per spec this new ad view should also
								// be set to expanded after it loads.
								expandedAdView.getMraidInterface().setState(MraidInterface.STATES.EXPANDED);
								
								expandedCallback(options.height, options.width);
								
								Dialog dialog = adDialogFactory.createDialog(expandedAdView, options);
								
								dialog.show();
								
								// Apply orientation options
								updateOrientation();								
							}
							catch(Exception ex)
							{
								adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "expandInBackgroundThread", ex.getMessage());
								parentContainer.getAdWebView().getMraidInterface().fireErrorEvent(ex.getMessage(), "expand");
							}
						}
					});
				}
			}
		};
		fetchUrl.setName("[AdSizeUtilities] expandInBackgroundThread");
		fetchUrl.start();
		
		return null;
	}

	
	private void updateOrientation()
	{
		if ((context instanceof Activity) == false)
			return;
		
		// Force as requested.  Note that a force without being locked may not work as the creative intended.
		switch (expandForceOrientation)
		{
		case PORTRAIT:
			((Activity)context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			break;

		case LANDSCAPE:
			((Activity)context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			break;
		
		default:
			break;
		}
		
		// Lock takes precedence over force.  So it's possible force will be meaningless without being locked.
		if (expandAllowOrientationChange == true)
		{
			((Activity)context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		}
		else
		{
			if (expandForceOrientation == MraidInterface.FORCE_ORIENTATION_PROPERTIES.NONE)
			{
				int currentOrientation = ((Activity)context).getResources().getConfiguration().orientation;
				
				switch (currentOrientation)
				{
				case Configuration.ORIENTATION_PORTRAIT:
					((Activity)context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
					break;
				case Configuration.ORIENTATION_LANDSCAPE:
					((Activity)context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
					break;
				default:
					((Activity)context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
					break;
				}
			}
		}
	}
	
	private String resizePropertiesValid(int toWidth, int toHeight, String closePosition, int offsetX, int offsetY, boolean offscreen)
	{
		AdWebView adWebView = parentContainer.getAdWebView();
		
		int maxWidth = metrics.widthPixels;
		int maxHeight = metrics.heightPixels - adWebView.getStatusBarHeight();
				
		if ((toWidth > maxWidth) || (toHeight > maxHeight))
		{
			return "Resize to larger than screen size not allowed";	// ZZZ move to strings
		}
		
		if ((toWidth == maxWidth) && (toHeight == maxHeight))
		{
			return "Resize may not completely fill the screen";		// ZZZ move to strings
		}
		
		// The resulting size must be at least large enough for the close control.
		int closeControlSize = mraidPointToDevicePixel(CloseControlSize, adWebView.getContext());
		if ((toWidth < closeControlSize) || (toHeight < closeControlSize))
		{
			return "Resize must be large enough for close control.";
		}
		
		return null;
	}
	
	
	private RelativeLayout.LayoutParams createCloseLayoutParameters(String closePosition, final AdWebView adWebView)
	{
		RelativeLayout.LayoutParams adWebViewLayoutParams = (RelativeLayout.LayoutParams)adWebView.getLayoutParams();
		
		// Mraid spec requires min. 50 pixel height and width for close area
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
				mraidPointToDevicePixel(CloseControlSize, adWebView.getContext()), 
				mraidPointToDevicePixel(CloseControlSize, adWebView.getContext()));
		
		switch(MraidInterface.get_RESIZE_CUSTOM_CLOSE_POSITION_by_name(closePosition))
		{
		case TOP_LEFT:
			layoutParams.addRule(RelativeLayout.ALIGN_LEFT, adWebView.getId());
			layoutParams.addRule(RelativeLayout.ALIGN_TOP, adWebView.getId());
			break;
		case TOP_CENTER:
			layoutParams.leftMargin = adWebViewLayoutParams.leftMargin + adWebView.getWidth()/2 - layoutParams.width/2;
			layoutParams.addRule(RelativeLayout.ALIGN_TOP, adWebView.getId());
			break;
		case CENTER:
			layoutParams.leftMargin = adWebViewLayoutParams.leftMargin + adWebView.getWidth()/2 - layoutParams.width/2;
			layoutParams.topMargin = adWebViewLayoutParams.topMargin + adWebView.getHeight()/2 - layoutParams.height/2;
			break;
		case BOTTOM_LEFT:
			layoutParams.addRule(RelativeLayout.ALIGN_LEFT, adWebView.getId());
			layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, adWebView.getId());
			break;
		case BOTTOM_RIGHT:
			layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, adWebView.getId());
			layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, adWebView.getId());
			break;
		case BOTTOM_CENTER:
			layoutParams.leftMargin = adWebViewLayoutParams.leftMargin + adWebView.getWidth()/2 - layoutParams.width/2;
			layoutParams.addRule(RelativeLayout.ALIGN_BOTTOM, adWebView.getId());
			break;
		default: // top right is the default
			layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, adWebView.getId());
			layoutParams.addRule(RelativeLayout.ALIGN_TOP, adWebView.getId());
		}
			
		return layoutParams;
	}
	
	
	private Button createResizeCloseButton(String where, final AdWebView adWebView)
	{
		// setup close "button" (transparent clickable region positioned based on resize properties)
		Button closeButton = new Button(context);
		closeButton.setText("");
		closeButton.setBackgroundColor(Color.TRANSPARENT);
		closeButton.setLayoutParams(createCloseLayoutParameters(where, adWebView));
		
		closeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				//System.out.println("resize close click: dismissing...");
				adWebView.injectJavaScript("mraid.close();");
			}
		});
		
		return closeButton;
	}
	
	
	synchronized public String startResize(Bundle data)
	{
		// Get all resize parameters from data bundle
		Integer toWidth 		= null;
		Integer toHeight 		= null;
		String  closePosition 	= null;
		Integer offsetX 		= null;
		Integer offsetY 		= null;
		Boolean offScreen 		= null;
		try
		{
			String value = data.getString(MraidInterface.get_RESIZE_PROPERTIES_name(MraidInterface.RESIZE_PROPERTIES.WIDTH));
			if (value != null)
			{
				toWidth = Integer.parseInt(value);
			}
			else
			{
				return MASTAdConstants.STR_RICHMEDIA_ERROR_RESIZE; // XXX new, more specific error for missing width
			}
			
			value = data.getString(MraidInterface.get_RESIZE_PROPERTIES_name(MraidInterface.RESIZE_PROPERTIES.HEIGHT));
			if (value != null)
			{
				toHeight = Integer.parseInt(value);
			}
			else
			{
				return MASTAdConstants.STR_RICHMEDIA_ERROR_RESIZE; // XXX new, more specific error for missing height
			}	
				
			closePosition = data.getString(MraidInterface.get_RESIZE_PROPERTIES_name(MraidInterface.RESIZE_PROPERTIES.CUSTOM_CLOSE_POSITION));
			if (closePosition == null)
			{
				closePosition = MraidInterface.get_RESIZE_CUSTOM_CLOSE_POSITION_name(MraidInterface.RESIZE_CUSTOM_CLOSE_POSITION.TOP_RIGHT);
			}
			
			value = data.getString(MraidInterface.get_RESIZE_PROPERTIES_name(MraidInterface.RESIZE_PROPERTIES.OFFSET_X));
			if (value != null)
			{
				offsetX = Integer.parseInt(value);
			}
			else
			{
				offsetX = 0;
			}
			
			value = data.getString(MraidInterface.get_RESIZE_PROPERTIES_name(MraidInterface.RESIZE_PROPERTIES.OFFSET_Y));
			if (value != null)
			{
				offsetY = Integer.parseInt(value);
			}
			else
			{
				offsetY = 0;
			}
			
			value = data.getString(MraidInterface.get_RESIZE_PROPERTIES_name(MraidInterface.RESIZE_PROPERTIES.ALLOW_OFF_SCREEN));
			if ((value != null) && (value.equalsIgnoreCase("false")))
			{
				// reposition view if part will go off screen
				offScreen = false;
			}
			else
			{
				// do nothing even if part of view will be off screen (default)
				offScreen = true;
			}
		}
		catch(Exception ex)
		{
			return MASTAdConstants.STR_RICHMEDIA_ERROR_RESIZE; // XXX new, more specific error
		}
	
		String invalidMessage = resizePropertiesValid(toWidth, toHeight, closePosition, offsetX, offsetY, offScreen);
		if (invalidMessage != null)
		{
			//System.out.println(invalidMessage);
			return invalidMessage;
		}
		else
		{
			//System.out.println("resize validated: h/w = " + toHeight + "/" + toWidth);
		}
		
		return resizeWorker(toWidth, toHeight, closePosition, offsetX, offsetY, offScreen);
	}
	
	
	// Move ad web view to new / larger contain in front of app content, and display
	private String resizeWorker(int toWidth, int toHeight, String closePosition, int offsetXin, int offsetYin, boolean allowOffScreen)
	{
		// Combine offset and relative position of ad view on screen
		int[] bannerPosition = { 0, 0 };
		parentContainer.getLocationOnScreen(bannerPosition);
		int offsetX = offsetXin + bannerPosition[0];
		int offsetY = offsetYin + bannerPosition[1];
		
		AdWebView adWebView = parentContainer.getAdWebView();
		
		if (!allowOffScreen)
		{
			int maxWidth = metrics.widthPixels;
			int maxHeight = metrics.heightPixels;

			// TODO: Determine if the assumption that the min/max coordinates 
			// are between 0 and maxSize.  If not adjust the following.
			int minOffsetX = 0;
			int minOffsetY = 0;
			int maxOffsetX = maxWidth;
			int maxOffsetY = maxHeight;			
					
			// First adjust the height and width to fit.
			if (toWidth > maxWidth)
			{
				toWidth = maxWidth;
			}
			if (toHeight > maxHeight)
			{
				toHeight = maxHeight;
			}
			
			// Adjust X to be between 0 and maxWidth
			if (offsetX < minOffsetX)
			{
				offsetX = minOffsetX;
			}
			else if (offsetX + toWidth > maxOffsetX)
			{
				int diff = (offsetX + toWidth) - maxOffsetX;
				offsetX -= diff;
			}
			
			// Adjust Y to be between 0 and maxHeight
			if (offsetY < minOffsetY)
			{
				offsetY = minOffsetY;
			}
			else if (offsetY + toHeight > maxOffsetY)
			{
				int diff = (offsetY + toHeight) - maxOffsetY; 
				offsetY -= diff;
			}
		}
		else
		{
			int closeControlSize = mraidPointToDevicePixel(CloseControlSize, adWebView.getContext());
			
			// The resulting resize with close control must expose the close control on the screen.
			// Calculate where the close button will end up.
			int closeX = toWidth - closeControlSize;
			int closeY = 0;
			
			switch(MraidInterface.get_RESIZE_CUSTOM_CLOSE_POSITION_by_name(closePosition))
			{
			case TOP_LEFT:
				closeX = 0;
				closeY = 0;
				break;
			case TOP_CENTER:
				closeX = toWidth/2 - closeControlSize/2;
				closeY = 0;
				break;
			case CENTER:
				closeX = toWidth/2 - closeControlSize/2;
				closeY = toHeight/2 - closeControlSize/2;
				break;
			case BOTTOM_LEFT:
				closeX = 0;
				closeY = toHeight - closeControlSize;
				break;
			case BOTTOM_RIGHT:
				closeX = toWidth - closeControlSize;
				closeY = toHeight - closeControlSize;
				break;
			case BOTTOM_CENTER:
				closeX = toWidth/2 - closeControlSize/2;
				closeY = toHeight - closeControlSize;
				break;
			default: // top right is the default
				break;
			}
			
			// Adjust for the new offset.
			closeX += offsetX;
			closeY += offsetY;

			int screenMinX = 0;
			int screenMinY = 0;
			int screenMaxX = metrics.widthPixels;
			int screenMaxY = metrics.heightPixels;
			
			if ((closeX < screenMinX) || (closeY < screenMinY) || 
					(closeX + closeControlSize > screenMaxX) || (closeY + closeControlSize > screenMaxY))
			{
				return "Resize must include the close control on screen.";
			}
		}
	
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)adWebView.getLayoutParams();
		lp.setMargins(offsetX, offsetY, Integer.MIN_VALUE, Integer.MIN_VALUE);
		lp.width = toWidth;
		lp.height = toHeight;
		
		// Get parent for ad view
		ViewGroup parent = (ViewGroup)adWebView.getParent();
		
		// notify ad using mraid methods per spec.
		// NOTE: this needs to come before the block below so that the state is resized when the ad container checks
		adWebView.getMraidInterface().fireSizeChangeEvent(toWidth, toHeight);
		adWebView.getMraidInterface().setState(MraidInterface.STATES.RESIZED);
				
		//if (adWebView.getMraidInterface().getState() == MraidInterface.STATES.RESIZED)
		if ((parent instanceof AdViewContainer) == false)
		{
			// ad view already displayed in resized "overlay", simply adjust the size/position
			parent.requestLayout();
			adWebView.requestLayout();
			
			// Update close button position.
			RelativeLayout adFrame = (RelativeLayout)resizeCloseButton.getParent();
			adFrame.removeView(resizeCloseButton);
			resizeCloseButton = createResizeCloseButton(closePosition, adWebView);  
			adFrame.addView(resizeCloseButton);
		}
		else
		{
			// Create new "overlay" and move ad view and screen content to it
			RelativeLayout adFrame = new RelativeLayout(context);
			ViewGroup.LayoutParams plp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
			//ViewGroup.LayoutParams plp = new ViewGroup.LayoutParams(toWidth, toHeight);
			adFrame.setLayoutParams(plp);
			
			// Remove ad view from parent (inline with app content)
			parent.removeView(adWebView);
			adFrame.addView(adWebView);
			
			// Find previous screen content, remove it 
			resizeDecorView = ((ViewGroup)((Activity) context).getWindow().getDecorView());
			//resizeOldContent = resizeDecorView.findViewById(android.R.id.content);
			//resizeOldContentIndex = ((ViewGroup)resizeOldContent.getParent()).indexOfChild(resizeOldContent);
			resizeOldContentIndex = 0;
			resizeOldContent = resizeDecorView.getChildAt(resizeOldContentIndex);
			((ViewGroup)resizeOldContent.getParent()).removeView(resizeOldContent);
			adFrame.addView(resizeOldContent);
			
			// Create close button (transparent, as visual indicator is provided by ad)
			// and position based on ad rule
			resizeCloseButton = createResizeCloseButton(closePosition, adWebView);  
			adFrame.addView(resizeCloseButton);
			
			// Finish, display it
			resizeDecorView.addView(adFrame, resizeOldContentIndex);
			adFrame.bringChildToFront(adWebView);
			adFrame.bringChildToFront(resizeCloseButton);
			adWebView.measure(toWidth, toHeight);
			adWebView.requestLayout();
			adFrame.requestLayout();
			adWebView.requestFocus();
		}
		
		MASTAdDelegate delegate = parentContainer.getAdDelegate();
		if (delegate != null)
		{
			MASTAdDelegate.AdActivityEventHandler activityHandler = delegate.getAdActivityEventHandler();
			if (activityHandler != null)
			{
				activityHandler.onAdResized((MASTAdView)parentContainer, toHeight, toWidth);
			}
		}
		
		return null;
	}
	
	
	synchronized public void undoResize()
	{
		// Put old screen content back in decor view where it started
		if ((resizeDecorView != null) && (resizeOldContent != null))
		{
			if (resizeCloseButton != null)
			{
				RelativeLayout adFrame = (RelativeLayout)resizeCloseButton.getParent();
				if (adFrame != null)
				{
					adFrame.removeView(resizeCloseButton);
				}
			}
			
			((ViewGroup)resizeOldContent.getParent()).removeView(resizeOldContent);
			resizeDecorView.addView(resizeOldContent, resizeOldContentIndex);
			
			resizeOldContent = null;
			resizeDecorView = null;
		}
	}

	
	// Show interstitial ad view
	synchronized public void showInterstitialDialog(int showCloseDelay, int autoCloseDelay)
	{
		expandedCallback(metrics.heightPixels, metrics.widthPixels);
		
		// create dialog object for showing interstitial ad
		DialogOptions options = new DialogOptions();
		options.showCloseDelay = showCloseDelay;
		options.autoCloseDelay = autoCloseDelay;
		
		if (context instanceof Activity)
			preExpandRequestedOrientation = ((Activity)context).getRequestedOrientation();
		
		Dialog dialog = adDialogFactory.createDialog(parentContainer, options);
		dialog.show();
	}

	
	// Dismiss dialog created via open/expand/show
	synchronized public void dismissDialog()
	{
		Dialog d = adDialogFactory.getDialog();
		if (d != null)
		{
			d.dismiss();
			
			if (context instanceof Activity)
				((Activity)context).setRequestedOrientation(preExpandRequestedOrientation);
			
			MASTAdDelegate delegate = parentContainer.getAdDelegate();
			if (delegate != null)
			{
				MASTAdDelegate.AdActivityEventHandler eventHandler = delegate.getAdActivityEventHandler();
				if (eventHandler != null)
				{
					eventHandler.onAdCollapsed((MASTAdView)parentContainer);
				}
			}
		}	
	}
	
	public static MraidInterface.FORCE_ORIENTATION_PROPERTIES getScreenOrientation(Context context)
	{
		switch (context.getResources().getConfiguration().orientation)
		{
		case Configuration.ORIENTATION_PORTRAIT:
		  return MraidInterface.FORCE_ORIENTATION_PROPERTIES.PORTRAIT;
		case Configuration.ORIENTATION_LANDSCAPE:
			return MraidInterface.FORCE_ORIENTATION_PROPERTIES.LANDSCAPE;
		case Configuration.ORIENTATION_SQUARE:
			return MraidInterface.FORCE_ORIENTATION_PROPERTIES.PORTRAIT;
		default:
			return MraidInterface.FORCE_ORIENTATION_PROPERTIES.PORTRAIT;
		}	
	}
	
	synchronized public AdWebView getExpandedAdView()
	{
		return expandedAdView;
	}
	
	
	synchronized public void clearExpandedAdView()
	{
		expandedAdView = null;
	}
	
	
	public static int devicePixelToMraidPoint(int pixelSize, Context context)
	{
		int points = Math.round(pixelSize / context.getResources().getDisplayMetrics().density);
		return points;
	}
	
	
	public static int mraidPointToDevicePixel(int pointSize, Context context)
	{
		int pixels = Math.round(pointSize * context.getResources().getDisplayMetrics().density);
		return pixels;
	}
}
