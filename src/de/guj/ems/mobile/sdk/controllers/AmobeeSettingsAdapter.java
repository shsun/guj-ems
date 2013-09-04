package de.guj.ems.mobile.sdk.controllers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import de.guj.ems.mobile.sdk.R;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;

/**
 * This class implements all available ad server parameters for the Amobee
 * AdServer
 * 
 * Base URL http://vfdeprod.amobee.com Base Query String "?tp=4&prt=G+J" The
 * following parameters are addded at runtime: "as" for the zoneId, "uid" for a
 * unique device id and "t", containing the timestamp in milliseconds since Jan
 * 1st 1970, "lat", "lon" if "ems_geo" is set to true
 * 
 * Custom params and keywords (kw=/nkw=) can be added for programmatically added views.
 * 
 * @author stein16
 * 
 */
public final class AmobeeSettingsAdapter extends AdServerSettingsAdapter {

	private final static char STATUS_3G_ON = '3';
	
	private final static char STATUS_4G_ON = '4';
	
	private final static char STATUS_GPS_ON = 'g';
	
	private final static char STATUS_PORTRAIT_MODE = 'p';
	
	private final static char STATUS_HEADSET_CONNECTED = 'h';
	
	private final static char STATUS_CHARGER_CONNECTED = 'c';
	
	private final static char STATUS_WIFI_ON = 'w';
	
	private final static char STATUS_LANDSCAPE_MODE = 'l';
	
	private final static String BASE_URL = SdkUtil.getContext()
			.getString(R.string.baseUrl);

	private final static String BASE_PARAMS = "?" + SdkUtil.getContext()
			.getString(R.string.baseParams).replaceAll("#version#", SdkUtil.VERSION_STR);

	private final static String TAG = "AmobeeSettingsAdapter";
	
	/**
	 * Constructor with all attributes stored in an AttributeSet 
	 * @param context android application context
	 * @param set attribute set with configuration
	 */
	public AmobeeSettingsAdapter(Context context, AttributeSet set) {
		super(set);

		TypedArray tVals = context.obtainStyledAttributes(set,
				R.styleable.GuJEMSAdView);
		if (getAttrsToParams().get(AdServerSettingsAdapter.EMS_UUID) != null) {
			if (tVals
					.getBoolean(AdServerSettingsAdapter.EMS_UUID_ID, false)) {
				putAttrToParam(AdServerSettingsAdapter.EMS_UUID,
						SdkUtil.getContext()
								.getString(R.string.amobeeUserId));
				putAttrValue(AdServerSettingsAdapter.EMS_UUID, SdkUtil.getDeviceId());
			}
			else {
				SdkLog.d(TAG, "Device id transmission not allowed by adspace.");
			}
		}
		if (getAttrsToParams().get(AdServerSettingsAdapter.EMS_ZONEID) != null) {
			String as = tVals.getString(AdServerSettingsAdapter.EMS_ZONEID_ID);
			putAttrToParam(AdServerSettingsAdapter.EMS_ZONEID,
					SdkUtil.getContext()
							.getString(R.string.amobeeAdSpace));
			putAttrValue(AdServerSettingsAdapter.EMS_ZONEID, as);
		}
		if (getAttrsToParams().get(AdServerSettingsAdapter.EMS_GEO) != null) {
			if (tVals
					.getBoolean(AdServerSettingsAdapter.EMS_GEO_ID, false)) {
				double[] loc = getLocation();
				if (loc != null && 0.0 != loc[0]) {
					putAttrToParam(AdServerSettingsAdapter.EMS_LAT,
							SdkUtil.getContext()
									.getString(R.string.amobeeLatitude));
					putAttrValue(AdServerSettingsAdapter.EMS_LAT,
							String.valueOf(loc[0]));
					putAttrToParam(AdServerSettingsAdapter.EMS_LON,
							SdkUtil.getContext()
									.getString(R.string.amobeeLongitude));
					putAttrValue(AdServerSettingsAdapter.EMS_LON,
							String.valueOf(loc[1]));
				} else {
					SdkLog.i(TAG, "ems_geo: location too old or not fetchable.");
				}
			}
			else {
				SdkLog.d(TAG, "ems_geo: location fetching not allowed by adspace.");
			}
		}
		if (getAttrsToParams().get(AdServerSettingsAdapter.EMS_BACKFILL_SITEID) != null && getAttrsToParams().get(AdServerSettingsAdapter.EMS_BACKFILL_ZONEID) != null) {
			String site = tVals.getString(AdServerSettingsAdapter.EMS_BACKFILL_SITEID_ID);
			String zone = tVals.getString(AdServerSettingsAdapter.EMS_BACKFILL_ZONEID_ID);
			this.setDirectBackfill(new BackfillDelegator.BackfillData(site, zone, "", -1));
			SdkLog.d(TAG, "Direct backfill configuration detected. [site=" + site + ", zone=" + zone);			
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
		}
		catch (UnsupportedEncodingException e) {
			SdkLog.e(TAG, "Error encoding query string.", e);
		}
		return null;
	}

	/**
	 * Constructor with additional array of keywords and non-keywords
	 * which will be added to the ad server requests.
	 * Use this constructor to add parameters to the request during runtime
	 * @param context android application context
	 * @param set attribute set with configuration
	 * @param kws matching keywords
	 * @param nkws non-matching keywords
	 */
	public AmobeeSettingsAdapter(Context context, AttributeSet set,
			String[] kws, String[] nkws) {
		this(context, set);
		TypedArray tVals = context.obtainStyledAttributes(set,
				R.styleable.GuJEMSAdView);
		if (kws != null
				&& kws.length > 0
				&& getAttrsToParams().get(AdServerSettingsAdapter.EMS_KEYWORDS) != null) {
			if (tVals.getBoolean(AdServerSettingsAdapter.EMS_KEYWORDS_ID, false)) {
				String kwstr = strArrToString(kws);
				putAttrValue(AdServerSettingsAdapter.EMS_KEYWORDS, kwstr);
			}
			else {
				SdkLog.d(TAG, "Skipped keywords because view is not configured with ems_kw=true.");
			}			
		}
		if (nkws != null
				&& nkws.length > 0
				&& getAttrsToParams()
						.get(AdServerSettingsAdapter.EMS_NKEYWORDS) != null) {
			if (Boolean.valueOf(getAttrsToParams().get(AdServerSettingsAdapter.EMS_NKEYWORDS)).booleanValue()) {
				String nkwstr = strArrToString(nkws);
				putAttrValue(AdServerSettingsAdapter.EMS_NKEYWORDS, nkwstr);
			}
			else {
				SdkLog.d(TAG, "Skipped non-keywords because view is not configured with ems_nkw=true.");
			}
		}
		tVals.recycle();
	}

	/**
	 * Constructor with additional array of keywords and non-keywords
	 * which will be added to the ad server requests.
	 * Use this constructor to add parameters to the request during runtime
	 * @param context android application context
	 * @param savedInstance bundle with configuration
	 * @param kws matching keywords
	 * @param nkws non-matching keywords
	 */
	public AmobeeSettingsAdapter(Context context, Bundle savedInstance,
			String[] kws, String[] nkws) {
		this(context, savedInstance);
		if (kws != null
				&& kws.length > 0
				&& getAttrsToParams().get(AdServerSettingsAdapter.EMS_KEYWORDS) != null) {
			if (savedInstance.getBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_KEYWORDS, false)) {
				String kwstr = strArrToString(kws);
				putAttrValue(AdServerSettingsAdapter.EMS_KEYWORDS, kwstr);
			}
			else {
				SdkLog.d(TAG, "Skipped keywords because view is not configured with ems_kw=true.");
			}			
		}
		if (nkws != null
				&& nkws.length > 0
				&& getAttrsToParams()
						.get(AdServerSettingsAdapter.EMS_NKEYWORDS) != null) {
			if (savedInstance.getBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_NKEYWORDS, false)) {
				String nkwstr = strArrToString(nkws);
				putAttrValue(AdServerSettingsAdapter.EMS_NKEYWORDS, nkwstr);
			}
			else {
				SdkLog.d(TAG, "Skipped non-keywords because view is not configured with ems_nkw=true.");
			}
		}
	}

	/**
	 * Constructor with configuration in bundle
	 * @param context android application context
	 * @param savedInstance bundle with configuration
	 */
	public AmobeeSettingsAdapter(Context context, Bundle savedInstance) {
		super(savedInstance);
		if (getAttrsToParams().get(AdServerSettingsAdapter.EMS_UUID) != null) {
			if (savedInstance.getBoolean(
					AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX
							+ AdServerSettingsAdapter.EMS_UUID, false)) {
				putAttrToParam(AdServerSettingsAdapter.EMS_UUID,
						SdkUtil.getContext()
								.getString(R.string.amobeeUserId));
				putAttrValue(AdServerSettingsAdapter.EMS_UUID, SdkUtil.getDeviceId());
			}
			else {
				SdkLog.d(TAG, "ems_uid: device id transmission not allowed by adspace.");
			}

		}
		if (getAttrsToParams().get(AdServerSettingsAdapter.EMS_ZONEID) != null) {
			String as = savedInstance
					.getString(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX
							+ AdServerSettingsAdapter.EMS_ZONEID);
			putAttrToParam(AdServerSettingsAdapter.EMS_ZONEID,
					SdkUtil.getContext()
							.getString(R.string.amobeeAdSpace));
			putAttrValue(AdServerSettingsAdapter.EMS_ZONEID, as);
		}
		if (getAttrsToParams().get(AdServerSettingsAdapter.EMS_GEO) != null) {
			if (savedInstance.getBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_GEO, false)) {
				double[] loc = getLocation();
				if (loc != null && 0.0 != loc[0]) {
					putAttrToParam(AdServerSettingsAdapter.EMS_LAT,
							SdkUtil.getContext()
									.getString(R.string.amobeeLatitude));
					putAttrValue(AdServerSettingsAdapter.EMS_LAT,
							String.valueOf(loc[0]));
					putAttrToParam(AdServerSettingsAdapter.EMS_LON,
							SdkUtil.getContext()
									.getString(R.string.amobeeLongitude));
					putAttrValue(AdServerSettingsAdapter.EMS_LON,
							String.valueOf(loc[1]));
					SdkLog.i(TAG, "ems_geo: using " + loc[0] + "x" + loc[1] + " as location.");
				} else {
					SdkLog.i(TAG, "ems_geo: location too old or not fetchable.");
				}
			}
			else {
				SdkLog.d(TAG, "ems_geo: location fetching not allowed by adspace.");
			}
		}
		if (getAttrsToParams().get(AdServerSettingsAdapter.EMS_BACKFILL_SITEID) != null && getAttrsToParams().get(AdServerSettingsAdapter.EMS_BACKFILL_ZONEID) != null) {
			String site = savedInstance.getString(AdServerSettingsAdapter.EMS_BACKFILL_SITEID);
			String zone = savedInstance.getString(AdServerSettingsAdapter.EMS_BACKFILL_ZONEID);
			this.setDirectBackfill(new BackfillDelegator.BackfillData(site, zone, "", -1));
			SdkLog.d(TAG, "Direct backfill configuration detected. [site=" + site + ", zone=" + zone);		}
		
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
		return "&" + SdkUtil.getContext().getResources().getString(R.string.bLevelParam) + "=" + SdkUtil.getBatteryLevel(); 
	}
	
	private String getPhoneStatus() {
		String pStr = "&" + SdkUtil.getContext().getResources().getString(R.string.pStatusParam) + "=";
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
		}
		else {
			pStr += STATUS_LANDSCAPE_MODE +	",";
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
		return pStr.endsWith(",") ? pStr.substring(0,  pStr.length() - 1) : pStr;
	}
	
	@Override
	public String getQueryString() {
		String qStr = super.getQueryString();
		qStr = qStr.concat(getPhoneStatus());
		qStr = qStr.concat(getBatteryStatus());
		return qStr;
	}
	
}
