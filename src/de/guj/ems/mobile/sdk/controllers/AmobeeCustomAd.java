package de.guj.ems.mobile.sdk.controllers;

import java.util.Iterator;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;
import de.guj.ems.mobile.sdk.views.AdResponseHandler;

/**
 * Abstract class template for custom ads. Performs an adrequest.
 * Classes implementing AmobeeCustomAd must implement the callback method "processResponse"
 * to actually handle the content of the adserver response.
 * 
 * @author stein16
 *
 */
public abstract class AmobeeCustomAd implements AdResponseHandler {

	private final static String TAG = "AmobeeCustomAd";
	
	AdServerSettingsAdapter settings;
	
	/**
	 * The empty default constructor is hidden
	 */
	@SuppressWarnings("unused")
	private AmobeeCustomAd() {
		// Hidden default constructor
	}
	
	/**
	 * Constructor for custom ads.
	 * @param context Android application context
	 * @param zoneId The ad space id
	 * @param geo wether to append geo data to the request
	 * @param uid whether to append a unique device id to the request
	 */
	public AmobeeCustomAd(Context context, String zoneId, boolean geo, boolean uid) {
		if (SdkUtil.getContext()  == null) {
			SdkUtil.setContext(context);
		}
		Bundle bundle = new Bundle();
		bundle.putString(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_ZONEID, zoneId);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_UUID, uid);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_GEO, geo);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_KEYWORDS, false);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_NKEYWORDS, false);
		this.settings = new AmobeeSettingsAdapter(context, bundle);
	}
	
	/**
	 * Constructor for custom ads.
	 * @param context Android application context
	 * @param zoneId The ad space id
	 * @param geo wether to append geo data to the request
	 * @param uid whether to append a unique device id to the request
	 * @param keywords optional array of keywords appended to the request
	 */
	public AmobeeCustomAd(Context context, String zoneId, String [] keywords, boolean geo, boolean uid) {
		if (SdkUtil.getContext()  == null) {
			SdkUtil.setContext(context);
		}
		Bundle bundle = new Bundle();
		bundle.putString(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_ZONEID, zoneId);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_UUID, uid);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_GEO, geo);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_KEYWORDS, keywords != null && keywords.length > 0);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_NKEYWORDS, false);
		this.settings = new AmobeeSettingsAdapter(context, bundle, keywords, null);		
	}
	
	/**
	 * Constructor for custom ads.
	 * @param context Android application context
	 * @param zoneId The ad space id
	 * @param geo wether to append geo data to the request
	 * @param uid whether to append a unique device id to the request
	 * @param keywords optional array of keywords appended to the request
	 * @param nkeywords optional array of non-keywords appended to the request
	 */
	public AmobeeCustomAd(Context context, String zoneId, String [] keywords, String [] nkeywords, boolean geo, boolean uid) {
		if (SdkUtil.getContext()  == null) {
			SdkUtil.setContext(context);
		}
		Bundle bundle = new Bundle();
		bundle.putString(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_ZONEID, zoneId);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_UUID, uid);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_GEO, geo);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_KEYWORDS, keywords != null && keywords.length > 0);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_NKEYWORDS, nkeywords != null && nkeywords.length > 0);
		this.settings = new AmobeeSettingsAdapter(context, bundle, keywords, nkeywords);		
	}
	
	/**
	 * Constructor for custom ads.
	 * @param context Android application context
	 * @param zoneId The ad space id
	 * @param geo wether to append geo data to the request
	 * @param uid whether to append a unique device id to the request
	 * @param customParams Map of custom parameters (String, Double or Integer objects)
	 */
	public AmobeeCustomAd(Context context, String zoneId, Map<String, ?> customParams, boolean geo, boolean uid) {
		if (SdkUtil.getContext()  == null) {
			SdkUtil.setContext(context);
		}
		Bundle bundle = new Bundle();
		bundle.putString(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_ZONEID, zoneId);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_UUID, uid);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_GEO, geo);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_KEYWORDS, false);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_NKEYWORDS, false);
		this.settings = new AmobeeSettingsAdapter(context, bundle);
		this.addCustomParams(customParams);
		
	}
	
	/**
	 * Constructor for custom ads.
	 * @param context Android application context
	 * @param zoneId The ad space id
	 * @param geo wether to append geo data to the request
	 * @param uid whether to append a unique device id to the request
	 * @param keywords optional array of keywords appended to the request
	 * @param customParams Map of custom parameters (String, Double or Integer objects)
	 */
	public AmobeeCustomAd(Context context, String zoneId, Map<String, ?> customParams, String [] keywords, boolean geo, boolean uid) {
		if (SdkUtil.getContext()  == null) {
			SdkUtil.setContext(context);
		}
		Bundle bundle = new Bundle();
		bundle.putString(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_ZONEID, zoneId);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_UUID, uid);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_GEO, geo);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_KEYWORDS, keywords != null && keywords.length > 0);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_NKEYWORDS, false);
		this.settings = new AmobeeSettingsAdapter(context, bundle, keywords, null);
		this.addCustomParams(customParams);
				
	}
	
	/**
	 * Constructor for custom ads.
	 * @param context Android application context
	 * @param zoneId The ad space id
	 * @param geo wether to append geo data to the request
	 * @param uid whether to append a unique device id to the request
	 * @param keywords optional array of keywords appended to the request
	 * @param nkeywords optional array of non-keywords appended to the request
	 * @param customParams Map of custom parameters (String, Double or Integer objects)
	 */
	public AmobeeCustomAd(Context context, String zoneId, Map<String, ?> customParams, String [] keywords, String [] nkeywords, boolean geo, boolean uid) {
		if (SdkUtil.getContext()  == null) {
			SdkUtil.setContext(context);
		}
		Bundle bundle = new Bundle();
		bundle.putString(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_ZONEID, zoneId);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_UUID, uid);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_GEO, geo);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_KEYWORDS, keywords != null && keywords.length > 0);
		bundle.putBoolean(AdServerSettingsAdapter.EMS_ATTRIBUTE_PREFIX + AdServerSettingsAdapter.EMS_NKEYWORDS, nkeywords != null && nkeywords.length > 0);
		this.settings = new AmobeeSettingsAdapter(context, bundle, keywords, nkeywords);
		this.addCustomParams(customParams);		
	}
	
	private void addCustomParams(Map<String, ?> params) {
		Iterator<String> mi = params.keySet().iterator();
		while (mi.hasNext()) {
			String param = mi.next();
			Object value = params.get(param);
			if (value.getClass().equals(String.class)) {
				this.settings.addCustomRequestParameter(param, (String) value);
			} else if (value.getClass().equals(Double.class)) {
				this.settings.addCustomRequestParameter(param,
						((Double) value).doubleValue());
			} else if (value.getClass().equals(Integer.class)) {
				this.settings.addCustomRequestParameter(param,
						((Integer) value).intValue());
			} else {
				SdkLog.e(TAG,
						"Unknown object in custom params. Only String, Integer, Double allowed.");
			}
		}
	}
	
	
	/**
	 * Performs the request to the adserver
	 */
	public void load() {
		if (SdkUtil.isOnline()) {
			final String url = this.settings.getRequestUrl();
			SdkLog.i(TAG, "START AdServer request");
			new AdServerAccess(
					SdkUtil.getUserAgent(), this).execute(new String[] { url });


		} else if (SdkUtil.isOffline()) {
			SdkLog.i(TAG, "No network connection - not requesting ads.");
		}		
	}
	
	/**
	 * Calback method for the finished ad request. Perform your tasks on the adserver's response here.
	 * @param response The adservers response as a strign (null if the response was empty)
	 */
	public abstract void processResponse(String response);

}
