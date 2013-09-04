package de.guj.ems.mobile.sdk.controllers;

import java.util.Map;

import de.guj.ems.mobile.sdk.util.SdkLog;

import android.content.Context;

public class SimpleDumpAd extends AmobeeCustomAd {

	private final static String TAG = "SimpleDumpAd";
	
	public SimpleDumpAd(Context context, String zoneId,
			Map<String, ?> customParams, String[] keywords, String[] nkeywords,
			boolean geo, boolean uid) {
		super(context, zoneId, customParams, keywords, nkeywords, geo, uid);
		super.load();
	}

	@Override
	public void processResponse(String response) {
		SdkLog.d(TAG, "DUMP: " + response);
	}

	@Override
	public void processError(String msg) {
		SdkLog.e(TAG, "ERROR: " + msg);
		
	}

	@Override
	public void processError(String msg, Throwable t) {
		SdkLog.e(TAG, "ERROR: " + msg, t);
		
	}

}
