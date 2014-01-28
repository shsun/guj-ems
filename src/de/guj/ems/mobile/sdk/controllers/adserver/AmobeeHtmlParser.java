package de.guj.ems.mobile.sdk.controllers.adserver;

import de.guj.ems.mobile.sdk.util.SdkLog;

public class AmobeeHtmlParser extends AdResponseParser {

	private final static String TAG = "AmobeeHtmlParser";

	public AmobeeHtmlParser(String response) {
		super(response);
	}

	private AmobeeHtmlParser(String response, boolean xml) {
		super(response, xml);
	}

	private void parseClickUrl() {
		String c = getResponse().substring(getResponse().indexOf("href=") + 6);
		String tC = c.substring(0, c.indexOf("\""));
		setClickUrl(tC.replaceAll("&amp;", "&"));
		SdkLog.d(TAG, "Ad Click URL = " + getClickUrl());
	}

	private void parseImageUrl() {
		String i = getResponse().substring(getResponse().indexOf("src=") + 5);
		setImageUrl(i.substring(0, i.indexOf("\"")));
		SdkLog.d(TAG, "Ad Image URL = " + getImageUrl());
	}

	private void parseTrackingUrl() {
		String i = getResponse().substring(
				getResponse().lastIndexOf("notification"));
		if (i != null && i.length() > 1) {
			String tI = "http://vfdeprod.amobee.com/upsteed/"
					+ i.substring(0, i.indexOf("\""));
			setTrackingImageUrl(tI.replaceAll("&amp;", "&"));
			SdkLog.d(TAG, "Ad Tracking URL = " + getTrackingImageUrl());
		}
	}

	@Override
	protected void process() {
		super.process();
		try {
			parseClickUrl();
			parseImageUrl();
			parseTrackingUrl();
		} catch (Exception e) {
			SdkLog.e(TAG, "Error parsing Amobee HTML.", e);
			setInvalid();
		}

	}

}
