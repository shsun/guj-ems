package de.guj.ems.mobile.sdk.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ReceiverCallNotAllowedException;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;
import android.webkit.WebView;
import de.guj.ems.mobile.sdk.controllers.AdServerAccess;
import de.guj.ems.mobile.sdk.views.AdResponseHandler;

/**
 * Various global static methods for initialization, configuration of sdk plus
 * targeting parameters.
 * 
 * @author stein16
 * 
 */
public class SdkUtil {

	private final static String TAG = "SdkUtil";

	private static Intent BATTERY_INTENT = null;

	private static TelephonyManager TELEPHONY_MANAGER;
	
	private static DisplayMetrics METRICS = new DisplayMetrics();

	private static WindowManager WINDOW_MANAGER = null;

	private static String COOKIE_REPL;

	private static String DEVICE_ID;

	private static final String EMSUID = ".emsuid";

	private static Context CONTEXT;

	private static String USER_AGENT = null;

	private final static boolean DEBUG = false;

	private final static String DEBUG_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 2.3; de-de; GT-I9100 Build/GRH78) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";

	/**
	 * major sdk version integer
	 */
	public final static int MAJOR_VERSION = 1;

	/**
	 * minor sdk version integer
	 */
	public final static int MINOR_VERSION = 3;

	/**
	 * revision sdk version integer
	 */
	public final static int REV_VERSION = 0;

	/**
	 * Version string containing major, minor and revision as string divided by
	 * underscores for passing it to the adserver
	 */
	public final static String VERSION_STR = MAJOR_VERSION + "_"
			+ MINOR_VERSION + "_" + REV_VERSION;

	/**
	 * Get android application context
	 * 
	 * @return context (if set before)
	 */
	public final static Context getContext() {
		return CONTEXT;
	}

	/**
	 * Returns an app specific unique id used as a cookie replacement
	 * 
	 * @return
	 */
	public synchronized static String getCookieReplStr() {
		if (COOKIE_REPL == null) {
			File fuuid = new File(SdkUtil.getContext().getFilesDir(), EMSUID);
			try {
				if (!fuuid.exists())
					writeUUID(fuuid);
				COOKIE_REPL = readUUID(fuuid);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return COOKIE_REPL;
	}

	/**
	 * Get screen density in dots per inch
	 * 
	 * @return screen density in dots per inch
	 */
	public static float getDensity() {
		return SdkUtil.getMetrics().density;
	}

	/**
	 * Get screen density (hdpi, mdpi, ldpi)
	 * 
	 * @return android screen density
	 */
	public static int getDensityDpi() {
		return SdkUtil.getMetrics().densityDpi;
	}

	/**
	 * Android device identifier
	 * 
	 * @return device identifier as string
	 */
	public static String getDeviceId() {
		if (DEVICE_ID == null) {
			if (TELEPHONY_MANAGER == null) {
				TELEPHONY_MANAGER = (TelephonyManager) SdkUtil.getContext()
						.getSystemService(Context.TELEPHONY_SERVICE);
			}
			try {
				DEVICE_ID = TELEPHONY_MANAGER.getDeviceId();
			} catch (SecurityException se) {
				SdkLog.w(TAG,
						"TelephonyManager not available, using replacement UID.");
				DEVICE_ID = getCookieReplStr();
			}
		}

		return DEVICE_ID;
	}

	private static DisplayMetrics getMetrics() {
		if (SdkUtil.WINDOW_MANAGER == null) {
			SdkUtil.WINDOW_MANAGER = getWinMgr();
		}
		SdkUtil.WINDOW_MANAGER.getDefaultDisplay().getMetrics(
				SdkUtil.METRICS);
		return METRICS;

	}

	/**
	 * Get screen width in pixels
	 * 
	 * @return screen width in pixels
	 */
	public static int getScreenHeight() {
		return SdkUtil.getMetrics().heightPixels;
	}

	/**
	 * Get screen height in pixels
	 * 
	 * @return screen height in pixels
	 */
	public static int getScreenWidth() {
		return SdkUtil.getMetrics().widthPixels;
	}

	/**
	 * Returns a fixed user-agent if in debug mode, the device user agent if not
	 * 
	 * @return device or fixed user agent as string
	 */
	public static String getUserAgent() {
		if (DEBUG) {
			SdkLog.w(TAG,
					"UserAgentHelper is in DEBUG mode. Do not deploy to production like this.");
		}
		if (USER_AGENT == null) {
			// determine user-agent
			WebView w = new WebView(SdkUtil.getContext());
			USER_AGENT = w.getSettings().getUserAgentString();
			w.destroy();
			w = null;
		}
		return DEBUG ? DEBUG_USER_AGENT : USER_AGENT;
	}

	private static WindowManager getWinMgr() {
		if (SdkUtil.WINDOW_MANAGER == null) {
			SdkUtil.WINDOW_MANAGER = (WindowManager) SdkUtil.getContext()
					.getSystemService(Context.WINDOW_SERVICE);
		}
		return WINDOW_MANAGER;
	}

	/**
	 * Check whether phone has mobile 3G connection
	 * 
	 * @return
	 */
	@SuppressLint("InlinedApi")
	public static boolean is3G() {
		if (!isWifi()) {
			if (TELEPHONY_MANAGER == null) {
				TELEPHONY_MANAGER = (TelephonyManager) SdkUtil.getContext()
						.getSystemService(Context.TELEPHONY_SERVICE);
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				if (TELEPHONY_MANAGER.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE) {
					return false;
				}
			}
			switch (TELEPHONY_MANAGER.getNetworkType()) {
			case TelephonyManager.NETWORK_TYPE_EDGE:
			case TelephonyManager.NETWORK_TYPE_GPRS:
			case TelephonyManager.NETWORK_TYPE_UNKNOWN:
				return false;
			}
			return true;
		}
		return false;
	}

	
	/**
	 * Check whether phone has mobile 4G connection
	 * 
	 * @return
	 */
	@SuppressLint("InlinedApi")
	public static boolean is4G() {
		if (!isWifi() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (TELEPHONY_MANAGER == null) {
				TELEPHONY_MANAGER = (TelephonyManager) SdkUtil.getContext()
						.getSystemService(Context.TELEPHONY_SERVICE);
			}
			return TELEPHONY_MANAGER.getNetworkType() == TelephonyManager.NETWORK_TYPE_LTE;
		}
		return false;
	}

	/**
	 * Check wheter GPS is active / allowed
	 * 
	 * @return
	 */
	public static boolean isGPSActive() {
		final LocationManager manager = (LocationManager) CONTEXT
				.getSystemService(Context.LOCATION_SERVICE);
		try {
			return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		} catch (Exception e) {
			SdkLog.w(TAG,
					"Access fine location not allowed by app - assuming no GPS");
			return false;
		}
	}

	/**
	 * Check whether device is in portait mode
	 * 
	 * @return true if portrait mode, false if landscape mode
	 */
	public static boolean isPortrait() {
		int r = getWinMgr().getDefaultDisplay().getRotation();
		return r == Surface.ROTATION_0 || r == Surface.ROTATION_180;
	}

	/**
	 * Check whether device is offline. if
	 * android.Manifest.permission.ACCESS_NETWORK_STATE is not granted or the
	 * state cannot be determined, the device will alsways be assumed to be
	 * online.
	 * 
	 * @return true if device is not connected to any network
	 */
	public static boolean isOffline() {

		Context c = SdkUtil.getContext();
		if (c.getPackageManager().checkPermission(
				permission.ACCESS_NETWORK_STATE, c.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
			SdkLog.w(TAG,
					"Access Network State not granted in Manifest - assuming ONLINE.");
			return false;
		}

		final ConnectivityManager conMgr = (ConnectivityManager) c
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		try {
			return conMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
					.getState() == NetworkInfo.State.DISCONNECTED
					&& conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
							.getState() == NetworkInfo.State.DISCONNECTED;
		} catch (Exception e) {
			SdkLog.w(TAG, "Exception in getNetworkInfo - assuming ONLINE.");
			return false;
		}
	}

	/**
	 * Check whether device is online. if
	 * android.Manifest.permission.ACCESS_NETWORK_STATE is not granted or the
	 * state cannot be determined, the device will alsways be assumed to be
	 * online.
	 * 
	 * @return true if device is connected to any network
	 */
	public static boolean isOnline() {

		Context c = SdkUtil.getContext();
		if (c.getPackageManager().checkPermission(
				permission.ACCESS_NETWORK_STATE, c.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
			SdkLog.w(TAG,
					"Access Network State not granted in Manifest - assuming ONLINE.");
			return true;
		}

		final ConnectivityManager conMgr = (ConnectivityManager) c
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		try {
			return conMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
					.getState() == NetworkInfo.State.CONNECTED
					|| conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
							.getState() == NetworkInfo.State.CONNECTED
					|| conMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)
							.getState() == NetworkInfo.State.CONNECTING
					|| conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
							.getState() == NetworkInfo.State.CONNECTING;
		} catch (Exception e) {
			SdkLog.w(TAG, "Exception in getNetworkInfo - assuming ONLINE.");
			return true;
		}
	}

	/**
	 * Check whether device is connected via WiFi. if
	 * android.Manifest.permission.ACCESS_NETWORK_STATE is not granted or the
	 * state cannot be determined, the device will always be assumed to be
	 * online via a mobile concection.
	 * 
	 * @return true if device is connected via wifi
	 */
	public static boolean isWifi() {

		Context c = SdkUtil.getContext();
		if (c.getPackageManager().checkPermission(
				permission.ACCESS_NETWORK_STATE, c.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
			SdkLog.w(TAG,
					"Access Network State not granted in Manifest - assuming mobile connection.");
			return false;
		}

		final ConnectivityManager conMgr = (ConnectivityManager) c
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		try {
			return conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
					.getState() == NetworkInfo.State.CONNECTED
					|| conMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
							.getState() == NetworkInfo.State.CONNECTING;
		} catch (Exception e) {
			SdkLog.w(TAG,
					"Exception in getNetworkInfo - assuming mobile connection.");
			return false;
		}
	}

	private static String readUUID(File fuuid) throws IOException {
		RandomAccessFile f = new RandomAccessFile(fuuid, "r");
		byte[] bytes = new byte[(int) f.length()];
		f.readFully(bytes);
		f.close();
		return new String(bytes);
	}

	/**
	 * Set application context
	 * 
	 * @param c
	 *            android application context
	 */
	public final static void setContext(Context c) {
		CONTEXT = c;
	}

	private static void writeUUID(File fuuid) throws IOException {
		FileOutputStream out = new FileOutputStream(fuuid);
		String id = UUID.randomUUID().toString();
		out.write(id.getBytes());
		out.close();
	}

	public static boolean isChargerConnected() {
		if (BATTERY_INTENT == null) {
			try {
				IntentFilter ifilter = new IntentFilter(
						Intent.ACTION_BATTERY_CHANGED);
				BATTERY_INTENT = getContext().getApplicationContext().registerReceiver(null, ifilter);
			}
			catch (ReceiverCallNotAllowedException e) {
				SdkLog.w(TAG, "Skipping start of phone status receivers from start interstitial.");
				BATTERY_INTENT = null;
				return false;
			}
		}
		int cp = BATTERY_INTENT.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		return cp == BatteryManager.BATTERY_PLUGGED_AC
				// || cp == BatteryManager.BATTERY_PLUGGED_WIRELESS
				|| cp == BatteryManager.BATTERY_PLUGGED_USB;
	}

	public static int getBatteryLevel() {
		if (BATTERY_INTENT == null) {
			try {
				IntentFilter ifilter = new IntentFilter(
						Intent.ACTION_BATTERY_CHANGED);
				BATTERY_INTENT = getContext().getApplicationContext().registerReceiver(null, ifilter);
			}
			catch (ReceiverCallNotAllowedException e) {
				SdkLog.w(TAG, "Skipping start of phone status receivers from start interstitial.");
				BATTERY_INTENT = null;
				return 100;
			}			
		}
		int level = BATTERY_INTENT.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = BATTERY_INTENT.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		return (int) (100.0f * (level / (float) scale));
	}

	@SuppressWarnings("deprecation")
	public static boolean isHeadsetConnected() {
		return ((AudioManager) getContext().getSystemService(
				Context.AUDIO_SERVICE)).isWiredHeadsetOn();
	}

	public static void httpRequest(final String url) {
		SdkUtil.httpRequests(new String[] { url });
	}

	public static void httpRequests(final String[] url) {
		(new AdServerAccess(getUserAgent(), new AdResponseHandler() {

			@Override
			public void processResponse(String response) {
			}

			@Override
			public void processError(String msg, Throwable t) {
				SdkLog.e(TAG, msg, t);

			}

			@Override
			public void processError(String msg) {
				SdkLog.e(TAG, msg);
			}
		})).execute(url);
	}

}
