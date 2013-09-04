/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package org.ormma.controller;

import org.json.JSONException;
import org.json.JSONObject;
import org.ormma.controller.util.OrmmaConfigurationBroadcastReceiver;
import org.ormma.view.OrmmaView;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import de.guj.ems.mobile.sdk.util.SdkLog;

/**
 * The Class OrmmaDisplayController.  A ormma controller for handling display related operations
 */
public class OrmmaDisplayController extends OrmmaController {

	//tag for SdkLogging
	private static final String SdkLog_TAG = "OrmmaDisplayController";
	
	private WindowManager mWindowManager;
	private boolean bMaxSizeSet = false;
	private int mMaxWidth = -1;
	private int mMaxHeight = -1;
	private OrmmaConfigurationBroadcastReceiver mBroadCastReceiver;
	private float mDensity;
	
	/**
	 * Instantiates a new ormma display controller.
	 *
	 * @param adView the ad view
	 * @param c the context
	 */
	public OrmmaDisplayController(OrmmaView adView, Context c) {
		super(adView, c);
		DisplayMetrics metrics = new DisplayMetrics();
		mWindowManager = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
		mWindowManager.getDefaultDisplay().getMetrics(metrics);
		mDensity = metrics.density;

	}

	/**
	 * Resize the view.
	 *
	 * @param width the width
	 * @param height the height
	 */
	@JavascriptInterface
	public void resize(int width, int height) {
		SdkLog.d(SdkLog_TAG, "resize: width: " + width + " height: " + height);
		if (((mMaxHeight > 0) && (height > mMaxHeight)) || ((mMaxWidth > 0) && (width > mMaxWidth))) {
			mOrmmaView.raiseError("Maximum size exceeded", "resize");
		} else
			mOrmmaView.resize((int) (mDensity * width), (int) (mDensity * height));

	}

	/**
	 * Open a browser
	 *
	 * @param url the url
	 * @param back show the back button
	 * @param forward show the forward button
	 * @param refresh show the refresh button
	 */
	@JavascriptInterface
	public void open(String url, boolean back, boolean forward, boolean refresh) {
		SdkLog.d(SdkLog_TAG, "open: url: " + url + " back: " + back + " forward: " + forward + " refresh: " + refresh);
		if(!URLUtil.isValidUrl(url)){
			mOrmmaView.raiseError("Invalid url", "open");
		}else{
			mOrmmaView.open(url, back, forward, refresh);
		}
		

	}
	
	/**Open map
	 * @param url - map url
	 * @param fullscreen - boolean indicating whether map to be launched in full screen
	 */
	@JavascriptInterface
	public void openMap(String url, boolean fullscreen) {
		SdkLog.d(SdkLog_TAG, "openMap: url: " + url);
		mOrmmaView.openMap(url, fullscreen);
	}
	

	/**
	 * Play audio
	 * @param url - audio url to be played
	 * @param autoPlay - if audio should play immediately
	 * @param controls - should native player controls be visible
	 * @param loop - should video start over again after finishing
	 * @param position - should audio be included with ad content
	 * @param startStyle - normal/full screen (if audio should play in native full screen mode)
	 * @param stopStyle - normal/exit (exit if player should exit after audio stops)
	 */
	@JavascriptInterface
	public void playAudio(String url, boolean autoPlay, boolean controls, boolean loop, boolean position, String startStyle, String stopStyle) {
		SdkLog.d(SdkLog_TAG, "playAudio: url: " + url + " autoPlay: " + autoPlay + " controls: " + controls + " loop: " + loop + " position: " + position + " startStyle: " + startStyle + " stopStyle: "+stopStyle);
		if(!URLUtil.isValidUrl(url)){
			mOrmmaView.raiseError("Invalid url", "playAudio");
		}else{
			mOrmmaView.playAudio(url, autoPlay, controls, loop, position, startStyle, stopStyle);
		}
		
	}
	
	
	/**
	 * Play video
	 * @param url - video url to be played
	 * @param audioMuted - should audio be muted
	 * @param autoPlay - should video play immediately
	 * @param controls  - should native player controls be visible
	 * @param loop - should video start over again after finishing
	 * @param position - top and left coordinates of video in pixels if video should play inline
	 * @param startStyle - normal/fullscreen (if video should play in native full screen mode)
	 * @param stopStyle - normal/exit (exit if player should exit after video stops)
	 */
	@JavascriptInterface
	public void playVideo(String url, boolean audioMuted, boolean autoPlay, boolean controls, boolean loop, int[] position, String startStyle, String stopStyle) {
		SdkLog.d(SdkLog_TAG, "playVideo: url: " + url + " audioMuted: " + audioMuted + " autoPlay: " + autoPlay + " controls: " + controls + " loop: " + loop + " x: " + position[0] + 
				" y: " + position[1] + " width: " + position[2] + " height: " + position[3] + " startStyle: " + startStyle + " stopStyle: " + stopStyle);
		Dimensions d = null;
		if(position[0] != -1) {
			d = new Dimensions();
			d.x = position[0];
			d.y = position[1];
			d.width = position[2];
			d.height = position[3];
			d = getDeviceDimensions(d);
		}		
		if(!URLUtil.isValidUrl(url)){
			mOrmmaView.raiseError("Invalid url", "playVideo");
		}else{
			mOrmmaView.playVideo(url, audioMuted, autoPlay, controls, loop, d, startStyle, stopStyle);
		}
	}

	/**
	 * Get Device dimensions
	 * @param d - dimensions received from java script
	 * @return
	 */
	private Dimensions getDeviceDimensions(Dimensions d){
		d.width *= mDensity;
		d.height *= mDensity;
		d.x *= mDensity;
		d.y *= mDensity;
		if (d.height < 0)
			d.height = mOrmmaView.getHeight();
		if (d.width < 0)
			d.width = mOrmmaView.getWidth();
		int loc[] = new int[2];
		mOrmmaView.getLocationInWindow(loc);
		if (d.x < 0)
			d.x = loc[0];
		if (d.y < 0) {
			int topStuff = 0;// ((Activity)mContext).findViewById(Window.ID_ANDROID_CONTENT).getTop();
			d.y = loc[1] - topStuff;
		}
		return d;
	}
	
	/**
	 * Expand the view
	 *
	 * @param dimensions the dimensions to expand to
	 * @param URL the uRL
	 * @param properties the properties for the expansion
	 */
	@JavascriptInterface
	public void expand(String dimensions, String URL, String properties) {
        SdkLog.d(SdkLog_TAG, "expand: dimensions: " + dimensions + " url: " + URL + " properties: " + properties);
		try {
			Dimensions d = (Dimensions) getFromJSON(new JSONObject(dimensions), Dimensions.class);
			mOrmmaView.expand(getDeviceDimensions(d), URL, (Properties) getFromJSON(new JSONObject(properties), Properties.class));
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NullPointerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Close the view
	 */
	@JavascriptInterface
	public void close() {
		SdkLog.d(SdkLog_TAG, "close");
		mOrmmaView.close();
	}

	/**
	 * Hide the view
	 */
	@JavascriptInterface
	public void hide() {
		SdkLog.d(SdkLog_TAG, "hide");
		mOrmmaView.hide();
	}

	/**
	 * Show the view
	 */
	@JavascriptInterface
	public void show() {
		SdkLog.d(SdkLog_TAG, "show");
		mOrmmaView.show();
	}

	/**
	 * Checks if is visible.
	 *
	 * @return true, if is visible
	 */
	@JavascriptInterface
	public boolean isVisible() {
		return (mOrmmaView.getVisibility() == View.VISIBLE);
	}

	/**
	 * Dimensions.
	 *
	 * @return the string
	 */
	@JavascriptInterface
	public String dimensions() {
		return "{ \"top\" :" + (int) (mOrmmaView.getTop() / mDensity) + "," + "\"left\" :"
				+ (int) (mOrmmaView.getLeft() / mDensity) + "," + "\"bottom\" :"
				+ (int) (mOrmmaView.getBottom() / mDensity) + "," + "\"right\" :"
				+ (int) (mOrmmaView.getRight() / mDensity) + "}";
	}

	/**
	 * Gets the orientation.
	 *
	 * @return the orientation
	 */
	@JavascriptInterface
	public int getOrientation() {
		int orientation = mWindowManager.getDefaultDisplay().getRotation();
		int ret = -1;
		switch (orientation) {
		case Surface.ROTATION_0:
			ret = 0;
			break;

		case Surface.ROTATION_90:
			ret = 90;
			break;

		case Surface.ROTATION_180:
			ret = 180;
			break;

		case Surface.ROTATION_270:
			ret = 270;
			break;
		}
		SdkLog.d(SdkLog_TAG, "getOrientation: " +  ret);
		return ret;
	}

	/**
	 * Gets the screen size.
	 *
	 * @return the screen size
	 */
	@JavascriptInterface
	public String getScreenSize() {
		DisplayMetrics metrics = new DisplayMetrics();
		mWindowManager.getDefaultDisplay().getMetrics(metrics);

		return "{ width: " + (int) (metrics.widthPixels / metrics.density) + ", " + "height: "
				+ (int) (metrics.heightPixels / metrics.density) + "}";
	}

	/**
	 * Gets the size.
	 *
	 * @return the size
	 */
	@JavascriptInterface
	public String getSize() {
		return mOrmmaView.getSize();
	}

	/**
	 * Gets the max size.
	 *
	 * @return the max size
	 */
	@JavascriptInterface
	public String getMaxSize() {
		if (bMaxSizeSet)
			return "{ width: " + mMaxWidth + ", " + "height: " + mMaxHeight + "}";
		else
			return getScreenSize();
	}

	/**
	 * Sets the max size.
	 *
	 * @param w the w
	 * @param h the h
	 */
	@JavascriptInterface
	public void setMaxSize(int w, int h) {
		bMaxSizeSet = true;
		mMaxWidth = w;
		mMaxHeight = h;
	}

	/**
	 * On orientation changed.
	 *
	 * @param orientation the orientation
	 */
	public void onOrientationChanged(int orientation) {
		String script = "window.ormmaview.fireChangeEvent({ orientation: " + orientation + "});";
		SdkLog.d(SdkLog_TAG, script );
		mOrmmaView.injectJavaScript(script);
	}

	/**
	 * SdkLog html.
	 *
	 * @param html the html
	 */
	public void SdkLogHTML(String html) {
		SdkLog.d(SdkLog_TAG, html);
	}

	/* (non-Javadoc)
	 * @see com.ormma.controller.OrmmaController#stopAllListeners()
	 */
	@Override
	public void stopAllListeners() {
		stopConfigurationListener();
		mBroadCastReceiver = null;
	}

	@JavascriptInterface
	public void stopConfigurationListener() {
		try {
			mContext.unregisterReceiver(mBroadCastReceiver);
		} catch (Exception e) {
		}
	}
	
	@JavascriptInterface
	public void startConfigurationListener() {
		try {
			if(mBroadCastReceiver == null) 
				mBroadCastReceiver = new OrmmaConfigurationBroadcastReceiver(this);
			mContext.registerReceiver(mBroadCastReceiver, new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
		}catch(Exception e) {
		}
	}
}
