//
// Copyright (C) 2011, 2012 Mocean Mobile. All Rights Reserved. 
//
package com.MASTAdView.core;

import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.http.NameValuePair;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.MailTo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.MASTAdView.MASTAdConstants;
import com.MASTAdView.MASTAdDelegate;
import com.MASTAdView.MASTAdDelegate.RichmediaEventHandler;
import com.MASTAdView.MASTAdLog;
import com.MASTAdView.MASTAdRequest;
import com.MASTAdView.MASTAdView;


public class AdViewContainer extends RelativeLayout implements ContentManager.ContentConsumer
{
	private Context 								context;
	
	private TextView								adTextView;
	//private ImageView								adImageView;
	private View									adImageView;
	private AdWebView								adWebView;
	
	private Button									bannerCloseButton = null;
	private Button									customCloseButton = null;

	private Integer 								defaultImageResource = null;
	private int 									defaltBackgroundColor = Color.TRANSPARENT;
	private int										defaultTextColor = Color.BLACK;

	private boolean									isShowCloseOnBanner = false;
	private boolean									isShowPreviousAdOnError = false;
	
	private AdSizeUtilities							adSizeUtilities;
	final private MASTAdLog 						adLog = new MASTAdLog(this);

	private MASTAdRequest							adserverRequest;
	private String 									lastRequest;
	private AdData 									lastResponse;
	private int 									requestCounter = 0;
	
	private DisplayMetrics							metrics = null;
	private MASTAdDelegate							adDelegate;
	
	private AdReloadTimer							adReloadTimer;
	private int										showCloseInterstitialTime = 0;
	
	private Handler 								handler;
	
	private OrientationChangeListener				orientationChangeListener = null;
	private AdLocationListener						locationListener = null;
	
	// Local notion of placement type, partically duplicating the mraid interface value, needed for non-mraid ads
	private MraidInterface.PLACEMENT_TYPES			adPlacementType = MraidInterface.PLACEMENT_TYPES.INLINE;
	
	// Reference to self
	private AdViewContainer							self;
	
	// Use internal browser or standalone browser?
	private boolean 								useInternalBrowser = false;

	// Coordinates of view on screen
	private int[] coordinates = { 0, 0 };
	
	
	//
	// Constructors
	//
	
	
	public AdViewContainer(Context context, Integer site, Integer zone)
	{
		super(context); // NOTE: needs to be an activity for orientation changes to work
		
		initialize(context);
		
		adserverRequest.setProperty(MASTAdRequest.parameter_site, site);
		adserverRequest.setProperty(MASTAdRequest.parameter_zone, zone);
	}
	
	
	public AdViewContainer(Context context, Integer site, Integer zone, boolean isInterstitial)
	{
		super(context); // NOTE: needs to be an activity for orientation changes to work
		
		initialize(context);
		
		adserverRequest.setProperty(MASTAdRequest.parameter_site, site);
		adserverRequest.setProperty(MASTAdRequest.parameter_zone, zone);
	
		adPlacementType = MraidInterface.PLACEMENT_TYPES.INLINE;
		if (isInterstitial)
		{
			adPlacementType = MraidInterface.PLACEMENT_TYPES.INTERSTITIAL;
		}
		adWebView.getMraidInterface().setPlacementType(adPlacementType);
	}
	
	
	public AdViewContainer(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle); // NOTE: needs to be an activity for orientation changes to work
		
		initialize(context);
		
		LayoutAttributeHandler layoutHandler = new LayoutAttributeHandler(this);
		layoutHandler.processAttributes(attrs);

		// For layout based views, peform an implicit update() as long as site and zone
		// are set, so that user doesn't need to get a code reference to the veiw just
		// to start an update.
		if ((adserverRequest.getProperty(MASTAdRequest.parameter_site) != null) &&
			(adserverRequest.getProperty(MASTAdRequest.parameter_zone) != null))
		{
			update();
		}
	}

	
	public AdViewContainer(Context context, AttributeSet attrs)
	{
		super(context, attrs); // NOTE: needs to be an activity for orientation changes to work
		
		initialize(context);
		
		LayoutAttributeHandler layoutHandler = new LayoutAttributeHandler(this);
		layoutHandler.processAttributes(attrs);

		// For layout based views, peform an implicit update() as long as site and zone
		// are set, so that user doesn't need to get a code reference to the veiw just
		// to start an update.
		if ((adserverRequest.getProperty(MASTAdRequest.parameter_site) != null) &&
			(adserverRequest.getProperty(MASTAdRequest.parameter_zone) != null))
		{
			update();
		}	
	}

	
	public AdViewContainer(Context context)
	{
		super(context); // NOTE: needs to be an activity for orientation changes to work
		
		initialize(context);
	}
	
	
	//
	// Initialization/view creation
	//
	
	
	// Common initialization for various constructors; creates ad request object, handler, orientation
	// listeners, display metrics, ad size helper, delegates, and views.
	private void initialize(Context c)
	{
		context = c;
		setScriptPath();
		
		if (adserverRequest == null) adserverRequest = new MASTAdRequest(adLog, context);
		
		self = this; // save reference to the container
		
		// Setup handler for inter-thread communication/method invocation
		handler = new AdMessageHandler(this);
		
		WindowManager windowManager = (WindowManager) ((Activity)context).getSystemService(Context.WINDOW_SERVICE);
			
		// listen for orientation changes, and handle them for eveyr add view
		orientationChangeListener = OrientationChangeListener.getInstance(context, windowManager.getDefaultDisplay());
		orientationChangeListener.addView(this);
		
		// Save original screen dimensions for later use
		metrics = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(metrics);
				
		// Setup ad dialog factory for interstitial/exapnded use
		//adDialogFactory = new AdDialogFactory(context);
		adSizeUtilities = new AdSizeUtilities(this, metrics);
		
		// Create views for each ad type
		adTextView = createTextView(context);
		adImageView = createImageView(context, true); // create view with gif animation support
		adWebView = createWebView(context);
		
		// Setup auto parameters (such as user agent, etc.)
		setAutomaticParameters(context);
				
		setVisibility(View.GONE);
		//setOrientation(LinearLayout.HORIZONTAL);
		setBackgroundColor(MASTAdConstants.DEFAULT_COLOR);
		
		ContentManager.getInstance(this); // force create
		
		adDelegate = new MASTAdDelegate();
		adReloadTimer = new AdReloadTimer(context, this, adLog);
		
		// Set a global scroll listener which should be called when anything in the view tree scrolls;
		// when this is calld, check the visibility of the ad view and update the viewable property appropriately.
		getViewTreeObserver().addOnScrollChangedListener(
			new ViewTreeObserver.OnScrollChangedListener()
			{ 
				public void onScrollChanged()
				{ 
					//System.out.println("!!! onScrollChanged called");
					setViewable(adWebView);
				} 
			});
	}
	
	
	public void removeContent()
	{
		// If interstitial, resized or expanded ad open, close
		close(null); // XXX
		
		// Reset all to initial state
		removeAllViews();

		// Try to recycle bitmap memory
		if (adImageView instanceof ImageView)
		{
			freeBitmapImageviewResouces((ImageView)adImageView);
		}		
	}
	
	
	public void reset()
	{
		removeContent();
		
		// stop ad reload timer and disable
		adReloadTimer.setAdReloadPeriod(MASTAdConstants.AD_RELOAD_PERIOD);
		adReloadTimer.stopTimer(true);
		
		bannerCloseButton = null;
		customCloseButton = null;
		
		// If listening for location updates, stop
		if (locationListener != null)
		{
			locationListener.stop();
			locationListener = null;
		}
		
		defaultImageResource = null;
		
		defaltBackgroundColor = Color.TRANSPARENT;
		defaultTextColor = Color.BLACK;

		isShowCloseOnBanner = false;
		isShowPreviousAdOnError = false;
		
		adserverRequest.reset();
	}
	
	
	private TextView createTextView(Context context)
	{
		TextView v = new TextView(context);
		
		// Child will fill the parent container; we manage the size at the parent level
		v.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		
		// apply standard properties
		v.setBackgroundColor(defaltBackgroundColor);
		v.setTextColor(defaultTextColor);
		
		return v;
	}

	
	private View createImageView(Context context, boolean withAnimataedGifSupport)
	{
		View v;
		if (withAnimataedGifSupport)
		{
			boolean handleClicks = true;
			v = new AdImageView(this, adLog, metrics, handleClicks);
			v.setLayoutParams(createAdLayoutParameters());
		}
		else
		{
			v = new ImageView(context);
			
			// Child will fill the parent container; we manage the size at the parent level
			v.setLayoutParams(new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		}
		
		// apply standard properties
		v.setBackgroundColor(defaltBackgroundColor);
		
		return v;
	}
	
	
	public AdWebView createWebView(final Context context)
	{
		boolean supportMraid = true;
		boolean handleClicks = true;
		AdWebView v = new AdWebView(this, adLog, metrics, supportMraid, handleClicks);

		v.setLayoutParams(createAdLayoutParameters());		
		v.setBackgroundColor(defaltBackgroundColor);

		// Set a global layout listener which will be called when the layout pass is completed and the view is drawn;
		// this seems to be the only reliable way to get the initial location information which isn't set until the
		// layout is complete.
		getViewTreeObserver().addOnGlobalLayoutListener(
			new ViewTreeObserver.OnGlobalLayoutListener()
			{
				public void onGlobalLayout()
				{
					if ((adWebView != null) && (adWebView.getMraidInterface().getState() == MraidInterface.STATES.DEFAULT))
					{
							adWebView.getLocationOnScreen(coordinates); // getLocationInWindow() for relative
							adWebView.getMraidInterface().setCurrentPosition(coordinates[0], coordinates[1], adWebView.getWidth(), adWebView.getHeight());
					}
				}
			});
		
		return v;
	}

	
	private RelativeLayout.LayoutParams createAdLayoutParameters()
	{
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
		return layoutParams;
	}
	
	
	public AdWebView getAdWebView()
	{
		return adWebView; 
	}
	
	
	public View getAdImageView()
	{
		return adImageView;
	}
	
	
	public TextView getAdTextView()
	{
		return adTextView;
	}
	
	
	public boolean prefetchImages()
	{
		// If using an imageview for images, standalone fetch is required,
		// but if using a webview variation (for animated gif support), no.
		if (adImageView instanceof ImageView)
		{
			return true;
		}
		
		return false;
	}
	
	
	// remove and leave logic in ad server request
	public String getUserAgent()
	{
		String userAgent = (String)adserverRequest.getProperty(MASTAdRequest.parameter_userAgent);
		if (userAgent == null)
		{
			userAgent = adWebView.getSettings().getUserAgentString();

			if ((userAgent != null) && (userAgent.length() > 0))
			{
				adserverRequest.setProperty(MASTAdRequest.parameter_userAgent, userAgent);
			}
		}
		
		return userAgent;
	}

	
	protected void setAutomaticParameters(Context context)
	{
		// Set SDK version
		String version = MASTAdConstants.SDK_VERSION;
		if ((version != null) && (version.length() > 0))
		{
			adserverRequest.setProperty(MASTAdRequest.parameter_version, version);
		}

		// Set user agent to match what comes from a web view
		getUserAgent();
	}
	
	
	//
	// Layout management for container view
	//
	
	
	/**
	 * Override method to set layout parameters so that local copy of width/height can be saved.
	 */
	@Override
	public void setLayoutParams(ViewGroup.LayoutParams params)
	{
		setVisibility(View.VISIBLE);
		
		//layoutWidth  = params.width;
		//layoutHeight = params.height;
		
		super.setLayoutParams(params);
	}


	// view being notified of a size change (rotation?)
	protected void onSizeChanged(int w, int h, int ow, int oh)
	{
		if (adWebView != null)
		{
			adWebView.getLocationOnScreen(coordinates); // getLocationInWindow() for relative
			adWebView.getMraidInterface().setCurrentPosition(coordinates[0], coordinates[1], w, h);
			
			// Notify ad that size has changed
			adWebView.getMraidInterface().fireSizeChangeEvent(w, h);
			
			if ((adWebView.getMraidInterface().getState() == MraidInterface.STATES.LOADING) ||
				(adWebView.getMraidInterface().getState() == MraidInterface.STATES.DEFAULT))
			{
				// and pass event through, unless expanded/resized (where this view is being covered)
		        super.onSizeChanged(w, h, ow, oh);
			}
		}
    }

	
	//
	// Content loaders; NOTE - must call these from a UI thread!!
	//
	
	
	private void setTextContent(AdData ad)
	{
		// remove view from any other containers (if needed)
		if (adTextView.getParent() != null)
		{
			((ViewGroup)adTextView.getParent()).removeView(adTextView);
		}
				
		addView(adTextView);
		adTextView.setText(ad.text);
		adTextView.setVisibility(View.VISIBLE);
		
		if (ad.clickUrl != null)
		{
			AdClickHandler clickHandler = new AdClickHandler(this, ad);
			adTextView.setOnClickListener(clickHandler);
		}
		
		//mViewState = Mraid.STATES.DEFAULT;
		
		//setBackgroundColor(Color.BLACK);
	}
	
	
	private void setImageContent(AdData ad)
	{
		// remove view from any other containers (if needed)
		if (adImageView.getParent() != null)
		{
			((ViewGroup)adImageView.getParent()).removeView(adImageView);
		}
		
		addView(adImageView);
		
		if (adImageView instanceof ImageView)
		{
			((ImageView)adImageView).setImageBitmap(ad.imageBitmap);
			
			if (ad.clickUrl != null)
			{
				AdClickHandler clickHandler = new AdClickHandler(this, ad);
				adImageView.setOnClickListener(clickHandler);
			}
		}
		else
		{
			((AdImageView)adImageView).setImage(ad);
		}
		
		adImageView.setVisibility(View.VISIBLE);
		
		//mViewState = Mraid.STATES.DEFAULT;
		
		//setBackgroundColor(Color.BLACK);
	}
	
	
	private void setWebContent(String webData)
	{	
		// remove view from any other containers (if needed)
		if (adWebView.getParent() != null)
		{
			((ViewGroup)adWebView.getParent()).removeView(adWebView);
		}
		
		// If state is not loading, set that before continuing
		if (adWebView.getMraidInterface().getState() != MraidInterface.STATES.LOADING)
		{
			adWebView.resetForNewAd();
		}
				
		// Put web view in place
		addView(adWebView);
		adWebView.setVisibility(View.VISIBLE);
		
		//webData = "<HTML><HEAD><TITLE>Testing...</TITLE></HEAD><BODY><H1>Testing document...</H1></BODY></HTML>";
		String dataOut = setupViewport(false, webData);
		//System.out.println("setWebContent: injecting: " + dataOut);
		//setupViewport(false, webData);
		
		adWebView.loadDataWithBaseURL(null, dataOut, "text/html", "UTF-8", null);
		//adWebView.loadDataWithBaseURL(null, webData, "text/html", "UTF-8", null);
	}
	
	
	// Display ad content in appropriate view based on ad type
	synchronized private void setAdContent(AdData ad)
	{
		if (ad == null)
		{
			return;
		}
			
		removeAllViews();
		
		if (ad.adType != MASTAdConstants.AD_TYPE_IMAGE)
		{
			// Try to recycle bitmap memory
			if (adImageView instanceof ImageView)
			{
				freeBitmapImageviewResouces((ImageView)adImageView);
			}
		}
		
		if (isShowCloseOnBanner)
		{
			// put close button back if requested
			if  (bannerCloseButton != null)
			{
				addView(bannerCloseButton);
			}
			
			showCloseButtonWorker();
		}
		
		if (ad.adType == MASTAdConstants.AD_TYPE_TEXT)
		{
			setTextContent(ad);
			sendTrackingImpression(ad);
		}
		else if (ad.adType == MASTAdConstants.AD_TYPE_IMAGE)
		{
			setImageContent(ad);
			sendTrackingImpression(ad);
		}
		else // RICHMEDIA or THIRDPARTY (which uses rich media)
		{
			setWebContent(ad.richContent);
		}
		
		//adViewState = MraidInterface.STATES.DEFAULT; already done in ad web view???
	}
	
	
	private void freeBitmapImageviewResouces(ImageView view)
    {
    	//System.out.println("Free image view resources...");
    	
    	// Do everything possible to free up memory associated with the ad image;
		// this can be important because android allocates and handles bitmap memory
		// differently from application memory, and if you run out your app will crash
		// with no way chance to resolve it.
    	if (view != null)
    	{
    		Bitmap bm;
    		Drawable d;
    		
    		d = (Drawable)view.getBackground();
    		if (d != null)
    		{
    			d.setCallback(null);
    			if (d instanceof BitmapDrawable)
    			{
    				bm = ((BitmapDrawable)d).getBitmap();
    				view.setBackgroundDrawable(null);
    				bm.recycle();
    				bm = null;
    			}
    			else
    			{
    				view.setBackgroundDrawable(null);
    			}
    		}
    		
    		d = (BitmapDrawable)view.getDrawable();
    		if (d != null)
    		{
    			d.setCallback(null);
    			if (d instanceof BitmapDrawable)
    			{
    				bm = ((BitmapDrawable)d).getBitmap();
    				view.setImageDrawable(null);
    				bm.recycle();
    				bm = null;
    			}
    			else
    			{
    				view.setImageDrawable(null);
    			}
    		}
    	}
    }
    
	
	private void sendTrackingImpression(AdData ad)
	{
		if ((ad != null) && (ad.trackUrl != null) && (ad.trackUrl.length() > 0))
		{
			// Looks like we have a tracking url, fire off worker to send impression back to server
			AdData.sendImpressionInBackground(ad.trackUrl, getUserAgent());
		}
	}
	
	
	//
	// Generic ad content loader which can be called from a non-ui thread
	//
	
	
	public void setAdContentOnUi(final AdData ad)
	{
		handler.post(new Runnable()
		{
			public void run()
			{
				setAdContent(ad);
				
				/*
				getLayoutParams().width  = layoutWidth;
				getLayoutParams().height = layoutHeight;
				requestLayout();
				*/
			}
		});
	}
	

	/**
	 * Immediately update ad view contents.
	 */
	public void update()
	{
		MraidInterface.STATES state = adWebView.getMraidInterface().getState(); 
		if ((state == MraidInterface.STATES.DEFAULT) || (state == MraidInterface.STATES.LOADING))
		{
			adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "update", "");
			StartLoadContent();	
		}
		else
		{
			adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "update", "skipped - state not default (" + state + ")");
		}
	}
	
	
	/*
	protected void update(boolean isManual)
	{
		if (isShown() || isManual) 
		{
			adLog.log(MASTAdLog.LOG_LEVEL_ALL, MASTAdLog.LOG_TYPE_INFO, "update", "");
			if (isManual) IsManualUpdate = true;
			//hideVirtualKeyboard();
			StartLoadContent();
		}
	}
	*/
	

	/**
	 * Set banner refresh interval (in seconds). Once an ad has finished loading, the timer starts
	 * and a new ad will be loaded after this amount of time has elapsed. Default 120 seconds.
	 * If 0, ads are not updated automatically (use the update() method for a manual update.)
	 * If less than 0, the default value is re-instated.
	 */
	public void setUpdateTime(int updateTime)
	{
		adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "setUpdateTime", String.valueOf(updateTime));
		adReloadTimer.setAdReloadPeriod(updateTime);
	}

	
	// 
	// Integration with ad-fetching/parsing 
	//

	
	// start loading an ad from server
	public void StartLoadContent()
	{
		adReloadTimer.cancelTask();
		
		adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "StartLoadContent", "");
		
		// have to have a valid site & zone to request content 
		if ((adserverRequest == null) ||
		    (adserverRequest.getProperty(MASTAdRequest.parameter_site, 0) == 0) ||
		    (adserverRequest.getProperty(MASTAdRequest.parameter_zone, 0) == 0))
		{
			adReloadTimer.startTimer();
			adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "StartLoadContent", "site=0 or zone=0");
			return;
		}
		
		if ((defaultImageResource != null) && (getBackground() == null))
		{
			try
			{
				handler.post(new SetBackgroundResourceAction(defaultImageResource));
			} 
			catch (Exception e)
			{
				adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "StartLoadContent", e.getMessage());
			}
		}
	
		// If expanded form of ad is being displayed, we don't load new content
		if (adWebView.getMraidInterface().getState() != MraidInterface.STATES.EXPANDED)
		{						
			try
			{
				if (adWebView.getMraidInterface().getState() == MraidInterface.STATES.RESIZED)
				{
					// Ad view is going to reload & resize to default state; we need our state to match that
					adWebView.getMraidInterface().setState(MraidInterface.STATES.DEFAULT);
				}

				// if delegate defined, invoke
				if (adDelegate != null)
				{
					MASTAdDelegate.AdDownloadEventHandler downloadHandler = adDelegate.getAdDownloadHandler(); 
					if (downloadHandler != null)
					{
						downloadHandler.onDownloadBegin((MASTAdView)this);
					}
				}
				
				String url = adserverRequest.toString(MASTAdConstants.AD_REQUEST_TYPE_XML);
				lastRequest = url;
				requestCounter++;
				adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "requestGet["+String.valueOf(requestCounter)+"]" , url);
				ContentManager.getInstance(this).startLoadContent(this, url);
			}
			catch (Exception e)
			{
				adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "StartLoadContent.requestGet", e.getMessage());
				//interceptOnAdDownload.error(this, e.getMessage());
				
				// start timer?
			}
		}
	}


	// Invoked when retrieving an ad fails
	private void setErrorResult(AdData ad)
	{
		if (ad.serverErrorCode != null)
		{
			// If the server returned an error code, and it is 404 (no ads available) we treat that as informational;
			// other codes are true errors.
			if (ad.serverErrorCode == 404)
			{
				adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "requestGet result["+String.valueOf(requestCounter)+"][ERROR][CODE=" + ad.serverErrorCode+"]", ad.error);
			}
			else
			{
				adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "requestGet result["+String.valueOf(requestCounter)+"][ERROR][CODE=" + ad.serverErrorCode+"]", ad.error);
			}
		}
		else
		{
			adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "requestGet result["+String.valueOf(requestCounter)+"][ERROR]", ad.error);
		}

		if (adDelegate != null)
		{
			MASTAdDelegate.AdDownloadEventHandler downloadHandler = adDelegate.getAdDownloadHandler(); 
			if (downloadHandler != null)
			{
				downloadHandler.onDownloadError((MASTAdView)this, ad.error);
			}
		}

		if (defaultImageResource != null)
		{
			try
			{
				handler.post(new SetBackgroundResourceAction(defaultImageResource));
			}
			catch (Exception e)
			{
				adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "setErrorResult", e.getMessage());
			}
		}
		
		adReloadTimer.startTimer();
		
		// Show previous ad?
		if (lastResponse == null)
		{
			return;
		}		
		// If supposed to show previous ad on error, but no previous content, skip out
		if ((lastResponse != null) && (lastResponse.hasContent()) && (lastResponse.error == null) && !isShowPreviousAdOnError)
		{
			return;
		}
		
		setResult(lastResponse);
		
		return;
	}

	
	// handle result ad data after fetch from server
	synchronized public boolean setResult(final AdData ad)
	{
		if (ad == null)
		{
			AdData error = new AdData();
			error.error = MASTAdConstants.STR_NULL_AD_ERROR;
			setErrorResult(error);
			return false;
		}
		else if (ad.error != null)
		{
			setErrorResult(ad);
			return false;
		}
		else if (ad.hasContent() == false)
		{
			ad.error = MASTAdConstants.STR_NO_AD_CONTENT_ERROR;
			setErrorResult(ad);
			return false;
		}
		else 
		{
			// Callback to notify that ad download completed.
			// if delegate defined, invoke
			if (adDelegate != null)
			{
				MASTAdDelegate.AdDownloadEventHandler downloadHandler = adDelegate.getAdDownloadHandler(); 
				if (downloadHandler != null)
				{
					downloadHandler.onDownloadEnd((MASTAdView)this);
				}
			}
			
			if (this.getParent() == null)
			{
				// View not currently included in any layout, don't try to show content for now, just save it
				lastResponse = ad;
				adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "requestGet result["+String.valueOf(requestCounter)+"]", ad.toString());
				adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "setResult", "no parent for ad view, skipping display for now...");
				return false;
			}
			
			adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "requestGet result["+String.valueOf(requestCounter)+"]", ad.toString());
		}
		
		try
		{
			if (ad.adType == MASTAdConstants.AD_TYPE_EXTERNAL_THIRDPARTY)
			{
				notifyExternalThirdPartyAd(ad);
			}
			else
			{	
				setAdContentOnUi(ad);
			}
			adReloadTimer.startTimer();
		}
		catch (Exception e)
		{
			adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "StartLoadContent", e.getMessage());
			adReloadTimer.startTimer();
			return false;
		}
		
		// Not an error, so remember this as the new "last" ad
		lastResponse = ad;
					
		return true;
	}

	
	// Client side third party (SDK) ad "redirect"
	private void notifyExternalThirdPartyAd(AdData ad)
	{
		if ((ad != null) && (adDelegate != null) && (adDelegate.getThirdPartyRequestHandler() != null))
		{
			try
			{
				HashMap<String, String> params = new HashMap<String, String>();
				params.put("type", ad.getAdTypeName());
				
				if ((ad.externalCampaignProperties != null) && (!ad.externalCampaignProperties.isEmpty()))
				{
					Iterator<NameValuePair> i = ad.externalCampaignProperties.iterator();
					NameValuePair nvp;
					while (i.hasNext())
					{
						nvp = i.next();
						if (nvp != null)
						{
							params.put(nvp.getName(), nvp.getValue());
						}
					}
				}
	
				adDelegate.getThirdPartyRequestHandler().onThirdPartyEvent((MASTAdView) this, params);
			}
			catch (Exception e)
			{
				adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "onThirdPartyRequest", e.getMessage());										
			}
		}
	}

	
	private static String mScriptPath = null;
	
	
	private synchronized void setScriptPath()
	{
		if (mScriptPath == null)
		{
			mScriptPath = FileUtils.copyTextFromJarIntoAssetDir(context, "/mraid.js", "/mraid.js");
		}
	}

	
	// Create viewport for showing ad
	public String setupViewport(boolean headerOnly, String body)
	{
		StringBuffer data = new StringBuffer("<html><head>");
		
		// Insert our javascript bridge library; this is always required
		data.append("<style>*{margin:0;padding:0}</style>");
		data.append("<script src=\"file://");
		data.append(mScriptPath);
		data.append("\" type=\"text/javascript\"></script>");
		data.append(getInjectionHeaderCode());
		data.append("</head><body>");
		
		if (!headerOnly && (body != null))
		{
			data.append(body);
		}
		
		data.append("</body></html>");
		
		//System.out.println("SetupViewport: final string: " + data.toString());
		return data.toString();
	}
	
	
	//
	// Javascript interaction
	//
	

	// Called when close() method is invoked from ad view
	public String close(Bundle ignoredBundle)
	{	
		MraidInterface.STATES adState = adWebView.getMraidInterface().getState();
		if (adPlacementType == MraidInterface.PLACEMENT_TYPES.INTERSTITIAL)
		{
			closeInterstitial();	
			if ((customCloseButton != null) && (customCloseButton.getParent() != null))
			{
				((ViewGroup)customCloseButton.getParent()).removeView(customCloseButton);
			}
			
			// add close button again???
		}
		else if (adState == MraidInterface.STATES.EXPANDED)
		{
			//System.out.println("Closing expanded ad view...");
			
			// dismiss dialog containing expanded view
			adSizeUtilities.dismissDialog();
			
			// In the case of a 2-part creative, the original adview was left in place,
			// which means none of this is needed.
			ViewGroup parent = (ViewGroup)adWebView.getParent();
			if (parent != this)
			{
				// Remove adview from temporary container
				if (parent != null)
				{
					parent.removeView(adWebView);
				}
							
				// Reset layout parameters
				adWebView.setLayoutParams(createAdLayoutParameters());
				
				// Move ad view back to normal container
				this.addView(adWebView);
				
				adSizeUtilities.clearExpandedAdView();
			}
				
			// Return to default state
			adWebView.getMraidInterface().setState(MraidInterface.STATES.DEFAULT);
			adWebView.getMraidInterface().fireSizeChangeEvent(this.getWidth(), this.getHeight());
			//adWebView.getMraidInterface().fireSizeChangeEvent(AdSizeUtilities.devicePixelToMraidPoint(this.getWidth(), context), AdSizeUtilities.devicePixelToMraidPoint(this.getHeight(), context));
		}
		else if (adState == MraidInterface.STATES.RESIZED)
		{
			int resizeToX;
			int resizeToY;
			/*
			if ((resizeOldWidth != 0) && (resizeOldWidth != 0))
			{
				resizeToX = resizeOldWidth;
				resizeToY = resizeOldHeight;
				resizeOldWidth = 0;
				resizeOldHeight = 0;
			}
			else
			{
				resizeToX = this.getWidth();
				resizeToY = this.getHeight();
			}
			*/
			resizeToX = this.getWidth();
			resizeToY = this.getHeight();
			
			// Remove adview from temporary container
			ViewGroup parent = (ViewGroup)adWebView.getParent();
			if (parent != null)
			{
				parent.removeView(adWebView);
			
				// Also put screen content back in place, unoding change made when resize was first done
				adSizeUtilities.undoResize();
				
				// Now remove parent, which was a temporary container created for the expand/resize
				/*
				ViewGroup pparent = (ViewGroup)parent.getParent();
				if (pparent != null)
				{
					pparent.removeView(parent);
				}
				*/
			}
	
			// Reset layout parameters
			//adWebView.setLayoutParams(createAdLayoutParameters());
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)adWebView.getLayoutParams();
			lp.setMargins(0, 0, 0, 0);
			lp.width = RelativeLayout.LayoutParams.FILL_PARENT;
			lp.height = RelativeLayout.LayoutParams.FILL_PARENT;
			
			// Move ad view back to normal container
			this.addView(adWebView);
			adWebView.requestLayout();
			this.requestLayout();
			
			// Return to default state
			adWebView.getMraidInterface().setState(MraidInterface.STATES.DEFAULT);
			//adWebView.getMraidInterface().fireSizeChangeEvent(this.getWidth(), this.getHeight());
			adWebView.getMraidInterface().fireSizeChangeEvent(resizeToX, resizeToY);
			//adWebView.getMraidInterface().fireSizeChangeEvent(AdSizeUtilities.devicePixelToMraidPoint(this.getWidth(), context), AdSizeUtilities.devicePixelToMraidPoint(this.getHeight(), context));
		}
		else
		{
			// Ignored, NOT an error, per spec.
		}
		
		
		return null;
	}

	
	public void closeInterstitial()
	{
		if (adPlacementType == MraidInterface.PLACEMENT_TYPES.INTERSTITIAL)
		{
			MraidInterface.STATES adState = adWebView.getMraidInterface().getState();
			if ((adState == MraidInterface.STATES.DEFAULT) ||
				(adState == MraidInterface.STATES.LOADING)) // Non-MRAID ad state never goes beyond loading
			{
				//System.out.println("Closing interstitial ad view...");
				
				// dismiss dialog containing interstitial view
				adSizeUtilities.dismissDialog();
				
				// Make view invisible
				//adWebView.setVisibility(View.GONE);
				
				// set state to hidden
				adWebView.getMraidInterface().setState(MraidInterface.STATES.HIDDEN);
				
				// Notify ad that viewable state has changed
				adWebView.getMraidInterface().setViewable(false);
			}
			else
			{
				adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdViewContainer", "Attempt to close interstitial with state not default, ignored");
			}
		}
		else
		{
			adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdViewContainer", "Attempt to close interstitial with wrong placement");

		}
	}
	
	
	// ZZZ Can we invoke this when view scrolls in/out of screen (viewport)???
	private void setViewable(AdWebView webView)
	{
		Rect scrollBounds = new Rect();
		this.getHitRect(scrollBounds);
		if (webView.getLocalVisibleRect(scrollBounds)) {
		    // View is within the visible window
			adWebView.getMraidInterface().setViewable(true);
		} else {
		    // View is not within the visible window
			adWebView.getMraidInterface().setViewable(true);
		}	
	}
	
	
	// Hide an interstitial ad view
	public String hide(Bundle data)
	{ 
		
		//if(isInterstitial() && !isExpanded) InterstitialClose();
		if (adPlacementType == MraidInterface.PLACEMENT_TYPES.INTERSTITIAL)
		{
			adWebView.setVisibility(View.GONE);
			return null;
		}
			
		return "Hide called for ad that is not interstitial";
	}
	
	
	// Show interstitial ad view
	public void showInterstitial(int withDuration)
	{
		if (adPlacementType != MraidInterface.PLACEMENT_TYPES.INTERSTITIAL)
		{
			adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "AdViewContainer", "Attempt to show interstitial with wrong placement");
			return;
		}

		// Set ad state to default if it is hidden
		MraidInterface mraid = adWebView.getMraidInterface();
		if (mraid.getState() == MraidInterface.STATES.HIDDEN)
		{
			mraid.setState(MraidInterface.STATES.DEFAULT);
		}
		
		// create dialog object for showing interstitial ad
		adSizeUtilities.showInterstitialDialog(showCloseInterstitialTime, withDuration);
	}
	
	
	public String playVideo(Bundle data)
	{
		String mediaUri = null;
		try
		{
			mediaUri = data.getString(AdMessageHandler.PLAYBACK_URL);
		}
		catch(Exception ex)
		{
			String error = "Error getting playback uri for video: " + ex.getMessage();
			adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "AdViewContainer.playVideo", error);
			return error;
		}
		
		if (mediaUri != null)
		{
			return adWebView.getMraidInterface().getDeviceFeatures().playVideo(mediaUri);
		}
		else
		{
			String error = "No playback uri for video found, skipping...";
			adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "AdViewContainer.playVideo", error);
			return error;
		}
	}

	
	public String createCalendarEvent(Bundle data)
	{
		try
		{
			String description = data.getString(MraidInterface.get_CALENDAR_EVENT_PARAMETERS_name(MraidInterface.CALENDAR_EVENT_PARAMETERS.DESCRIPTION));
			if (description == null)
			{
				description = "";
			}
			
			String summary = data.getString(MraidInterface.get_CALENDAR_EVENT_PARAMETERS_name(MraidInterface.CALENDAR_EVENT_PARAMETERS.SUMMARY));
			if (summary == null)
			{
				summary = "";
			}
			
			String location = data.getString(MraidInterface.get_CALENDAR_EVENT_PARAMETERS_name(MraidInterface.CALENDAR_EVENT_PARAMETERS.LOCATION));
			if (location == null)
			{
				location = "";
			}
			
			String start = data.getString(MraidInterface.get_CALENDAR_EVENT_PARAMETERS_name(MraidInterface.CALENDAR_EVENT_PARAMETERS.START));
			if (start == null)
			{
				String error = "Missing calendar event start date/time, cannot continue";
				adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "AdViewContainer.createCalendar", error);
				return error;
			}
			
			String end = data.getString(MraidInterface.get_CALENDAR_EVENT_PARAMETERS_name(MraidInterface.CALENDAR_EVENT_PARAMETERS.END));
			if (end == null)
			{
				String error = "Missing calendar event end date/time, cannot continue";
				adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "AdViewContainer.createCalendar", error);
				return error;
			}
			
			return adWebView.getMraidInterface().getDeviceFeatures().createCalendarInteractive(description, location, summary, start, end);
		}
		catch(Exception ex)
		{
			String error = "Error getting parameters for calendar event: " + ex.getMessage();
			adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "AdViewContainer.createCalendar", error);
			return error;
		}
	}
	
	
	//private int resizeOldWidth = 0;
	//private int resizeOldHeight = 0;
	
	
	/**
	 * Resize the ad view container; this method is invoked by the javascript interface and runs on
	 * the UI thread via a handler invocation, with data passed as part of the message bundle.
	 */
	public String resize(Bundle data)
	{
		//resizeOldWidth = this.getWidth();
		//resizeOldHeight = this.getHeight();
		
		// You can only invoke resize from the default ad state, or from the resized state (to further change the size)
		if ((adWebView.getMraidInterface().getState() == MraidInterface.STATES.DEFAULT) ||
			(adWebView.getMraidInterface().getState() == MraidInterface.STATES.RESIZED))
		{
			adReloadTimer.stopTimer(false); // stop ad refresh timer
			return adSizeUtilities.startResize(data);		
		} 
		
		return MASTAdConstants.STR_RICHMEDIA_ERROR_RESIZE;
	}
	
	
	public String open(Bundle data)
	{
		String  url = data.getString(AdMessageHandler.OPEN_URL);
		
		try
		{
			url = URLDecoder.decode(url, "UTF-8");
			Uri uri = Uri.parse(url);
			
			// for "action" urls (sms, tel, mailto) just invoke, for others do a fetch/open
			if (uri.getScheme().equalsIgnoreCase("mailto"))
			{
				MailTo mt = MailTo.parse(url);
				Intent i = new Intent(Intent.ACTION_SEND);
		        i.setType("text/plain");
		        i.putExtra(Intent.EXTRA_EMAIL, new String[]{mt.getTo()});
		        i.putExtra(Intent.EXTRA_SUBJECT, mt.getSubject());
		        i.putExtra(Intent.EXTRA_CC, mt.getCc());
		        i.putExtra(Intent.EXTRA_TEXT, mt.getBody());
		        context.startActivity(i);
		        
            	return null;
			}
			else if (uri.getScheme().equalsIgnoreCase("sms"))
			{
				Intent i = new Intent(Intent.ACTION_VIEW);
				i.setType("vnd.android-dir/mms-sms");
				
				// android doesn't parse these urls correctly for all os versions...
				String phoneNumber = url.substring(4);
				i.putExtra("address", phoneNumber);
				
				// smsIntent.putExtra("sms_body","Body of Message");
				
				context.startActivity(i);
				
				return null;
			}
			else if (uri.getScheme().equalsIgnoreCase("tel"))
			{
				Intent i = new Intent(Intent.ACTION_DIAL); // could use ACTION_CALL to immedidately place the call, but this is better
				i.setData(uri);
				context.startActivity(i);
				
				return null;
			}
			else
			{
				// Pass options for dialog through to creator
				AdDialogFactory.DialogOptions options = new AdDialogFactory.DialogOptions();
				options.backgroundColor = Color.BLACK;
				options.noClose = true; // no add-on close function, just browser default
				
				return adSizeUtilities.openInBackgroundThread(options, url);
			}
		}
		catch (Exception e)
		{
			adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "openUrlInExternalBrowser","url=" + url +"; error="+e.getMessage());
			return e.getMessage();
		}
	}
	
	
	public String expand(Bundle data)
	{
		// You can only invoke expand from the default or expanded ad states
		if ((adWebView.getMraidInterface().getState() == MraidInterface.STATES.DEFAULT) ||
			(adWebView.getMraidInterface().getState() == MraidInterface.STATES.EXPANDED))
		{
			return adSizeUtilities.startExpand(data, adReloadTimer);
		}
		
		return MASTAdConstants.STR_RICHMEDIA_ERROR_EXPAND; // new, more specific error
	}
	
	
	public String updateOrientationProperties(Bundle data)
	{
		if (adWebView.getMraidInterface().getState() == MraidInterface.STATES.EXPANDED)
		{
			return adSizeUtilities.setOrientationProperties(data);
		}

		return null;
	}
	
		
	//
	// Injection header
	//

	
	/** Default viewport string injected into ad view **/
	private static final String defaultViewportDefinition =
		"<meta name=\"viewport\" content=\"user-scalable=no\"/>";
	
	/** Default body style css injected into ad view **/
	private static final String defaultBodyStyle =
		"<style>body{margin: 0px; padding: 0px;}</style>";
		
	
	/**
	 * Get current injection header code string.
	 * @return Current injection header value.
	 */
	private String getInjectionHeaderCode()
	{
		// Default fragment, revised as of 2.12 SDK
		return defaultViewportDefinition + defaultBodyStyle;
	}
	
	
	
	//
	// Misc
	//
	
	

	/**
	 * Provide access to the diagnostic log object created internal to this view
	 * 
	 * @return MASTAdLog usable for diagnostics debug logging
	 */
	public MASTAdLog getLog()
	{
		return adLog;
	}
			
	
	public Handler getHandler()
	{
		return handler;
	}

	
	public String getLastRequest()
	{
		return lastRequest;
	}
	
	
	public String getLastResponse()
	{
		return lastResponse.responseData;
	}

	
	public MASTAdRequest getAdRequest()
	{
		return adserverRequest;
	}

	
	public MASTAdDelegate getAdDelegate()
	{
		return adDelegate;
	}

	
	public void setDefaultImageResource(Integer resource)
	{
		defaultImageResource = resource;
	}
	
	
	public Integer getDefaultImageResource()
	{
		return defaultImageResource;
	}
	
	
	private class SetBackgroundResourceAction implements Runnable
	{
		private Integer backgroundResource;
		
		public SetBackgroundResourceAction(Integer backgroundResource)
		{
			this.backgroundResource = backgroundResource;
		}
		
		@Override
		public void run()
		{
			try
			{
				if(backgroundResource != null)
				{
					self.setBackgroundResource(backgroundResource);
					self.setBackgroundColor(0);					
				}
			}
			catch (Exception e)
			{
				adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "SetBackgroundResourceAction", e.getMessage());
			}
		}
	}	
	
	
	/*
	public int getAutoCloseInterstitialTime()
	{
		return autoCloseInterstitialTime;
	}
	
	
	public void setAutoCloseInterstitialTime(int time)
	{
		autoCloseInterstitialTime = time;
	}
	
	
	public int getShowCloseInterstitialTime()
	{
		return showCloseInterstitialTime;
	}
	
	
	public void setShowCloseInterstitialTime(int time)
	{
		showCloseInterstitialTime = time;
	}
	*/
	
		
	protected void onAttachedToWindow()
	{
		adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "Attached to Window", "");

		adReloadTimer.startTimer();
		
		//StartLoadContent(getContext(), this);
		
		super.onAttachedToWindow();

		if (adDelegate != null)
		{
			MASTAdDelegate.AdActivityEventHandler activityHandler = adDelegate.getAdActivityEventHandler(); 
			if (activityHandler != null)
			{
				activityHandler.onAdAttachedToActivity((MASTAdView)this);
			}
		}

		// ??? If the ad content was downloaded while the view was not attached to a window
		// they we attempt to "install" it now; however, need to make sure we don't get into
		// a race condition between set content completing and this method at the same time.
		if ((lastResponse != null) && (lastResponse.hasContent()) &&
			((adWebView == null) || 
			 ((adWebView.getMraidInterface().getState() == MraidInterface.STATES.LOADING) ||
			  (adWebView.getMraidInterface().getState() == MraidInterface.STATES.DEFAULT))))
		{
			if (lastResponse.adType == MASTAdConstants.AD_TYPE_EXTERNAL_THIRDPARTY)
			{
				notifyExternalThirdPartyAd(lastResponse);
			}
			else
			{
				setAdContent(lastResponse); // If we have ad content from "before", load it
			}
		}
		
		if ((adWebView != null) &&
			((adWebView.getMraidInterface().getState() == MraidInterface.STATES.LOADING) ||
			 (adWebView.getMraidInterface().getState() == MraidInterface.STATES.DEFAULT)))
		{
			adWebView.getLocationOnScreen(coordinates); // getLocationInWindow() for relative
			adWebView.getMraidInterface().setCurrentPosition(coordinates[0], coordinates[1], adWebView.getWidth(), adWebView.getHeight());
			//adWebView.getMraidInterface().setCurrentPosition(adWebView.getLeft(), adWebView.getTop(), adWebView.getWidth(), adWebView.getHeight());
		}
		
		// Notify ad that viewable state has changed
		adWebView.getMraidInterface().setViewable(true);
		//setViewable(adWebView); // if only there was a reliable way to get notified if we're scrolling
	}
	
	
	protected void onDetachedFromWindow()
	{
		adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "Detached from Window", "");
		
		/*
		if (image!=null)
		{
			image.recycle();
			image = null;
		}		
		*/
		
		if (locationListener != null)
		{
			locationListener.stop();
			locationListener = null;
		}
		
		adReloadTimer.stopTimer(true); // stop timer, clean up object
		
		// stop loading any content
		ContentManager.getInstance(this).stopLoadContent(this);

		super.onDetachedFromWindow();
		
		if (adDelegate != null)
		{
			MASTAdDelegate.AdActivityEventHandler activityHandler = adDelegate.getAdActivityEventHandler(); 
			if (activityHandler != null)
			{
				// If the activity is going away, and this callback attempts to do things with the UI
				// it can fail, so protected against an exception.
				try 
				{
					activityHandler.onAdDetachedFromActivity((MASTAdView)this);
				}
				catch (Exception e)
				{
					adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "onAdDetachedFromActivity - exceptioin", e.getMessage());
				}
			}
		}
		
		// Notify ad that viewable state has changed
		adWebView.getMraidInterface().setViewable(false);
	}
	
	
	synchronized public void setLocationDetection(boolean detect, Integer minWaitMillis, Float minMoveMeters)
	{
		if (detect)
		{		 	
			int isAccessFineLocation = context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
	    	if (isAccessFineLocation == PackageManager.PERMISSION_GRANTED)
	    	{
				locationListener = new AdLocationListener(context, minWaitMillis, minMoveMeters, LocationManager.GPS_PROVIDER, Looper.getMainLooper(), adLog)
				{
					public void fail(String m)
					{
						// Nothing (legacy)
					}
					
					public void success(Location location)
					{
						try
						{
							double latitude = location.getLatitude();
							adserverRequest.setProperty(MASTAdRequest.parameter_latitude, Double.toString(latitude));
							
							double longitude = location.getLongitude();
							adserverRequest.setProperty(MASTAdRequest.parameter_longitude, Double.toString(longitude)); 
							
							adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "LocationDetection changed", "("+latitude+";"+longitude+")");								
			    		}
						catch (Exception e)
						{
			    			adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "LocationDetection",e.getMessage());
			    		}									
					}
				};

		    	if (locationListener.isAvailable())
		    	{
		    		adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "LocationDetection", "Start listening for location updates");
		    		locationListener.start();
		    	}
		    	else
		    	{
		    		adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "LocationDetection", "Location updates not available");
		    		locationListener.stop();
		    		locationListener = null;
		    	}
			}
	    	else
	    	{
	    		adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "LocationDetection", "No permission for GPS");
	    	}
    	}
		else
		{
			// If listening, stop
			if (locationListener != null)
			{
				locationListener.stop();
				locationListener = null;
			}
			
			adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "LocationDetection", "Stop listening for location updates");
		}
	}

	
	synchronized public boolean getLocationDetection()
	{
		// If listener was successfully created and reports it is available, yes
		if ((locationListener != null) && (locationListener.isAvailable()))
		{
			return true;	
		}
		
		// otherwise, no
		return false;
	}

	
	public void setUseInternalBrowser(boolean flag)
	{
		useInternalBrowser = flag;
	}
	
	
	public boolean getUseInternalBrowser()
	{
		return useInternalBrowser;
		
	}
	
	
	private void showCloseButtonWorker()
	{
		Thread closeThread = new Thread()
		{
			public void run()
			{
				final int visible;
				if (isShowCloseOnBanner)
				{
					visible = View.VISIBLE;
					try { Thread.sleep(showCloseInterstitialTime * 1000); } catch(Exception e) { }
				}
				else
				{
					visible = View.GONE;
				}
				
				handler.post(new Runnable()
				{
					public void run()
					{
						// Create close button for banner
						if (bannerCloseButton == null)
						{
							bannerCloseButton = createCloseButton(context, createCloseClickListener());
							self.addView(bannerCloseButton);
						}
						
						bannerCloseButton.setVisibility(visible);
					}
				});
			}
		};
		closeThread.setName("[AdViewContainer] showCloseButton");
		closeThread.start();		
	}


	public void showCloseButton(boolean flag, int afterDelay)
	{
		showCloseInterstitialTime = afterDelay;
		
		boolean change = false;
		if (flag != isShowCloseOnBanner)
		{
			change = true;
		}
		
		isShowCloseOnBanner = flag;

		if (change)
		{
			showCloseButtonWorker();
		}
	}
	
	
	private OnClickListener createCloseClickListener()
	{
		return new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				handler.post(new Runnable()
				{
					@Override
					public void run()
					{
						// For interstitial ad, dismiss dialog;
						// for banner, remove the view (and if
						// richmedia, set state to hidden?)
						if (adPlacementType == MraidInterface.PLACEMENT_TYPES.INTERSTITIAL)
						{							
							// dismiss dialog containing interstitial view
							adSizeUtilities.dismissDialog();
						}
						else
						{	
							// Remove ad container from parent, hiding it
							ViewGroup parent = (ViewGroup)self.getParent();
							if (parent != null)
							{
								// Remove container from parent
								parent.removeView(self);
							}
						}
					}
				});
			}
		};
	}
	
	
	public Button createCloseButton(Context c, View.OnClickListener clickListener)
	{
		Button b;
		
		if (customCloseButton == null)
		{
			b = new Button(c);
			b.setMinHeight(50);
			b.setMinWidth(50);
			b.setVisibility(View.GONE); // default is not present
			
			
			b.setText("Close"); // string, allow customizing
		}
		else
		{
			b = customCloseButton;
		}
		
		
		// Position button in upper right
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
		b.setLayoutParams(layoutParams);
		
		b.setOnClickListener(clickListener);
		
		return b;
	}

	
	public void setCustomCloseButton(Button closeButton)
	{
		customCloseButton = closeButton;
	}

	
	public Button getCustomCloseButton()
	{
		return customCloseButton;
	}

	
	public void onOrientationChange(int orientationAngle, int screenOrientation)
	{
		// Update size/position (triggering a size change event if appropriate)
		WindowManager windowManager = (WindowManager) ((Activity)context).getSystemService(Context.WINDOW_SERVICE);
		if (metrics == null)
		{
			metrics = new DisplayMetrics();
		}
		windowManager.getDefaultDisplay().getMetrics(metrics);		
		adSizeUtilities.setMetrics(metrics);
		
		// Update current position values
		//adWebView.getMraidInterface().setCurrentPosition(adWebView.getLeft(), adWebView.getTop(), w, h);
		if (adWebView != null)
		{
			adWebView.getLocationOnScreen(coordinates); // getLocationInWindow() for relative
			adWebView.getMraidInterface().setCurrentPosition(coordinates[0], coordinates[1], adWebView.getWidth(), adWebView.getHeight());
			//adWebView.getMraidInterface().setCurrentPosition(adWebView.getLeft(), adWebView.getTop(), adWebView.getWidth(), adWebView.getHeight());
		}
		
		//adWebView.getMraidInterface().setOrientation(orientationAngle);
		
		// If ad is expanded and a 2 part creative caused a new view to be created,
		// inject events into that one also.
		if ((adWebView.getMraidInterface().getState() == MraidInterface.STATES.EXPANDED) &&
			(adSizeUtilities.getExpandedAdView() != null))
		{
			AdWebView expandedWebView = adSizeUtilities.getExpandedAdView(); 
			//expandedWebView.getMraidInterface().setCurrentPosition(expandedWebView.getLeft(), expandedWebView.getTop(), expandedWebView.getWidth(), expandedWebView.getHeight());
			expandedWebView.getMraidInterface().setCurrentPosition(coordinates[0], coordinates[1], adWebView.getWidth(), adWebView.getHeight());
			
			//expandedWebView.getMraidInterface().setOrientation(orientationAngle);
		}
	}
	
	
	public void richmediaEvent(String method, String parameter)
	{
		if (adDelegate != null)
		{
			RichmediaEventHandler handler = adDelegate.getRichmediaEventHandler(); 
			if (handler != null)
			{
				handler.onRichmediaEvent((MASTAdView)this, method, parameter);
			}
		}
	}
}
