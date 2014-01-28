package de.guj.ems.mobile.sdk.controllers.adserver;

import de.guj.ems.mobile.sdk.util.SdkLog;

public class OptimobileXmlParser extends AdResponseParser {

	private final static String TAG = "OptimobileXmlParser";

	public OptimobileXmlParser(String response) {
		super(response, true);
	}

	private OptimobileXmlParser(String response, boolean xml) {
		super(response, xml);
	}

	private void parseClickUrl() {
		String c = getResponse().substring(
				getResponse().indexOf("<url><![CDATA") + 14);
		setClickUrl(c.substring(0, c.indexOf("]")));
		SdkLog.d(TAG, "Ad Click URL = " + getClickUrl());
	}

	private void parseImageUrl() {
		String i = getResponse().substring(
				getResponse().indexOf("img.ads.mocean") - 7);
		setImageUrl(i.substring(0, i.indexOf("]")));
		SdkLog.d(TAG, "Ad Image URL = " + getImageUrl());
	}

	private void parseTrackingUrl() {
		String i = getResponse().substring(
				getResponse().indexOf("<track><![CDATA") + 16);
		if (i != null) {
			String ti = i.substring(0, i.indexOf("]"));
			if (ti != null && ti.startsWith("http")) {
				setTrackingImageUrl(ti);
				SdkLog.d(TAG, "Ad Tracking URL = " + getTrackingImageUrl());
			}
		} else {
			SdkLog.d(TAG, "No tracking image in optimobile XML");
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
			SdkLog.e(TAG, "Error parsing optimoble XML.", e);
			setInvalid();
		}

	}

}
