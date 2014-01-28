package de.guj.ems.mobile.sdk.controllers.adserver;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import de.guj.ems.mobile.sdk.R;
import de.guj.ems.mobile.sdk.controllers.AdViewConfiguration;
import de.guj.ems.mobile.sdk.controllers.backfill.BackfillDelegator;
import de.guj.ems.mobile.sdk.util.SdkGlobals;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;

/**
 * This class implements all available ad server parameters for the Amobee
 * AdServer
 * 
 * Base URL http://vfdeprod.amobee.com Base Query String "?tp=4&prt=G+J" The
 * following parameters are addded at runtime: "as" for the zoneId, "uid" for a
 * unique device id and "t", containing the timestamp in milliseconds since Jan
 * 1st 1970, "lat", "lon" if "ems_geo" is set to true, "pbl" for battery level
 * and "psx" for various phone settings
 * 
 * Custom params and keywords (kw=/nkw=) can be added for programmatically added
 * views.
 * 
 * @author stein16
 * 
 */
public final class AmobeeSettingsAdapter extends AdServerSettingsAdapter {

	private static final long serialVersionUID = -4637791449705402591L;

	private final static int SECURITY_HEADER_VALUE = 1958013300;

	private final static char STATUS_3G_ON = '3';

	private final static char STATUS_4G_ON = '4';

	private final static char STATUS_GPS_ON = 'g';

	private final static char STATUS_PORTRAIT_MODE = 'p';

	private final static char STATUS_HEADSET_CONNECTED = 'h';

	private final static char STATUS_CHARGER_CONNECTED = 'c';

	private final static char STATUS_WIFI_ON = 'w';

	private final static char STATUS_LANDSCAPE_MODE = 'l';

	private final static String BASE_URL = SdkUtil.getContext().getString(
			R.string.baseUrl);

	private final static String BASE_PARAMS = "?"
			+ SdkUtil.getContext().getString(R.string.baseParams)
					.replaceAll("#version#", SdkUtil.VERSION_STR);

	private final static String TAG = "AmobeeSettingsAdapter";

	/**
	 * Constructor with all attributes stored in an AttributeSet
	 * 
	 * @param context
	 *            android application context
	 * @param set
	 *            attribute set with configuration
	 */
	public AmobeeSettingsAdapter(Context context, Class<?> viewClass, AttributeSet set) {
		super(context, set, viewClass);

		TypedArray tVals = context.obtainStyledAttributes(set,
				R.styleable.GuJEMSAdView);
		if (getAttrsToParams().get(SdkGlobals.EMS_UUID) != null) {
			if (tVals.getBoolean(AdViewConfiguration.getConfig(viewClass).getUuidId(), false)) {
				putAttrToParam(SdkGlobals.EMS_UUID, SdkUtil
						.getContext().getString(R.string.amobeeUserId));
				putAttrValue(SdkGlobals.EMS_UUID,
						SdkUtil.getDeviceId());
			} else {
				SdkLog.d(TAG, "Device id transmission not allowed by adspace.");
			}
		}
		if (getAttrsToParams().get(SdkGlobals.EMS_ZONEID) != null) {
			String as = tVals.getString(AdViewConfiguration.getConfig(viewClass).getZoneIdId());
			putAttrToParam(SdkGlobals.EMS_ZONEID, SdkUtil
					.getContext().getString(R.string.amobeeAdSpace));
			putAttrValue(SdkGlobals.EMS_ZONEID, as);
		}
		if (getAttrsToParams().get(SdkGlobals.EMS_GEO) != null) {
			if (tVals.getBoolean(AdViewConfiguration.getConfig(viewClass).getGeoId(), false)) {
				double[] loc = getLocation();
				if (loc != null && 0.0 != loc[0]) {
					putAttrToParam(SdkGlobals.EMS_LAT, SdkUtil
							.getContext().getString(R.string.amobeeLatitude));
					putAttrValue(SdkGlobals.EMS_LAT,
							String.valueOf(loc[0]));
					putAttrToParam(SdkGlobals.EMS_LON, SdkUtil
							.getContext().getString(R.string.amobeeLongitude));
					putAttrValue(SdkGlobals.EMS_LON,
							String.valueOf(loc[1]));
				} else {
					SdkLog.i(TAG, "Location too old or not fetchable.");
				}
			} else {
				SdkLog.d(TAG,
						"Location fetching not allowed by adspace.");
			}
		}
		if (getAttrsToParams().get(SdkGlobals.EMS_BACKFILL_SITEID) != null
				&& getAttrsToParams().get(
						SdkGlobals.EMS_BACKFILL_ZONEID) != null) {
			String site = tVals
					.getString(AdViewConfiguration.getConfig(viewClass).getBackfillSiteIdId());
			String zone = tVals
					.getString(AdViewConfiguration.getConfig(viewClass).getBackfillZoneIdId());
			this.setDirectBackfill(new BackfillDelegator.BackfillData(site,
					zone, "", -1));
			SdkLog.d(TAG, "Direct backfill configuration detected. [site="
					+ site + ", zone=" + zone + "]");
		}
		tVals.recycle();
	}

	private String strArrToString(String[] strs) {
		try {
			if (strs != null && strs.length > 0) {
				String res = new String();
				for (int i = 0; i < strs.length; i++) {
					if (i > 0) {
						res += "%7C" + URLEncoder.encode(strs[i], "utf-8");
					} else {
						res = strs[i];
					}
				}

				return res;
			}
		} catch (UnsupportedEncodingException e) {
			SdkLog.e(TAG, "Error encoding query string.", e);
		}
		return null;
	}

	/**
	 * Constructor with additional array of keywords and non-keywords which will
	 * be added to the ad server requests. Use this constructor to add
	 * parameters to the request during runtime
	 * 
	 * @param context
	 *            android application context
	 * @param set
	 *            attribute set with configuration
	 * @param kws
	 *            matching keywords
	 * @param nkws
	 *            non-matching keywords
	 */
	public AmobeeSettingsAdapter(Context context, Class<?> viewClass, AttributeSet set,
			String[] kws, String[] nkws) {
		this(context, viewClass, set);
		TypedArray tVals = context.obtainStyledAttributes(set,
				R.styleable.GuJEMSAdView);
		if (kws != null
				&& kws.length > 0
				&& getAttrsToParams().get(SdkGlobals.EMS_KEYWORDS) != null) {
			if (tVals
					.getBoolean(AdViewConfiguration.getConfig(viewClass).getKeywordsId(), false)) {
				String kwstr = strArrToString(kws);
				putAttrValue(SdkGlobals.EMS_KEYWORDS, kwstr);
			} else {
				SdkLog.d(TAG,
						"Skipped keywords because view is not configured with ems_kw=true.");
			}
		}
		if (nkws != null
				&& nkws.length > 0
				&& getAttrsToParams()
						.get(SdkGlobals.EMS_NKEYWORDS) != null) {
			if (Boolean.valueOf(
					getAttrsToParams().get(
							SdkGlobals.EMS_NKEYWORDS))
					.booleanValue()) {
				String nkwstr = strArrToString(nkws);
				putAttrValue(SdkGlobals.EMS_NKEYWORDS, nkwstr);
			} else {
				SdkLog.d(TAG,
						"Skipped non-keywords because view is not configured with ems_nkw=true.");
			}
		}
		tVals.recycle();
	}

	/**
	 * Constructor with additional array of keywords and non-keywords which will
	 * be added to the ad server requests. Use this constructor to add
	 * parameters to the request during runtime
	 * 
	 * @param context
	 *            android application context
	 * @param savedInstance
	 *            bundle with configuration
	 * @param kws
	 *            matching keywords
	 * @param nkws
	 *            non-matching keywords
	 */
	public AmobeeSettingsAdapter(Context context, Class<?> viewClass, Bundle savedInstance,
			String[] kws, String[] nkws) {
		this(context, viewClass, savedInstance);
		if (kws != null
				&& kws.length > 0
				&& getAttrsToParams().get(SdkGlobals.EMS_KEYWORDS) != null) {
			if (savedInstance.getBoolean(
					SdkGlobals.EMS_ATTRIBUTE_PREFIX
							+ SdkGlobals.EMS_KEYWORDS, false)) {
				String kwstr = strArrToString(kws);
				putAttrValue(SdkGlobals.EMS_KEYWORDS, kwstr);
			} else {
				SdkLog.d(TAG,
						"Skipped keywords because view is not configured with ems_kw=true.");
			}
		}
		if (nkws != null
				&& nkws.length > 0
				&& getAttrsToParams()
						.get(SdkGlobals.EMS_NKEYWORDS) != null) {
			if (savedInstance.getBoolean(
					SdkGlobals.EMS_ATTRIBUTE_PREFIX
							+ SdkGlobals.EMS_NKEYWORDS, false)) {
				String nkwstr = strArrToString(nkws);
				putAttrValue(SdkGlobals.EMS_NKEYWORDS, nkwstr);
			} else {
				SdkLog.d(TAG,
						"Skipped non-keywords because view is not configured with ems_nkw=true.");
			}
		}
	}

	/**
	 * Constructor with configuration in bundle
	 * 
	 * @param context
	 *            android application context
	 * @param savedInstance
	 *            bundle with configuration
	 */
	public AmobeeSettingsAdapter(Context context, Class<?> viewClass, Bundle savedInstance) {
		super(context, savedInstance, viewClass);
		if (getAttrsToParams().get(SdkGlobals.EMS_UUID) != null) {
			if (savedInstance.getBoolean(
					SdkGlobals.EMS_ATTRIBUTE_PREFIX
							+ SdkGlobals.EMS_UUID, false)) {
				putAttrToParam(SdkGlobals.EMS_UUID, SdkUtil
						.getContext().getString(R.string.amobeeUserId));
				putAttrValue(SdkGlobals.EMS_UUID,
						SdkUtil.getDeviceId());
			} else {
				SdkLog.d(TAG,
						"ems_uid: device id transmission not allowed by adspace.");
			}

		}
		if (getAttrsToParams().get(SdkGlobals.EMS_ZONEID) != null) {
			String as = savedInstance
					.getString(SdkGlobals.EMS_ATTRIBUTE_PREFIX
							+ SdkGlobals.EMS_ZONEID);
			putAttrToParam(SdkGlobals.EMS_ZONEID, SdkUtil
					.getContext().getString(R.string.amobeeAdSpace));
			putAttrValue(SdkGlobals.EMS_ZONEID, as);
		}
		if (getAttrsToParams().get(SdkGlobals.EMS_GEO) != null) {
			if (savedInstance.getBoolean(
					SdkGlobals.EMS_ATTRIBUTE_PREFIX
							+ SdkGlobals.EMS_GEO, false)) {
				double[] loc = getLocation();
				if (loc != null && 0.0 != loc[0]) {
					putAttrToParam(SdkGlobals.EMS_LAT, SdkUtil
							.getContext().getString(R.string.amobeeLatitude));
					putAttrValue(SdkGlobals.EMS_LAT,
							String.valueOf(loc[0]));
					putAttrToParam(SdkGlobals.EMS_LON, SdkUtil
							.getContext().getString(R.string.amobeeLongitude));
					putAttrValue(SdkGlobals.EMS_LON,
							String.valueOf(loc[1]));
					SdkLog.i(TAG, "Using " + loc[0] + "x" + loc[1]
							+ " as location.");
				} else {
					SdkLog.i(TAG, "Location too old or not fetchable.");
				}
			} else {
				SdkLog.d(TAG, "Location fetching not allowed by adspace.");
			}
		}
		if (getAttrsToParams().get(SdkGlobals.EMS_BACKFILL_SITEID) != null
				&& getAttrsToParams().get(
						SdkGlobals.EMS_BACKFILL_ZONEID) != null) {
			String site = savedInstance
					.getString(SdkGlobals.EMS_BACKFILL_SITEID);
			String zone = savedInstance
					.getString(SdkGlobals.EMS_BACKFILL_ZONEID);
			this.setDirectBackfill(new BackfillDelegator.BackfillData(site,
					zone, "", -1));
			SdkLog.d(TAG, "Direct backfill configuration detected. [site="
					+ site + ", zone=" + zone + "]");
		}

	}

	@Override
	public String getBaseUrlString() {
		return AmobeeSettingsAdapter.BASE_URL;
	}

	@Override
	public String getBaseQueryString() {
		return BASE_PARAMS;
	}

	@Override
	public String getRequestUrl() {
		return super.getRequestUrl() + "&t=" + System.currentTimeMillis();
	}

	private String getBatteryStatus() {
		return "&"
				+ SdkUtil.getContext().getResources()
						.getString(R.string.bLevelParam) + "="
				+ SdkUtil.getBatteryLevel();
	}

	private String getPhoneStatus() {
		String pStr = "&"
				+ SdkUtil.getContext().getResources()
						.getString(R.string.pStatusParam) + "=";
		if (SdkUtil.is3G()) {
			pStr += STATUS_3G_ON + ",";
		}
		if (SdkUtil.is4G()) {
			pStr += STATUS_4G_ON + ",";
		}
		if (SdkUtil.isGPSActive()) {
			pStr += STATUS_GPS_ON + ",";
		}
		if (SdkUtil.isPortrait()) {
			pStr += STATUS_PORTRAIT_MODE + ",";
		} else {
			pStr += STATUS_LANDSCAPE_MODE + ",";
		}
		if (SdkUtil.isHeadsetConnected()) {
			pStr += STATUS_HEADSET_CONNECTED + ",";
		}
		if (SdkUtil.isChargerConnected()) {
			pStr += STATUS_CHARGER_CONNECTED + ",";
		}
		if (SdkUtil.isWifi()) {
			pStr += STATUS_WIFI_ON;
		}
		return pStr.endsWith(",") ? pStr.substring(0, pStr.length() - 1) : pStr;
	}

	@Override
	public String getQueryString() {
		String qStr = super.getQueryString();
		qStr = qStr.concat(getPhoneStatus());
		qStr = qStr.concat(getBatteryStatus());
		return qStr;
	}

	@Override
	public int getSecurityHeaderValueHash() {
		return SECURITY_HEADER_VALUE;
	}

}
