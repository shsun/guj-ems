package de.guj.ems.mobile.sdk.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
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
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import de.guj.ems.mobile.sdk.controllers.IAdResponseHandler;
import de.guj.ems.mobile.sdk.controllers.TrackingRequest;
import de.guj.ems.mobile.sdk.controllers.adserver.AdRequest;
import de.guj.ems.mobile.sdk.controllers.adserver.AmobeeAdRequest;

/**
 * Various global static methods for initialization, configuration of sdk plus
 * targeting parameters.
 * 
 * @author stein16
 * 
 */
public class SdkUtil {

	private final static String TAG = "SdkUtil";

	private final static Class<?>[] KITKAT_JS_PARAMTYPES = new Class[] {
			String.class, ValueCallback.class };

	private static Method KITKAT_JS_METHOD = null;

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

	private final static String DEBUG_USER_AGENT = "Mozilla/5.0 (Linux; U; Android 4.3; de-de; GT-I9100 Build/GRH78) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";

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
	public final static int REV_VERSION = 1;

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
				try {
					TELEPHONY_MANAGER = (TelephonyManager) SdkUtil.getContext()
						.getSystemService(Context.TELEPHONY_SERVICE);
				}
				catch (Exception e) {
					DEVICE_ID = Secure.ANDROID_ID;
					return DEVICE_ID;
				}
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
		SdkUtil.WINDOW_MANAGER.getDefaultDisplay().getMetrics(SdkUtil.METRICS);
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
	@SuppressLint("NewApi")
	public static String getUserAgent() {
		if (DEBUG) {
			SdkLog.w(TAG,
					"UserAgentHelper is in DEBUG mode. Do not deploy to production like this.");
		}
		if (USER_AGENT == null) {
			// determine user-agent
			if (Build.VERSION.SDK_INT < 17) {
				try {
					WebView w = new WebView(CONTEXT);
					USER_AGENT = w.getSettings().getUserAgentString();
					w.destroy();
					w = null;
				}
				catch (Exception e) {
					USER_AGENT = DEBUG_USER_AGENT;
				}
			} else {
				USER_AGENT = WebSettings.getDefaultUserAgent(CONTEXT);
			}
			SdkLog.i(TAG, "G+J EMS SDK UserAgent: " + USER_AGENT);
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

	/**
	 * Check whether a charger is connected to the device
	 * 
	 * @return true if a charger is connected
	 */
	public static boolean isChargerConnected() {
		if (BATTERY_INTENT == null) {
			try {
				IntentFilter ifilter = new IntentFilter(
						Intent.ACTION_BATTERY_CHANGED);
				BATTERY_INTENT = getContext().getApplicationContext()
						.registerReceiver(null, ifilter);
			} catch (ReceiverCallNotAllowedException e) {
				SdkLog.w(TAG,
						"Skipping start of phone status receivers from start interstitial.");
				BATTERY_INTENT = null;
				return false;
			}
		}
		int cp = BATTERY_INTENT.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		return cp == BatteryManager.BATTERY_PLUGGED_AC
		// || cp == BatteryManager.BATTERY_PLUGGED_WIRELESS
				|| cp == BatteryManager.BATTERY_PLUGGED_USB;
	}

	/**
	 * Get the battery charge level in percent
	 * 
	 * @return Integer value [0..100], indicating battery charge level in
	 *         percent
	 */
	public static int getBatteryLevel() {
		if (BATTERY_INTENT == null) {
			try {
				IntentFilter ifilter = new IntentFilter(
						Intent.ACTION_BATTERY_CHANGED);
				BATTERY_INTENT = getContext().getApplicationContext()
						.registerReceiver(null, ifilter);
			} catch (ReceiverCallNotAllowedException e) {
				SdkLog.w(TAG,
						"Skipping start of phone status receivers from start interstitial.");
				BATTERY_INTENT = null;
				return 100;
			}
		}
		int level = BATTERY_INTENT.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
		int scale = BATTERY_INTENT.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

		return (int) (100.0f * (level / (float) scale));
	}

	/**
	 * Check whether a headset is connected to the device
	 * 
	 * @return true if a headset is connected
	 */
	@SuppressWarnings("deprecation")
	public static boolean isHeadsetConnected() {
		return ((AudioManager) getContext().getSystemService(
				Context.AUDIO_SERVICE)).isWiredHeadsetOn();
	}

	/**
	 * Create an ad request object with url and response handler
	 * 
	 * @param url
	 *            Ad request url
	 * @param handler
	 *            response handler
	 * @return initialized ad request
	 */
	public static AdRequest adRequest(IAdResponseHandler handler) {
		return new AmobeeAdRequest(handler);
	}

	/**
	 * Create an ad request object url, response handler and security mechanism
	 * 
	 * @param url
	 *            Ad request url
	 * @param handler
	 *            response handler
	 * @param secHeaderName
	 *            name of security header
	 * @param secHeaderVal
	 *            security hash value
	 * @return initialized ad request
	 */
	public static AdRequest adRequest(IAdResponseHandler handler,
			String secHeaderName, int secHeaderVal) {
		return new AmobeeAdRequest(secHeaderName, secHeaderVal, handler);
	}

	/**
	 * Perform a quick simple http request without processing the response
	 * 
	 * @param url
	 *            The url to request
	 */
	public static void httpRequest(final String url) {
		SdkUtil.httpRequests(new String[] { url });
	}

	/**
	 * Perform quick simple http requests without processing the response.
	 * Errors are written to log output.
	 * 
	 * @param url
	 *            An array of url strings
	 */
	public static void httpRequests(final String[] url) {
		new TrackingRequest().execute(url);
	}

	/**
	 * Helper method to determine the correct way to execute javascript in a
	 * webview. Starting from Android 4.4, the Android webview is a chrome
	 * webview and the method to execute javascript has changed from loadUrl to
	 * evaluateJavascript
	 * 
	 * @param webView
	 *            The webview to exeute the script in
	 * @param javascript
	 *            the actual script
	 */
	public static void evaluateJavascript(WebView webView, String javascript) {
		if (KITKAT_JS_METHOD == null && Build.VERSION.SDK_INT >= 19) {
			try {
				KITKAT_JS_METHOD = Class.forName("android.webkit.WebView")
						.getDeclaredMethod("evaluateJavascript",
								KITKAT_JS_PARAMTYPES);
				KITKAT_JS_METHOD.setAccessible(true);
				SdkLog.i(TAG,
						"G+J EMS SDK AdView: Running in KITKAT mode with new Chromium webview!");
			} catch (Exception e0) {
				SdkLog.e(
						TAG,
						"FATAL ERROR: Could not invoke Android 4.4 Chromium WebView method evaluateJavascript",
						e0);
			}
		}

		if (Build.VERSION.SDK_INT < 19) {
			webView.loadUrl("javascript:" + javascript);
		} else
			try {
				KITKAT_JS_METHOD.invoke(webView, javascript, null);
			} catch (Exception e) {
				SdkLog.e(
						TAG,
						"FATAL ERROR: Could not invoke Android 4.4 Chromium WebView method evaluateJavascript",
						e);
			}
	}

}
