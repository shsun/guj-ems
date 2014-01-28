package de.guj.ems.mobile.sdk.util;

import android.util.Log;

/**
 * Globally available interface for logging from the sdk. SdkLog distinguishes
 * between three log levels: TEST - allows log levels up to verbose PROD -
 * allows log levels up to info OFF - switches all logging off Uses android
 * logging utility.
 * 
 * @author stein16
 * 
 */
public class SdkLog {

	private final static int LOG_LEVEL_TEST = 1;

	private final static int LOG_LEVEL_PROD = 0;

	private final static int LOG_LEVEL_OFF = 2;

	private static int LOG_LEVEL = LOG_LEVEL_TEST;

	/**
	 * Sets log level to production (max = info)
	 */
	public static void setProductionLogLevel() {
		SdkLog.LOG_LEVEL = SdkLog.LOG_LEVEL_TEST;
	}

	/**
	 * Sets log level to test (max = verbose)
	 */
	public static void setTestLogLevel() {
		SdkLog.LOG_LEVEL = SdkLog.LOG_LEVEL_TEST;
	}

	/**
	 * Turns all logging off
	 */
	public static void setLogLevelOff() {
		SdkLog.LOG_LEVEL = SdkLog.LOG_LEVEL_OFF;
	}

	/**
	 * @see android.util.Log.d(String, String)
	 * @param tag
	 *            Log tag
	 * @param message
	 *            Log message
	 */
	public static void d(String tag, String message) {
		if (SdkLog.LOG_LEVEL == SdkLog.LOG_LEVEL_TEST) {
			Log.d(tag, message);
		}
	}

	/**
	 * @see android.util.Log.i(String, String)
	 * @param tag
	 *            Log tag
	 * @param message
	 *            Log message
	 */
	public static void i(String tag, String message) {
		if (SdkLog.LOG_LEVEL != SdkLog.LOG_LEVEL_OFF) {
			Log.i(tag, message);
		}
	}

	/**
	 * @see android.util.Log.v(String, String)
	 * @param tag
	 *            Log tag
	 * @param message
	 *            Log message
	 */
	public static void v(String tag, String message) {
		if (SdkLog.LOG_LEVEL == SdkLog.LOG_LEVEL_TEST) {
			Log.v(tag, message);
		}
	}

	/**
	 * @see android.util.Log.e(String, String)
	 * @param tag
	 *            Log tag
	 * @param message
	 *            Log message
	 */
	public static void e(String tag, String message) {
		Log.e(tag, message);
	}

	/**
	 * @see android.util.Log.e(String, String, Throwable)
	 * @param tag
	 *            Log tag
	 * @param message
	 *            Log message
	 * @param t
	 *            Thrown exception
	 */
	public static void e(String tag, String message, Throwable t) {
		Log.e(tag, message, t);
	}

	/**
	 * @see android.util.Log.w(String, String)
	 * @param tag
	 *            Log tag
	 * @param message
	 *            Log message
	 */
	public static void w(String tag, String message) {
		if (SdkLog.LOG_LEVEL != SdkLog.LOG_LEVEL_OFF) {
			Log.w(tag, message);
		}
	}

	/**
	 * Check whether log level is for testing (debug)
	 * 
	 * @return true if log level is test
	 */
	public static boolean isTestLogLevel() {
		return LOG_LEVEL == LOG_LEVEL_TEST;
	}

	/**
	 * Check whether logging is off
	 * 
	 * @return true if logging is off
	 */
	public static boolean isLogLevelOff() {
		return LOG_LEVEL == LOG_LEVEL_OFF;
	}

	/**
	 * Check whether log level is for production
	 * 
	 * @return true if log level is production
	 */
	public static boolean isProdLogLevel() {
		return LOG_LEVEL == LOG_LEVEL_PROD;
	}

}
