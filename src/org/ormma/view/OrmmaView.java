/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

/**
 * 
 * This is the view to place into a layout to implement ormma functionality.
 * It can be used either via xml or programatically
 * 
 * It is a subclass of the standard WebView which brings with it all the standard
 * functionality as well as the inherent bugs on some os versions.
 * 
 * Webviews have a tendency to leak on orientation in older versions of the android OS
 * this can be minimized by using an application context, but this will break the launching
 * of subwindows (such as alert calls from javascript)
 * 
 * 
 */
package org.ormma.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Locale;

import org.ormma.controller.OrmmaController.Dimensions;
import org.ormma.controller.OrmmaController.PlayerProperties;
import org.ormma.controller.OrmmaController.Properties;
import org.ormma.controller.OrmmaUtilityController;
import org.ormma.controller.util.OrmmaPlayer;
import org.ormma.controller.util.OrmmaPlayerListener;
import org.ormma.controller.util.OrmmaUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.TypedArray;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.VideoView;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;

/*
 import com.google.android.maps.MapActivity;
 import com.google.android.maps.MapView;
 */

/**
 * This is the view to place into a layout to implement ormma functionality. It
 * can be used either via xml or programatically
 * 
 * It is a subclass of the standard WebView which brings with it all the
 * standard functionality as well as the inherent bugs on some os versions.
 * 
 * Webviews have a tendency to leak on orientation in older versions of the
 * android OS this can be minimized by using an application context, but this
 * will break the launching of subwindows (such as alert calls from javascript)
 * 
 * It is important to not use any of the layout constants elsewhere in the same
 * view as things will get confused. Normally this is not an issue as generated
 * layout constants will not interfere.
 */
public class OrmmaView extends WebView implements OnGlobalLayoutListener {

	public enum ACTION {
		PLAY_AUDIO, PLAY_VIDEO
	}

	/**
	 * The Class NewLocationReciever.
	 */
	public static abstract class NewLocationReciever {

		/**
		 * On new location.
		 * 
		 * @param v
		 *            the v
		 */
		public abstract void OnNewLocation(ViewState v);
	}

	final static class OrmmaHandler extends Handler {

		OrmmaView ref = null;

		OrmmaHandler(OrmmaView ref) {
			this.ref = ref;
		}

		@Override
		public void handleMessage(Message msg) {

			Bundle data = msg.getData();
			switch (msg.what) {
			case MESSAGE_SEND_EXPAND_CLOSE:
				if (ref.mListener != null) {
					ref.mListener.onExpandClose();
				}
				break;
			case MESSAGE_RESIZE: {
				ref.mViewState = ViewState.RESIZED;
				ViewGroup.LayoutParams lp = ref.getLayoutParams();
				lp.height = data.getInt(RESIZE_HEIGHT, lp.height);
				lp.width = data.getInt(RESIZE_WIDTH, lp.width);
				String injection = "window.ormmaview.fireChangeEvent({ state: \'resized\',"
						+ " size: { width: "
						+ lp.width
						+ ", "
						+ "height: "
						+ lp.height + "}});";
				ref.injectJavaScript(injection);
				ref.requestLayout();
				if (ref.mListener != null)
					ref.mListener.onResize();
				break;
			}
			case MESSAGE_CLOSE: {
				switch (ref.mViewState) {
				case RESIZED:
					ref.closeResized();
					break;
				case EXPANDED:
					ref.closeExpanded();
					break;
				default: {
				}
				}
				break;
			}
			case MESSAGE_HIDE: {
				ref.setVisibility(View.INVISIBLE);
				String injection = "window.ormmaview.fireChangeEvent({ state: \'hidden\' });";

				ref.injectJavaScript(injection);
				break;
			}
			case MESSAGE_SHOW: {
				String injection = "window.ormmaview.fireChangeEvent({ state: \'default\' });";
				ref.injectJavaScript(injection);
				ref.setVisibility(View.VISIBLE);
				break;
			}
			case MESSAGE_EXPAND: {
				ref.doExpand(data);
				break;
			}
			case MESSAGE_OPEN: {
				ref.mViewState = ViewState.LEFT_BEHIND;
				break;
			}

			case MESSAGE_PLAY_AUDIO: {
				ref.playAudioImpl(data);
				break;
			}

			case MESSAGE_PLAY_VIDEO: {
				ref.playVideoImpl(data);
				break;
			}
			case MESSAGE_RAISE_ERROR:
				String strMsg = data.getString(ERROR_MESSAGE);
				String action = data.getString(ERROR_ACTION);
				String injection = "window.ormmaview.fireErrorEvent(\""
						+ strMsg + "\", \"" + action + "\")";
				ref.injectJavaScript(injection);
				break;
			}
			super.handleMessage(msg);
		}
	}

	/**
	 * The listener interface for receiving ormmaView events. The class that is
	 * interested in processing a ormmaView event implements this interface, and
	 * the object created with that class is registered with a component using
	 * the component's <code>addOrmmaViewListener<code> method. When
	 * the ormmaView event occurs, that object's appropriate
	 * method is invoked.
	 * 
	 * @see OrmmaViewEvent
	 */
	public interface OrmmaViewListener {

		/**
		 * On Handling Requests
		 * 
		 * @param url
		 *            The url whose protocol has been registered.
		 */

		abstract void handleRequest(String url);

		/**
		 * On event fired.
		 * 
		 * @return true, if successful
		 */
		abstract boolean onEventFired();

		/**
		 * On expand.
		 * 
		 * @return true, if successful
		 */
		abstract boolean onExpand();

		/**
		 * On expand close.
		 * 
		 * @return true, if successful
		 */
		abstract boolean onExpandClose();

		/**
		 * On ready.
		 * 
		 * @return true, if successful
		 */
		abstract boolean onReady();

		/**
		 * On resize.
		 * 
		 * @return true, if successful
		 */
		abstract boolean onResize();

		/**
		 * On resize close.
		 * 
		 * @return true, if successful
		 */
		abstract boolean onResizeClose();
	}

	/**
	 * The Class ScrollEater.
	 */
	class ScrollEater extends SimpleOnGestureListener {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * android.view.GestureDetector.SimpleOnGestureListener#onScroll(android
		 * .view.MotionEvent, android.view.MotionEvent, float, float)
		 * 
		 * Gesture detector for eating scroll events
		 */
		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2,
				float distanceX, float distanceY) {
			return true;
		}
	}

	/**
	 * The Class TimeOutRunnable. A timertask for terminating the load if it
	 * takes too long. It is started with a delay of 2 seconds and waits for 2
	 * seconds (ORMMA standard).
	 */
	public class TimeOutRunnable implements Runnable {

		int mProgress = 0;
		int mCount = 0;
		boolean mRun = true;

		public void cancel() {
			mRun = false;
		}

		@Override
		public void run() {
			int progress = getProgress();

			if (progress == 100 || !mRun) {
				SdkLog.d(SdkLog_TAG, !mRun ? "TimeOutRunnable: Cancelled."
						: "TimeOutRunnable: Finished loading.");
				return;
			} else {
				if (mProgress == progress) {
					mCount++;
					if (mCount == 10) {
						try {
							SdkLog.w(SdkLog_TAG,
									"TimeOutRunnable: Timeout reached!");
							stopLoading();
						} catch (Exception e) {
							e.printStackTrace();
						}
						return;
					}
				}
			}
			mProgress = progress;
			mTimeOutHandler.postDelayed(this, 1000);
		}
	}

	/**
	 * enum representing possible view states
	 */
	public enum ViewState {
		DEFAULT, RESIZED, EXPANDED, HIDDEN, LEFT_BEHIND, OPENED;
	}
	private static final String SdkLog_TAG = "OrmmaView";

	// static for accessing xml attributes
	private static int[] attrs = { android.R.attr.maxWidth,
			android.R.attr.maxHeight };

	// 1 MB Cache for webview
	private final static long WEBVIEW_CACHE_SIZE = 1048576;
	// Messaging constants
	private static final int MESSAGE_RESIZE = 1000;
	private static final int MESSAGE_CLOSE = 1001;
	private static final int MESSAGE_HIDE = 1002;

	private static final int MESSAGE_SHOW = 1003;
	private static final int MESSAGE_EXPAND = 1004;
	private static final int MESSAGE_SEND_EXPAND_CLOSE = 1005;
	private static final int MESSAGE_OPEN = 1006;
	private static final int MESSAGE_PLAY_VIDEO = 1007;
	private static final int MESSAGE_PLAY_AUDIO = 1008;
	private static final int MESSAGE_RAISE_ERROR = 1009;
	// Extra constants
	public static final String DIMENSIONS = "expand_dimensions";
	public static final String PLAYER_PROPERTIES = "player_properties";
	public static final String EXPAND_URL = "expand_url";
	public static final String ACTION_KEY = "action";

	private static final String EXPAND_PROPERTIES = "expand_properties";

	private static final String RESIZE_WIDTH = "resize_width";
	private static final String RESIZE_HEIGHT = "resize_height";
	private static final String CURRENT_FILE = "_ormma_current";

	private static final String AD_PATH = "AD_PATH";
	private static final String ERROR_MESSAGE = "message";
	private static final String ERROR_ACTION = "action";
	// layout constants
	protected static final int BACKGROUND_ID = 101;
	protected static final int PLACEHOLDER_ID = 100;

	// private static final AbsoluteLayout.LayoutParams LAYOUT_PARAMS_0 = new
	// AbsoluteLayout.LayoutParams(AbsoluteLayout.LayoutParams.MATCH_PARENT,0);
	//
	// private static final AbsoluteLayout.LayoutParams LAYOUT_PARAMS_1 = new
	// AbsoluteLayout.LayoutParams(AbsoluteLayout.LayoutParams.MATCH_PARENT,
	// AbsoluteLayout.LayoutParams.WRAP_CONTENT);

	public static final int ORMMA_ID = 102;
	// private constants
	private static String mScriptPath/* = null */; // holds the path for the
	// ormma.js
	private static String mBridgeScriptPath /* = null */; // holds the path for
	// the
	// ormma_bridge.js
	private boolean bPageFinished /* = false */; // boolean flag holding the
	// loading
	// state of a page
	private OrmmaUtilityController mUtilityController; // primary javascript
	// bridge
	private float mDensity; // screen pixel density
	private int mContentViewHeight; // height of the content

	private boolean bKeyboardOut; // state of the keyboard
	private int mViewHeight;
	private int mViewWidth;
	private int mIndex; // index of the view within its viewgroup
	private Handler mTimeOutHandler; // handle load timeouts
	private ViewGroup mScrollContainer;
	private boolean mFindScrollContainer = true;
	private int mScrollPosY = -1;

	private TimeOutRunnable mTimeOutRunnable; // check load time

	private GestureDetector mGestureDetector; // gesture detector for capturing

	// unwanted gestures
	private ViewState mViewState = ViewState.DEFAULT; // holds current view

	// state
	// state
	private OrmmaViewListener mListener; // listener for communicated events

	private static OrmmaPlayer player;

	// (back to the parent)
	// public String mDataToInject = null; // javascript to inject into the view
	private String mLocalFilePath; // local path the the ad html

	// URL Protocols registered by the client.
	// if such a protocol is encountered then
	// shouldOverrideUrlLoading will forward the url to the listener
	// by calling handleRequest
	// Should this be a static variable?
	private final HashSet<String> registeredProtocols = new HashSet<String>();

	/*
	 * public OrmmaView(Context context, String mapAPIKey){ super(context);
	 * 
	 * if(!(context instanceof MapActivity)){ throw new
	 * IllegalArgumentException("MapActivity context required"); }
	 * 
	 * this.mapAPIKey = mapAPIKey;
	 * 
	 * initialize(); }
	 * 
	 * public OrmmaView(Context context, String mapAPIKey, OrmmaViewListener
	 * listener){ super(context);
	 * 
	 * if(!(context instanceof MapActivity)){ throw new
	 * IllegalArgumentException("MapActivity context required"); }
	 * 
	 * this.mapAPIKey = mapAPIKey;
	 * 
	 * setListener(listener);
	 * 
	 * initialize(); }
	 */

	private String mapAPIKey;

	private Handler mHandler = new OrmmaHandler(this);

	/**
	 * The webview client used for trapping certain events
	 */
	WebViewClient mWebViewClient = new WebViewClient() {
		public void onLoadResource(WebView view, String url) {
			SdkLog.d(SdkLog_TAG, "lr:" + url);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			super.onPageFinished(view, url);
			bPageFinished = true;
			view.setVisibility(View.VISIBLE);
		}

		@Override
		public void onReceivedError(WebView view, int errorCode,
				String description, String failingUrl) {
			SdkLog.d(SdkLog_TAG, "error:" + description);
			super.onReceivedError(view, errorCode, description, failingUrl);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Uri uri = Uri.parse(url);
			try {
				// If the protocol is registered then forward it to listener.
				if (mListener != null && isRegisteredProtocol(uri)) {
					mListener.handleRequest(url);
					return true;
				}

				if (url.startsWith("tel:")) {
					Intent intent = new Intent(Intent.ACTION_DIAL,
							Uri.parse(url));
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getContext().startActivity(intent);
					return true;
				}

				if (url.startsWith("mailto:")) {
					Intent intent = new Intent(Intent.ACTION_VIEW,
							Uri.parse(url));
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getContext().startActivity(intent);
					return true;
				}

				if (url.startsWith("http") || url.startsWith("https")) {
					SdkLog.i(SdkLog_TAG,
							"ormma_client: Intercepted http/https click with redirect to in-app browser");
					open(url, true, true, true);
					return true;
				}

				Intent intent = new Intent();
				intent.setAction(android.content.Intent.ACTION_VIEW);
				intent.setData(uri);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				getContext().startActivity(intent);
				return true;

			} catch (Exception e) {
				try {
					Intent intent = new Intent();
					intent.setAction(android.content.Intent.ACTION_VIEW);
					intent.setData(uri);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					getContext().startActivity(intent);
					return true;
				} catch (Exception e2) {
					return false;
				}
			}

		};

	};

	/**
	 * The m web chrome client.
	 */
	WebChromeClient mWebChromeClient = new WebChromeClient() {
		@Override
		public boolean onJsAlert(WebView view, String url, String message,
				JsResult result) {
			SdkLog.d("OrmmaView", message);
			return false;
		}
		
		@Override
		public void onShowCustomView(View view, CustomViewCallback callback) {
		    super.onShowCustomView(view, callback);
		    if (view instanceof FrameLayout){
		        FrameLayout frame = (FrameLayout) view;
		        if (frame.getFocusedChild() instanceof VideoView){
		            VideoView video = (VideoView) frame.getFocusedChild();
		            video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
						
						@Override
						public void onCompletion(MediaPlayer mp) {
							SdkLog.i(SdkLog_TAG, "video complete ");
						}
					});
		            video.setOnErrorListener(new MediaPlayer.OnErrorListener() {
						
						@Override
						public boolean onError(MediaPlayer mp, int what, int extra) {
							SdkLog.e(SdkLog_TAG, "video error " + what + ", " + extra);
							switch (what) {
							case MediaPlayer.MEDIA_ERROR_IO:
								SdkLog.w(SdkLog_TAG, "IO Error"); break;
							case MediaPlayer.MEDIA_ERROR_MALFORMED:
								SdkLog.w(SdkLog_TAG, "Malformed"); break;
							case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
								SdkLog.w(SdkLog_TAG, "Not valid for progressive"); break;
							case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
								SdkLog.w(SdkLog_TAG, "Server died"); break;
							case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
								SdkLog.w(SdkLog_TAG, "Timed Out"); break;
							case MediaPlayer.MEDIA_ERROR_UNKNOWN:
								SdkLog.w(SdkLog_TAG, "Unknown"); break;
							case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
								SdkLog.w(SdkLog_TAG, "Unsupported"); break;								
							}
							return false;
						}
					});
		            video.start();
		        }
		    }
		    else {
		    	SdkLog.w(SdkLog_TAG, "onShowCustomView:: " + view);
		    }
		}
				
	};

	/**
	 * The b got layout params.
	 */
	private boolean bGotLayoutParams;

	/**
	 * Instantiates a new ormma view.
	 * 
	 * @param context
	 *            the context
	 */
	public OrmmaView(Context context) {
		super(context);
		initialize();
	}

	/**
	 * Instantiates a new ormma view.
	 * 
	 * @param context
	 *            the context
	 * @param set
	 *            the set
	 */
	public OrmmaView(Context context, AttributeSet set) {
		super(context, set);
		initialize();

		TypedArray a = getContext().obtainStyledAttributes(set, attrs);

		int w = a.getDimensionPixelSize(0, -1);
		int h = a.getDimensionPixelSize(1, -1);
		if (w > 0 && h > 0)
			mUtilityController.setMaxSize(w, h);

		a.recycle();

	}

	/**
	 * Instantiates a new ormma view.
	 * 
	 * @param context
	 *            the context
	 * @param listener
	 *            the listener
	 */
	public OrmmaView(Context context, OrmmaViewListener listener) {
		super(context);
		setListener(listener);
		initialize();
	}

	public void addJavascriptObject(Object obj, String name) {
		addJavascriptInterface(obj, name);
	}

	/**
	 * Change ad display to new dimensions
	 * 
	 * @param d
	 *            - display dimensions
	 * 
	 */
	private FrameLayout changeContentArea(Dimensions d) {

		FrameLayout contentView = (FrameLayout) getRootView().findViewById(
				android.R.id.content);

		ViewGroup parent = (ViewGroup) getParent();
		FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(
				(int) (d.width), (int) (d.height));
		fl.topMargin = (int) (d.x);
		fl.leftMargin = (int) (d.y);

		int index = 0;
		int count = parent.getChildCount();
		for (index = 0; index < count; index++) {
			if (parent.getChildAt(index) == OrmmaView.this)
				break;
		}
		mIndex = index;
		FrameLayout placeHolder = new FrameLayout(getContext());
		placeHolder.setId(PLACEHOLDER_ID);

		ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
				(int) mViewWidth, (int) mViewHeight);

		if (mScrollContainer == null && mFindScrollContainer) {
			ViewGroup _p = parent;
			while ((mScrollContainer = _p) != null) {
				if (mScrollContainer instanceof ScrollView) {
					mFindScrollContainer = false;
					mScrollPosY = ((ScrollView) mScrollContainer).getScrollY();
					break;
				}
				try {
					_p = (ViewGroup) (_p.getParent());
				} catch (ClassCastException ce) {
					_p = null;
				}
			}
		}
		mScrollContainer = mFindScrollContainer ? null : mScrollContainer;

		parent.addView(placeHolder, index, lp);

		FrameLayout backGround = new FrameLayout(getContext());

		backGround.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				SdkLog.i(SdkLog_TAG, "background touch called");
				return true;
			}
		});

		FrameLayout.LayoutParams bgfl = new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.WRAP_CONTENT);
		backGround.setId(BACKGROUND_ID);
		backGround.setPadding((int) (d.x), (int) (d.y), 0, 0);
		parent.removeView(OrmmaView.this);
		backGround.addView(OrmmaView.this, fl);
		contentView.addView(backGround, bgfl);

		return backGround;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebView#clearView()
	 */
	@Override
	public void clearView() {
		bPageFinished = false;
		reset();
		loadUrl("about:blank");
	}

	/**
	 * Close the view
	 */
	public void close() {
		mHandler.sendEmptyMessage(MESSAGE_CLOSE);
	}

	/**
	 * Close an expanded view.
	 */
	protected synchronized void closeExpanded() {

		setVisibility(GONE);
		String injection = "window.ormmaview.fireChangeEvent({ state: \'default\',"
				+ " size: "
				+ "{ width: "
				+ (int) mViewWidth
				/ mDensity
				+ ", "
				+ "height: " + (int) mViewHeight / mDensity + "}" + "});";
		SdkLog.d(SdkLog_TAG, "closeExpanded: injection: " + injection);
		injectJavaScript(injection);

		resetContents();

		mViewState = ViewState.DEFAULT;
		mHandler.sendEmptyMessage(MESSAGE_SEND_EXPAND_CLOSE);

		if (mScrollContainer != null) {
			((ScrollView) mScrollContainer).scrollTo(
					((ScrollView) mScrollContainer).getScrollX(), mScrollPosY);
		}
	}

	/**
	 * Close an opened view.
	 * 
	 * @param openedFrame
	 *            the opened frame
	 */
	protected void closeOpened(View openedFrame) {
		((ViewGroup) ((Activity) getContext()).getWindow().getDecorView())
				.removeView(openedFrame);
		requestLayout();
	}

	/**
	 * Close resized.
	 */
	private void closeResized() {
		if (mListener != null)
			mListener.onResizeClose();
		String injection = "window.ormmaview.fireChangeEvent({ state: \'default\',"
				+ " size: "
				+ "{ width: "
				+ (int) mViewWidth
				/ mDensity
				+ ", "
				+ "height: " + (int) mViewHeight / mDensity + "}" + "});";
		SdkLog.d(SdkLog_TAG, "closeResized: injection: " + injection);
		injectJavaScript(injection);
		resetLayout();
	}

	/**
	 * Deregister a protocol
	 * 
	 * @param protocol
	 *            the protocol to be de registered
	 */

	public void deregisterProtocol(String protocol) {
		if (protocol != null)
			registeredProtocols.remove(protocol.toLowerCase(Locale.GERMAN));
	}

	/**
	 * Do the real work of an expand
	 */
	private void doExpand(Bundle data) {

		Dimensions d = (Dimensions) data.getParcelable(DIMENSIONS);
		String url = data.getString(EXPAND_URL);
		Properties p = data.getParcelable(EXPAND_PROPERTIES);
		if (URLUtil.isValidUrl(url)) {
			loadUrl(url);
		}

		FrameLayout backGround = changeContentArea(d);

		if (p.useBackground) {
			int color = p.backgroundColor
					| ((int) (p.backgroundOpacity * 0xFF) * 0x10000000);
			backGround.setBackgroundColor(color);
		}

		String injection = "window.ormmaview.fireChangeEvent({ state: \'expanded\',"
				+ " size: "
				+ "{ width: "
				+ (int) (d.width / mDensity)
				+ ", "
				+ "height: " + (int) (d.height / mDensity) + "}" + " });";
		SdkLog.d(SdkLog_TAG, "doExpand: injection: " + injection);
		injectJavaScript(injection);
		if (mListener != null)
			mListener.onExpand();
		mViewState = ViewState.EXPANDED;

	}

	/**
	 * Dump.
	 */
	public void dump() {
		// TODO Auto-generated method stub
	}

	/**
	 * creates an expand message and throws it to the handler for the real work
	 * 
	 * @param dimensions
	 *            the dimensions
	 * @param URL
	 *            the uRL
	 * @param properties
	 *            the properties
	 */
	public void expand(Dimensions dimensions, String URL, Properties properties) {
		Message msg = mHandler.obtainMessage(MESSAGE_EXPAND);

		Bundle data = new Bundle();
		data.putParcelable(DIMENSIONS, dimensions);
		data.putString(EXPAND_URL, URL);
		data.putParcelable(EXPAND_PROPERTIES, properties);
		msg.setData(data);

		mHandler.sendMessage(msg);
	}

	/**
	 * Gets the content view height.
	 * 
	 * @return the content view height
	 */
	private int getContentViewHeight() {
		View contentView = getRootView().findViewById(android.R.id.content);
		if (contentView != null) {
			return contentView.getHeight();
		} else
			return -1;
	}

	OrmmaPlayer getPlayer() {

		if (player != null)
			player.releasePlayer();
		player = new OrmmaPlayer(getContext());
		return player;
	}

	/**
	 * Gets the size.
	 * 
	 * @return the size
	 */
	public String getSize() {
		return "{ width: " + (int) (getWidth() / mDensity) + ", " + "height: "
				+ (int) (getHeight() / mDensity) + "}";
	}

	/**
	 * Gets the state.
	 * 
	 * @return the state
	 */
	public String getState() {
		return mViewState.toString().toLowerCase(Locale.GERMAN);
	}

	protected TimeOutRunnable getTimeOutRunnable() {
		return mTimeOutRunnable;
	}

	/**
	 * Hide the view
	 */
	public void hide() {
		mHandler.sendEmptyMessage(MESSAGE_HIDE);
	}

	/**
	 * Initialize the view
	 */
	@SuppressWarnings("deprecation")
	@SuppressLint({ "SetJavaScriptEnabled" })
	private void initialize() {
		SdkUtil.setContext(getContext());
		if (!isInEditMode()) {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
				getSettings().setPluginState(WebSettings.PluginState.ON);
				getSettings().setAppCacheMaxSize(WEBVIEW_CACHE_SIZE);
			}
			getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
			getSettings().setAppCacheEnabled(true);
			getSettings().setUseWideViewPort(false);
			getSettings().setLoadWithOverviewMode(false);
			getSettings().setJavaScriptEnabled(true);

			mGestureDetector = new GestureDetector(getContext(),
					new ScrollEater());
			mUtilityController = new OrmmaUtilityController(this,
					this.getContext());

		}
		setScrollContainer(false);
		setVerticalScrollBarEnabled(false);
		setHorizontalScrollBarEnabled(false);

		setBackgroundColor(0);
		mDensity = SdkUtil.getDensity();
		bPageFinished = false;

		addJavascriptInterface(mUtilityController,
				"ORMMAUtilityControllerBridge");

		if (!isInEditMode()) {
			setScriptPath();
		}
		setWebViewClient(mWebViewClient);
		setWebChromeClient(mWebChromeClient);

		mContentViewHeight = getContentViewHeight();

		getViewTreeObserver().addOnGlobalLayoutListener(this);

		setVisibility(View.GONE);

	}

	/**
	 * Inject java script into the view
	 * 
	 * @param str
	 *            the java script to inject
	 */
	public void injectJavaScript(String str) {
		if (str != null) {
			super.loadUrl("javascript:" + str);
		}
	}

	/**
	 * Checks if is expanded.
	 * 
	 * @return true, if is expanded
	 */
	public boolean isExpanded() {
		return mViewState == ViewState.EXPANDED;
	}

	/**
	 * Checks if is page finished.
	 * 
	 * @return true, if is page finished
	 */
	public boolean isPageFinished() {
		return bPageFinished;
	}

	/**
	 * Is Protocol Registered
	 * 
	 * @param uri
	 *            The uri
	 * @return true , if the url's protocol is registered by the user, else
	 *         false if scheme is null or not registered
	 */
	private boolean isRegisteredProtocol(Uri uri) {

		String scheme = uri.getScheme();
		if (scheme == null)
			return false;

		for (String protocol : registeredProtocols) {
			if (protocol.equalsIgnoreCase(scheme))
				return true;
		}
		return false;
	}

	/**
	 * Load view from raw data
	 * 
	 * @param data
	 *            raw data as string
	 * @param type
	 *            string indicating mime-type
	 * @param enc
	 *            string indicating encoding type
	 */
	@Override
	public void loadData(String data, String type, String enc) {

		String url;
		
		bPageFinished = false;
		if (mTimeOutRunnable == null) {
			mTimeOutRunnable = new TimeOutRunnable();
		}
		try {
			mLocalFilePath = mUtilityController.writeToDiskWrap(data,
					CURRENT_FILE, true, null, mBridgeScriptPath, mScriptPath);
			url = "file://" + mLocalFilePath + java.io.File.separator
					+ CURRENT_FILE;
			mTimeOutHandler = new Handler();
			mTimeOutHandler.postDelayed(mTimeOutRunnable, 10000);
			super.loadUrl(url);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * Load view from html in a local file
	 * 
	 * @param f
	 *            the file
	 * @param dataToInject
	 *            any additional javascript to inject
	 */
	public void loadFile(File f, String dataToInject) {
		try {
			// mDataToInject = dataToInject;
			loadInputStream(new FileInputStream(f), dataToInject);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads the view from an input stream. Does the real loading work
	 * 
	 * @param is
	 *            the input stream
	 * @param dataToInject
	 *            the data to inject
	 */
	private void loadInputStream(InputStream is, String dataToInject) {
		String url;
		reset();
		if (mTimeOutRunnable != null) {
			mTimeOutRunnable.cancel();
		}
		mTimeOutRunnable = new TimeOutRunnable();

		try {
			mLocalFilePath = mUtilityController.writeToDiskWrap(is,
					CURRENT_FILE, true, dataToInject, mBridgeScriptPath,
					mScriptPath);
			url = "file://" + mLocalFilePath + java.io.File.separator
					+ CURRENT_FILE;
			mTimeOutHandler = new Handler();
			mTimeOutHandler.postDelayed(mTimeOutRunnable, 5000);
			if (dataToInject != null) {
				injectJavaScript(dataToInject);
			}

			super.loadUrl(url);
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				try {
					
					is.close();
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
			is = null;
		}
	}

	/*
	 * @see android.webkit.WebView#loadUrl(java.lang.String)
	 */
	@Override
	public void loadUrl(String url) {
		loadUrl(url, false, null);
	}

	/**
	 * Load url.
	 * 
	 * @param url
	 *            the url
	 * @param dontLoad
	 *            the dont load
	 * @param dataToInject
	 *            any additional javascript to inject
	 */
	public void loadUrl(String url, boolean dontLoad, String dataToInject) {
		// mDataToInject = dataToInject;
		if (URLUtil.isValidUrl(url)) {
			if (!dontLoad) {
				InputStream is = null;
				bPageFinished = false;
				try {
					URL u = new URL(url);
					String name = u.getFile();
					// if it is in the asset directory use the assetmanager
					if (url.startsWith("file:///android_asset/")) {
						name = url.replace("file:///android_asset/", "");
						AssetManager am = getContext().getAssets();
						is = am.open(name);
					} else {
						is = u.openStream();
					}
					loadInputStream(is, dataToInject);
					return;

				} catch (MalformedURLException e) {
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
			super.loadUrl(url);
		}
	}

	/**
	 * Load a url into the view
	 * 
	 * @param url
	 *            the url
	 * @param dataToInject
	 *            any additional javascript to inject
	 */
	public void loadUrl(String url, String dataToInject) {
		loadUrl(url, false, dataToInject);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebView#onAttachedToWindow()
	 * 
	 * Gather some initial information about the view.
	 */
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (!bGotLayoutParams) {
			bGotLayoutParams = true;
			ViewGroup.LayoutParams lp = getLayoutParams();
			lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
			lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
			setLayoutParams(lp);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		if (mLocalFilePath != null && mLocalFilePath.length() > 1) {
			mUtilityController.deleteOldAds(mLocalFilePath);
		}
		mUtilityController.stopAllListeners();
	}

	// trap keyboard state and view height/width


	public void onGlobalLayout() {

		boolean state = bKeyboardOut;
		if (!bKeyboardOut && mContentViewHeight >= 0
				&& getContentViewHeight() >= 0
				&& (mContentViewHeight != getContentViewHeight())) {

			state = true;
			String injection = "window.ormmaview.fireChangeEvent({ keyboardState: true});";
			injectJavaScript(injection);

		}
		if (bKeyboardOut && mContentViewHeight >= 0
				&& getContentViewHeight() >= 0
				&& (mContentViewHeight == getContentViewHeight())) {

			state = false;
			String injection = "window.ormmaview.fireChangeEvent({ keyboardState: false});";
			injectJavaScript(injection);
		}
		if (mContentViewHeight < 0) {
			mContentViewHeight = getContentViewHeight();
		}
		if (bPageFinished && bGotLayoutParams && mViewHeight <= 0
				& getHeight() > 0) {
			SdkLog.d(SdkLog_TAG, "onGlobalLayout :: " + getWidth() + "x"
					+ getHeight());
			mViewHeight = getHeight();
			mViewWidth = getWidth();
			mUtilityController.init(mDensity);
		}

		bKeyboardOut = state;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebView#onTouchEvent(android.view.MotionEvent)
	 * 
	 * used for trapping scroll events
	 */
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		boolean ret = mGestureDetector.onTouchEvent(ev);
		if (ret)
			ev.setAction(MotionEvent.ACTION_CANCEL);
		return super.onTouchEvent(ev);
	}

	/**
	 * Open.
	 * 
	 * @param url
	 *            the url
	 * @param back
	 *            show the back button
	 * @param forward
	 *            show the forward button
	 * @param refresh
	 *            show the refresh button
	 */
	public void open(String url, boolean back, boolean forward, boolean refresh) {

		Intent i = new Intent(getContext(), Browser.class);
		SdkLog.d(SdkLog_TAG, "open:" + url);
		i.putExtra(Browser.URL_EXTRA, url);
		i.putExtra(Browser.SHOW_BACK_EXTRA, back);
		i.putExtra(Browser.SHOW_FORWARD_EXTRA, forward);
		i.putExtra(Browser.SHOW_REFRESH_EXTRA, refresh);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		getContext().startActivity(i);

	}

	/**
	 * Open Map
	 * 
	 * @param url
	 *            - map url
	 * @param fullscreen
	 *            - should map be shown in full screen
	 */
	public void openMap(String POI, boolean fullscreen) {

		SdkLog.d(SdkLog_TAG, "Opening Map Url " + POI);

		POI = POI.trim();
		POI = OrmmaUtils.convert(POI);

		if (fullscreen) {
			try {
				// start google maps
				Intent mapIntent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(POI));
				mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

				getContext().startActivity(mapIntent);

			} catch (ActivityNotFoundException e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		} else {
			// if not fullscreen, display map in current OrmmaView space
			// TODO
			if (mapAPIKey != null) {

				try {
					// TODO fix the following line gets:
					// TODO add com.google.maps
					// java.lang.RuntimeException: stub
					/*
					 * MapView mapView = new MapView(getContext(), mapAPIKey);
					 * mapView.setBuiltInZoomControls(true);
					 */
				} catch (Exception e) {
					// TODO
					e.printStackTrace();
				}
			} else {
				Toast.makeText(
						getContext(),
						"Error: no Google Maps API Key provided for embedded map",
						Toast.LENGTH_LONG).show();
			}
		}
	}

	/**
	 * Play audio
	 * 
	 * @param url
	 *            - audio URL
	 * @param autoPlay
	 *            - should audio play immediately
	 * @param controls
	 *            - should native controls be visible
	 * @param loop
	 *            - should audio start over again after finishing
	 * @param position
	 *            - should audio be included with ad content
	 * @param startStyle
	 *            - normal/fullscreen; full screen if audio should play in full
	 *            screen
	 * @param stopStyle
	 *            - normal/exit; exit if audio should exit after audio stops
	 */
	public void playAudio(String url, boolean autoPlay, boolean controls,
			boolean loop, boolean position, String startStyle, String stopStyle) {

		PlayerProperties properties = new PlayerProperties();

		properties.setProperties(false, autoPlay, controls, position, loop,
				startStyle, stopStyle);

		Bundle data = new Bundle();

		data.putString(ACTION_KEY, ACTION.PLAY_AUDIO.toString());
		data.putString(EXPAND_URL, url);
		data.putParcelable(PLAYER_PROPERTIES, properties);

		if (properties.isFullScreen()) {
			try {
				Intent intent = new Intent(getContext(),
						OrmmaActionHandler.class);
				intent.putExtras(data);
				getContext().startActivity(intent);
			} catch (ActivityNotFoundException e) {
				e.printStackTrace();
			}
		} else {
			Message msg = mHandler.obtainMessage(MESSAGE_PLAY_AUDIO);
			msg.setData(data);
			mHandler.sendMessage(msg);
		}
	}

	public void playAudioImpl(Bundle data) {

		PlayerProperties properties = (PlayerProperties) data
				.getParcelable(PLAYER_PROPERTIES);

		String url = data.getString(EXPAND_URL);

		OrmmaPlayer audioPlayer = getPlayer();
		audioPlayer.setPlayData(properties, url);
		audioPlayer.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
		((ViewGroup) getParent()).addView(audioPlayer);

		audioPlayer.playAudio();

	}

	/**
	 * Play video
	 * 
	 * @param url
	 *            - video URL
	 * @param audioMuted
	 *            - should audio be muted
	 * @param autoPlay
	 *            - should video play immediately
	 * @param controls
	 *            - should native player controls be visible
	 * @param loop
	 *            - should video start over again after finishing
	 * @param d
	 *            - inline area dimensions
	 * @param startStyle
	 *            - normal/fullscreen; full screen if video should play in full
	 *            screen
	 * @param stopStyle
	 *            - normal/exit; exit if video should exit after video stops
	 */
	public void playVideo(String url, boolean audioMuted, boolean autoPlay,
			boolean controls, boolean loop, Dimensions d, String startStyle,
			String stopStyle) {

		Message msg = mHandler.obtainMessage(MESSAGE_PLAY_VIDEO);

		PlayerProperties properties = new PlayerProperties();

		properties.setProperties(audioMuted, autoPlay, controls, false, loop,
				startStyle, stopStyle);

		Bundle data = new Bundle();
		data.putString(EXPAND_URL, url);
		data.putString(ACTION_KEY, ACTION.PLAY_VIDEO.toString());

		data.putParcelable(PLAYER_PROPERTIES, properties);

		if (d != null)
			data.putParcelable(DIMENSIONS, d);

		if (properties.isFullScreen()) {
			try {
				Intent intent = new Intent(getContext(),
						OrmmaActionHandler.class);
				intent.putExtras(data);
				getContext().startActivity(intent);
			} catch (ActivityNotFoundException e) {
				e.printStackTrace();
			}
		} else if (d != null) {
			msg.setData(data);
			mHandler.sendMessage(msg);
		}
	}

	public void playVideoImpl(Bundle data) {

		PlayerProperties properties = (PlayerProperties) data
				.getParcelable(PLAYER_PROPERTIES);
		Dimensions d = (Dimensions) data.getParcelable(DIMENSIONS);
		String url = data.getString(EXPAND_URL);

		OrmmaPlayer videoPlayer = getPlayer();
		videoPlayer.setPlayData(properties, url);

		FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(
				(int) (d.width), (int) (d.height));
		fl.topMargin = (int) (d.x);
		fl.leftMargin = (int) (d.y);
		videoPlayer.setLayoutParams(fl);

		FrameLayout backGround = new FrameLayout(getContext());
		backGround.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				SdkLog.i(SdkLog_TAG, "background touch called");
				return true;
			}
		});
		backGround.setId(BACKGROUND_ID);
		backGround.setPadding((int) (d.x), (int) (d.y), 0, 0);

		FrameLayout contentView = (FrameLayout) getRootView().findViewById(
				android.R.id.content);
		contentView.addView(backGround, new FrameLayout.LayoutParams(
				FrameLayout.LayoutParams.MATCH_PARENT,
				FrameLayout.LayoutParams.MATCH_PARENT));

		backGround.addView(videoPlayer);
		setVisibility(View.INVISIBLE);

		videoPlayer.setListener(new OrmmaPlayerListener() {
			@Override
			public void onComplete() {
				FrameLayout background = (FrameLayout) getRootView()
						.findViewById(BACKGROUND_ID);
				((ViewGroup) background.getParent()).removeView(background);
				setVisibility(View.VISIBLE);
			}

			@Override
			public void onError() {
				onComplete();
			}

			@Override
			public void onPrepared() {
			}
		});

		videoPlayer.playVideo();

	}

	public void raiseError(String strMsg, String action) {

		Message msg = mHandler.obtainMessage(MESSAGE_RAISE_ERROR);

		Bundle data = new Bundle();
		data.putString(ERROR_MESSAGE, strMsg);
		data.putString(ERROR_ACTION, action);
		msg.setData(data);
		mHandler.sendMessage(msg);
	}

	/**
	 * Register a protocol
	 * 
	 * @param protocol
	 *            the protocol to be registered
	 */

	public void registerProtocol(String protocol) {
		if (protocol != null)
			registeredProtocols.add(protocol.toLowerCase(Locale.GERMAN));
	}

	/**
	 * Removes the listener.
	 */
	public void removeListener() {
		mListener = null;
	}

	/**
	 * Reset the view.
	 */
	private void reset() {
		if (mViewState == ViewState.EXPANDED) {
			closeExpanded();
		} else if (mViewState == ViewState.RESIZED) {
			closeResized();
		}
		invalidate();
		if (mLocalFilePath != null && mLocalFilePath.length() > 1) {
			mUtilityController.deleteOldAds(mLocalFilePath);
		}
		mUtilityController.stopAllListeners();
		resetLayout();
	}

	/**
	 * Revert to earlier ad state
	 */
	public void resetContents() {

		FrameLayout contentView = (FrameLayout) getRootView().findViewById(
				android.R.id.content);

		FrameLayout placeHolder = (FrameLayout) getRootView().findViewById(
				PLACEHOLDER_ID);
		FrameLayout background = (FrameLayout) getRootView().findViewById(
				BACKGROUND_ID);
		ViewGroup parent = (ViewGroup) placeHolder.getParent();

		background.removeView(this);
		contentView.removeView(background);
		parent.removeView(placeHolder);
		resetLayout();
		setVisibility(VISIBLE);
		parent.addView(this, mIndex);
		contentView.scrollTo(0, 0);
	}

	/**
	 * Reset layout.
	 */
	private void resetLayout() {
		SdkLog.d(SdkLog_TAG, "resetLayout :: ?? " + mViewWidth + "x"
				+ mViewHeight);
		if (!bPageFinished && bGotLayoutParams) {
			ViewGroup.LayoutParams lp = getLayoutParams();
			if (lp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
				SdkLog.d(SdkLog_TAG, "resetLayout :: LP " + lp.width + "x"
						+ lp.height);
				lp.width = ViewGroup.LayoutParams.MATCH_PARENT;
				lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
				setLayoutParams(lp);
				setVisibility(VISIBLE);
				requestLayout();
			}
		} else if (bGotLayoutParams && mViewHeight > 0) {
			ViewGroup.LayoutParams lp = getLayoutParams();
			SdkLog.d(SdkLog_TAG, "resetLayout :: LP " + lp.width + "x"
					+ lp.height);
			SdkLog.d(SdkLog_TAG, "resetLayout :: " + mViewWidth + "x"
					+ mViewHeight);
			lp.height = mViewHeight;
			lp.width = mViewWidth;
			setVisibility(VISIBLE);
			requestLayout();
		}
	}

	/**
	 * Resize the view
	 * 
	 * @param width
	 *            the width
	 * @param height
	 *            the height
	 */
	public void resize(int width, int height) {
		Message msg = mHandler.obtainMessage(MESSAGE_RESIZE);

		Bundle data = new Bundle();
		data.putInt(RESIZE_WIDTH, width);
		data.putInt(RESIZE_HEIGHT, height);
		msg.setData(data);

		mHandler.sendMessage(msg);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebView#restoreState(android.os.Bundle)
	 */
	@Override
	public WebBackForwardList restoreState(Bundle savedInstanceState) {

		mLocalFilePath = savedInstanceState.getString(AD_PATH);

		String url = "file://" + mLocalFilePath + java.io.File.separator
				+ CURRENT_FILE;
		super.loadUrl(url);

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.webkit.WebView#saveState(android.os.Bundle)
	 */
	@Override
	public WebBackForwardList saveState(Bundle outState) {
		outState.putString(AD_PATH, mLocalFilePath);
		return null;
	}

	/**
	 * Sets the listener.
	 * 
	 * @param listener
	 *            the new listener
	 */
	public void setListener(OrmmaViewListener listener) {
		mListener = listener;
	}

	public void setMapAPIKey(String key) {
		this.mapAPIKey = key;
	}

	/**
	 * Sets the max size.
	 * 
	 * @param w
	 *            the width
	 * @param h
	 *            the height
	 */
	public void setMaxSize(int w, int h) {
		mUtilityController.setMaxSize(w, h);
	};

	/**
	 * Sets the script path.
	 */
	private synchronized void setScriptPath() {
		if (mScriptPath == null) {
			mScriptPath = mUtilityController.copyTextFromJarIntoAssetDir(
					"/js/ormma.js", "js/ormma.js");
		}
		if (mBridgeScriptPath == null) {
			mBridgeScriptPath = mUtilityController.copyTextFromJarIntoAssetDir(
					"/js/ormma_bridge.js", "js/ormma_bridge.js");
		}
	}

	protected void setTimeoutRunnable(TimeOutRunnable r) {
		if (mTimeOutRunnable != null) {
			mTimeOutRunnable.cancel();
		}
		mTimeOutRunnable = r;
	}

	/**
	 * Show the view
	 */
	public void show() {
		mHandler.sendEmptyMessage(MESSAGE_SHOW);
	}
	
	protected WebViewClient getWebViewClient() {
		return mWebViewClient;
	}
	
	@Override
	public void dispatchWindowVisibilityChanged(int v) {
		super.dispatchWindowVisibilityChanged(v);
		if (v == View.GONE) {
			if (mLocalFilePath != null && mLocalFilePath.length() > 1) {
				mUtilityController.deleteOldAds(mLocalFilePath);
			}
			mUtilityController.stopAllListeners();
		}
	}
}
