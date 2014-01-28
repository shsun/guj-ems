package de.guj.ems.mobile.sdk.controllers;

import android.Manifest.permission;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Vibrator;
import android.webkit.JavascriptInterface;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;

/**
 * This class is added to the GuJEMSAdView as a Javascript interface by default.
 * 
 * It adds G+J EMS specific Javascript functionality to the AdView.
 * 
 * @author stein16
 * 
 */
public class EMSInterface {

	private static EMSInterface instance = null;

	private final static String TAG = "EMSInterface";

	public static EMSInterface getInstance() {
		if (EMSInterface.instance == null) {
			EMSInterface.instance = new EMSInterface();
		}
		return EMSInterface.instance;
	}

	/**
	 * Makes the phone vibrate once for l milliseconds. If the vibrate
	 * permission is not granted in the AndroidManifest.xml, an exception is
	 * thrown and caught.
	 * 
	 * @param l
	 *            length of vibration
	 */
	@JavascriptInterface
	public void vibrateOnce(long l) {
		SdkLog.i(TAG, "ems_vibrate: " + l + " ms");
		try {
			Context c = SdkUtil.getContext();
			if (c.getPackageManager().checkPermission(permission.VIBRATE,
					c.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
				throw new SecurityException(
						"Vibrate Permission not granted in Manifest");
			}
			Vibrator v = (Vibrator) c
					.getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(l);
		} catch (Exception e) {
			SdkLog.e(TAG, "Vibration not possible in this app.", e);
		}
	}

	/**
	 * Makes the phone vibrate as in the pattern given in l. If the vibrate
	 * permission is not granted in the AndroidManifest.xml, an exception is
	 * thrown and caught. The pattern starts with a length of x milliseconds for
	 * an initial pause. Each length is followed by another value indicating the
	 * pause until the next vibration. To vibrate twice for 100ms initially with
	 * a pause of 200ms between the vibration tones, the pattern would thus be
	 * [0,100,200,100].
	 * 
	 * @param l
	 *            pattern of vibration
	 */
	@JavascriptInterface
	public void vibratePattern(long[] l) {
		SdkLog.i(TAG, "ems_vibrate: pattern called.");
		try {
			Context c = SdkUtil.getContext();
			if (c.getPackageManager().checkPermission(permission.VIBRATE,
					c.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
				throw new SecurityException(
						"Vibrate Permission not granted in Manifest");
			}
			Vibrator v = (Vibrator) c
					.getSystemService(Context.VIBRATOR_SERVICE);
			v.vibrate(l, -1);
		} catch (Exception e) {
			SdkLog.e(TAG, "Vibration not possible in this app.", e);
		}
	}

	/**
	 * Returns the Android battery level received from BatteryManager
	 * 
	 * @return battery level or -1 if nothing was received yet
	 */
	@JavascriptInterface
	public int getBatteryPercent() {
		SdkLog.i(TAG, "ems_battery: status requested.");
		return SdkUtil.getBatteryLevel();
	}

	/**
	 * Returns boolean indicating whether headset is connected to phone
	 * 
	 * @return true if headset is connected, false if not
	 */
	@JavascriptInterface
	public boolean headsetConnected() {
		SdkLog.i(TAG, "ems_headset: status requested.");
		return SdkUtil.isHeadsetConnected();
	}

}
