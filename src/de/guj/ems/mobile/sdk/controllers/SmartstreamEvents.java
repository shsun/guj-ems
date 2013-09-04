package de.guj.ems.mobile.sdk.controllers;

import de.guj.ems.mobile.sdk.R;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;

public final class SmartstreamEvents {
	
	private final static String SMARTSTREAM_EVENT_URL = SdkUtil.getContext().getString(R.string.baseUrl) + "?" + SdkUtil.getContext().getString(R.string.baseParams).replaceAll("#version#", SdkUtil.VERSION_STR);
	
	public final static int SMARTSTREAM_EVENT_IMPRESSION = 15549;
	
	public final static int SMARTSTREAM_EVENT_FAIL = 15539;
	
	public final static int SMARTSTREAM_EVENT_PLAY = 15537;
	
	public final static int SMARTSTREAM_EVENT_QUARTILE_1 = 15541;
	
	public final static int SMARTSTREAM_EVENT_MID = 15543;
	
	public final static int SMARTSTREAM_EVENT_QUARTILE_3 = 15545;
	
	public final static int SMARTSTREAM_EVENT_FINISH = 15547;
	
	private final static String TAG = "SmartstreamEvents";
	
	public static void processEvent(String userAgent, String adSpace, String placement, int event, boolean click) {
		AdServerAccess access = null;
		String url = SMARTSTREAM_EVENT_URL;
		boolean ok = false;
		switch (event) {
		case SMARTSTREAM_EVENT_FAIL:
		case SMARTSTREAM_EVENT_PLAY:
		case SMARTSTREAM_EVENT_QUARTILE_1:
		case SMARTSTREAM_EVENT_MID:
		case SMARTSTREAM_EVENT_QUARTILE_3:
		case SMARTSTREAM_EVENT_FINISH:
		case SMARTSTREAM_EVENT_IMPRESSION:
			ok = true;
			break;
		default:
			SdkLog.e(TAG, "Illegal event [" + event + "] passed to processEvent.");
			return;
		}
		if (ok && !click) {
			access = new AdServerAccess(userAgent, null);
			url += "&t=" + System.currentTimeMillis() + "&as=" + event + "&plmid=" + placement;
			try {
				((AdServerAccess) access.execute(new String []{url})).get();
			}
			catch (Exception e) {
				SdkLog.e(TAG, "Error sending tracking event to AdServer", e);
			}
		}
		else {
			// TODO handle video interstitial click events
			SdkLog.w(TAG, "Video Interstitial clicks are not being reported, yet.");
		}
	}

}
