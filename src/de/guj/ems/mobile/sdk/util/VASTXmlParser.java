package de.guj.ems.mobile.sdk.util;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.util.Xml;

/**
 * Implementation of VAST 2.0 XML Parser using XmlPullParser.
 * The parser finds all trackings, settings and the actual mediafile.
 * If the plain data contains a wrapped VAST xml, it is fetched by triggering 
 * a callback on the UI thread which should fetch the additional file.
 * 
 * Wrapped VAST xml trackings and original trackings will be combined, i.e.
 * you will receive a list of URLs for all trackings.
 *   
 * @author stein16
 *
 */
public class VASTXmlParser {

	private Context context;

	private VASTWrapperListener wrapperListener;

	private final static String TAG = "VASTXmlParser";

	private final static String VAST_ADTAGURI_TAG = "VASTAdTagURI";

	private final static String VAST_START_TAG = "VAST";

	private final static String VAST_AD_TAG = "Ad";

	private final static String VAST_INLINE_TAG = "InLine";

	private final static String VAST_WRAPPER_TAG = "Wrapper";

	private final static String VAST_IMPRESSION_TAG = "Impression";

	private final static String VAST_CREATIVES_TAG = "Creatives";

	private final static String VAST_CREATIVE_TAG = "Creative";

	private final static String VAST_LINEAR_TAG = "Linear";

	private final static String VAST_DURATION_TAG = "Duration";

	private final static String VAST_TRACKINGEVENTS_TAG = "TrackingEvents";

	private final static String VAST_TRACKING_TAG = "Tracking";

	private final static String VAST_MEDIAFILES_TAG = "MediaFiles";

	private final static String VAST_MEDIAFILE_TAG = "MediaFile";

	private final static String VAST_VIDEOCLICKS_TAG = "VideoClicks";

	private final static String VAST_CLICKTHROUGH_TAG = "ClickThrough";

	private final static String VAST_CLICKTRACKING_TAG = "ClickTracking";

	private boolean ready;

	private volatile boolean hasWrapper;

	private volatile VASTXmlParser wrappedVASTXml;

	private String clickThroughUrl;

	private String clickTrackingUrl;

	private int skipOffset;

	private String impressionTrackerUrl;

	private String duration;

	private String mediaFileUrl;

	private List<Tracking> trackings;

	public class Tracking {

		public final static int EVENT_FINAL_RETURN = 0;

		public final static int EVENT_IMPRESSION = 1;

		public final static int EVENT_START = 2;

		public final static int EVENT_FIRSTQ = 3;

		public final static int EVENT_MID = 4;

		public final static int EVENT_THIRDQ = 5;

		public final static int EVENT_COMPLETE = 6;

		public final static int EVENT_MUTE = 7;

		public final static int EVENT_UNMUTE = 8;

		public final static int EVENT_PAUSE = 9;

		public final static int EVENT_RESUME = 10;
		
		public final static int EVENT_FULLSCREEN = 11;

		public final String[] EVENT_MAPPING = new String[] { "finalReturn",
				"impression", "start", "firstQuartile", "midpoint",
				"thirdQuartile", "complete", "mute", "unmute", "pause",
				"resume", "fullscreen" };

		private int event;

		private String url;

		public Tracking(String e, String url) {
			this.event = findEvent(e);
			this.url = url;
			SdkLog.d(TAG, "VAST tracking url [" + e + ", " + this.event + "]: "
					+ this.url);
		}

		private int findEvent(String event) {
			for (int i = 0; i < EVENT_MAPPING.length; i++) {
				if (EVENT_MAPPING[i].equals(event)) {
					return i;
				}
			}
			return -1;
		}

		public int getEvent() {
			return this.event;
		}

		public String getUrl() {
			return this.url;
		}

	}

	public VASTXmlParser(Context c, VASTWrapperListener listener, String data) {
		this.trackings = new ArrayList<Tracking>();
		this.ready = false;
		this.context = c;
		this.wrapperListener = listener;
		if (SdkUtil.getContext() == null) {
			SdkUtil.setContext(context);
		}
		try {
			readVAST(data);
		} catch (Exception e) {
			SdkLog.e(TAG, "Error parsing VAST XML", e);
		}
		this.ready = true;
	}

	private void readVAST(String data) throws XmlPullParserException,
			IOException {

		XmlPullParser parser = Xml.newPullParser();
		parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
		parser.setInput(new StringReader(data));
		parser.nextTag();
		parser.require(XmlPullParser.START_TAG, null, VAST_START_TAG);
		while (parser.next() != XmlPullParser.END_TAG) {
			if (parser.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			if (parser.getName().equals(VAST_AD_TAG)) {
				readAd(parser);
			}
		}
	}

	private void readAd(XmlPullParser p) throws IOException,
			XmlPullParserException {
		p.require(XmlPullParser.START_TAG, null, VAST_AD_TAG);
		while (p.next() != XmlPullParser.END_TAG) {
			if (p.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = p.getName();
			if (name.equals(VAST_INLINE_TAG)) {
				SdkLog.i(TAG, "VAST file contains inline ad information.");
				readInLine(p);
			}
			if (name.equals(VAST_WRAPPER_TAG)) {
				SdkLog.i(TAG, "VAST file contains wrapped ad information.");
				hasWrapper = true;
				readWrapper(p);
			}
		}
	}

	private void readMediaFiles(XmlPullParser p) throws IOException,
			XmlPullParserException {
		p.require(XmlPullParser.START_TAG, null, VAST_MEDIAFILES_TAG);
		while (p.next() != XmlPullParser.END_TAG) {
			if (p.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = p.getName();
			if (name != null && name.equals(VAST_MEDIAFILE_TAG)) {
				p.require(XmlPullParser.START_TAG, null, VAST_MEDIAFILE_TAG);
				this.mediaFileUrl = readText(p);
				p.require(XmlPullParser.END_TAG, null, VAST_MEDIAFILE_TAG);
				SdkLog.i(TAG, "Mediafile url: " + this.mediaFileUrl);
			} else {
				skip(p);
			}
		}
	}

	private void readTrackingEvents(XmlPullParser p) throws IOException,
			XmlPullParserException {
		p.require(XmlPullParser.START_TAG, null, VAST_TRACKINGEVENTS_TAG);
		while (p.next() != XmlPullParser.END_TAG) {
			if (p.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = p.getName();
			if (name != null && name.equals(VAST_TRACKING_TAG)) {
				String ev = p.getAttributeValue(null, "event");
				p.require(XmlPullParser.START_TAG, null, VAST_TRACKING_TAG);
				this.trackings.add(new Tracking(ev, readText(p)));
				SdkLog.d(TAG, "Added VAST tracking \"" + ev + "\"");
				p.require(XmlPullParser.END_TAG, null, VAST_TRACKING_TAG);
			} else {
				skip(p);
			}
		}
	}

	private void readVideoClicks(XmlPullParser p) throws IOException,
			XmlPullParserException {
		p.require(XmlPullParser.START_TAG, null, VAST_VIDEOCLICKS_TAG);
		while (p.next() != XmlPullParser.END_TAG) {
			if (p.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = p.getName();
			if (name != null && name.equals(VAST_CLICKTHROUGH_TAG)) {
				p.require(XmlPullParser.START_TAG, null, VAST_CLICKTHROUGH_TAG);
				this.clickThroughUrl = readText(p);
				SdkLog.d(TAG, "Video clickthrough url: " + clickThroughUrl);
				p.require(XmlPullParser.END_TAG, null, VAST_CLICKTHROUGH_TAG);
			} else if (name != null && name.equals(VAST_CLICKTRACKING_TAG)) {
				p.require(XmlPullParser.START_TAG, null, VAST_CLICKTRACKING_TAG);
				this.clickTrackingUrl = readText(p);
				SdkLog.d(TAG, "Video clicktracking url: " + clickThroughUrl);
				p.require(XmlPullParser.END_TAG, null, VAST_CLICKTRACKING_TAG);
			} else {
				skip(p);
			}
		}
	}

	private void readLinear(XmlPullParser p) throws IOException,
			XmlPullParserException {
		p.require(XmlPullParser.START_TAG, null, VAST_LINEAR_TAG);
		while (p.next() != XmlPullParser.END_TAG) {
			String name = p.getName();
			if (p.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			if (name != null && name.equals(VAST_DURATION_TAG)) {
				p.require(XmlPullParser.START_TAG, null, VAST_DURATION_TAG);
				this.duration = readText(p);
				p.require(XmlPullParser.END_TAG, null, VAST_DURATION_TAG);

				SdkLog.d(TAG, "Video duration: " + this.duration);
			} else if (name != null && name.equals(VAST_TRACKINGEVENTS_TAG)) {
				readTrackingEvents(p);
			} else if (name != null && name.equals(VAST_MEDIAFILES_TAG)) {
				readMediaFiles(p);
			} else if (name != null && name.equals(VAST_VIDEOCLICKS_TAG)) {
				readVideoClicks(p);
			} else {
				skip(p);
			}
		}
	}

	private void readCreative(XmlPullParser p) throws IOException,
			XmlPullParserException {
		p.require(XmlPullParser.START_TAG, null, VAST_CREATIVE_TAG);
		while (p.next() != XmlPullParser.END_TAG) {
			if (p.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = p.getName();
			if (name != null && name.equals(VAST_LINEAR_TAG)) {
				String skipoffsetStr = p.getAttributeValue(null, "skipoffset");
				if (skipoffsetStr != null && skipoffsetStr.indexOf(":") < 0) {
					skipOffset = Integer.parseInt(skipoffsetStr.substring(0,
							skipoffsetStr.length() - 1));
					SdkLog.i(TAG, "Linear skipoffset is " + skipOffset + " [%]");
				} else if (skipoffsetStr != null
						&& skipoffsetStr.indexOf(":") >= 0) {
					skipOffset = -1;
					SdkLog.w(
							TAG,
							"Absolute time value ignored for skipOffset in VAST xml. Only percentage values will pe parsed.");
				}
				readLinear(p);
			} else {
				skip(p);
			}
		}
	}

	private void readCreatives(XmlPullParser p) throws IOException,
			XmlPullParserException {
		p.require(XmlPullParser.START_TAG, null, VAST_CREATIVES_TAG);
		while (p.next() != XmlPullParser.END_TAG) {
			if (p.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = p.getName();
			if (name != null && name.equals(VAST_CREATIVE_TAG)) {
				readCreative(p);
			} else {
				skip(p);
			}
		}
	}

	private void getWrappedVast(XmlPullParser p) throws IOException,
			XmlPullParserException {
		p.require(XmlPullParser.START_TAG, null, VAST_ADTAGURI_TAG);
		String url = readText(p);
		p.require(XmlPullParser.END_TAG, null, VAST_ADTAGURI_TAG);
		if (wrapperListener != null) {
			wrapperListener.onVASTWrapperFound(url);
		} else {
			SdkLog.e(TAG, "No listener set for wrapped VAST xml.");
		}
		// this.wrappedVASTXml = new VASTXmlParser(context, new URL(url));

	}

	private void readWrapper(XmlPullParser p) throws IOException,
			XmlPullParserException {
		p.require(XmlPullParser.START_TAG, null, VAST_WRAPPER_TAG);
		while (p.next() != XmlPullParser.END_TAG) {
			if (p.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = p.getName();
			if (name != null && name.equals(VAST_IMPRESSION_TAG)) {
				p.require(XmlPullParser.START_TAG, null, VAST_IMPRESSION_TAG);
				this.impressionTrackerUrl = readText(p);
				p.require(XmlPullParser.END_TAG, null, VAST_IMPRESSION_TAG);

				SdkLog.d(TAG, "Impression tracker url: "
						+ this.impressionTrackerUrl);
			} else if (name != null && name.equals(VAST_CREATIVES_TAG)) {
				readCreatives(p);
			} else if (name != null && name.equals(VAST_ADTAGURI_TAG)) {
				getWrappedVast(p);
			} else {
				skip(p);
			}
		}
	}

	private void readInLine(XmlPullParser p) throws IOException,
			XmlPullParserException {
		p.require(XmlPullParser.START_TAG, null, VAST_INLINE_TAG);
		while (p.next() != XmlPullParser.END_TAG) {
			if (p.getEventType() != XmlPullParser.START_TAG) {
				continue;
			}
			String name = p.getName();
			if (name != null && name.equals(VAST_IMPRESSION_TAG)) {
				p.require(XmlPullParser.START_TAG, null, VAST_IMPRESSION_TAG);
				this.impressionTrackerUrl = readText(p);
				p.require(XmlPullParser.END_TAG, null, VAST_IMPRESSION_TAG);

				SdkLog.d(TAG, "Impression tracker url: "
						+ this.impressionTrackerUrl);
			} else if (name != null && name.equals(VAST_CREATIVES_TAG)) {
				readCreatives(p);
			} else {
				skip(p);
			}
		}
	}

	private void skip(XmlPullParser p) throws XmlPullParserException,
			IOException {
		if (p.getEventType() != XmlPullParser.START_TAG) {
			throw new IllegalStateException();
		}
		int depth = 1;
		while (depth != 0) {
			switch (p.next()) {
			case XmlPullParser.END_TAG:
				depth--;
				break;
			case XmlPullParser.START_TAG:
				depth++;
				break;
			}
		}
	}

	private String readText(XmlPullParser parser) throws IOException,
			XmlPullParserException {
		String result = "";
		if (parser.next() == XmlPullParser.TEXT) {
			result = parser.getText();
			parser.nextTag();
		} else {
			SdkLog.w(TAG, "No text: " + parser.getName());
		}
		return result.trim();
	}

	public List<String> getImpressionTrackerUrl() {
		waitForWrapper();
		
		List<String> urls = new ArrayList<String>();
		urls.add(this.impressionTrackerUrl);
		if (wrappedVASTXml != null) {
			urls.addAll(wrappedVASTXml.getImpressionTrackerUrl());
		}

		return urls;
	}

	public String getDuration() {
		waitForWrapper();
		
		if (duration == null && wrappedVASTXml != null) {
			return wrappedVASTXml.getDuration();
		}
		return duration;
	}

	public String getMediaFileUrl() {
		waitForWrapper();

		if (mediaFileUrl == null && wrappedVASTXml != null) {
			return wrappedVASTXml.getMediaFileUrl();
		}
		return mediaFileUrl;
	}

	public List<Tracking> getTrackings() {
		waitForWrapper();
		
		List<Tracking> t = trackings;
		if (wrappedVASTXml != null) {
			t.addAll(wrappedVASTXml.getTrackings());
		}
		return t;
	}

	public List<String> getTrackingByType(int type) {
		waitForWrapper();
		
		Iterator<Tracking> i = this.trackings.iterator();
		List<String> urls = new ArrayList<String>();
		while (i.hasNext()) {
			Tracking t = i.next();
			if (t.getEvent() == type) {
				urls.add(t.getUrl());
			}
		}
		if (wrappedVASTXml != null) {
			urls.addAll(wrappedVASTXml.getTrackingByType(type));
		}
		return urls;
	}

	public int getSkipOffset() {
		waitForWrapper();
		
		if (skipOffset <= 0 && wrappedVASTXml != null) {
			return wrappedVASTXml.getSkipOffset();
		}
		return skipOffset;
	}

	public String getClickThroughUrl() {
		waitForWrapper();
		
		String url = this.clickThroughUrl;
		if (url == null && wrappedVASTXml != null) {
			url = wrappedVASTXml.getClickThroughUrl();
		}

		return url;
	}

	public List<String> getClickTrackingUrl() {
		waitForWrapper();
		
		List<String> urls = new ArrayList<String>();
		if (this.clickTrackingUrl != null) {
			urls.add(this.clickTrackingUrl);
		}
		if (wrappedVASTXml != null) {
			urls.addAll(wrappedVASTXml.getClickTrackingUrl());
		}

		return urls;
	}

	public synchronized boolean isReady() {
		waitForWrapper();
		
		return ready
				&& (wrappedVASTXml != null ? wrappedVASTXml.isReady()
						: !hasWrapper);
	}

	private void waitForWrapper() {
		if (!hasWrapper) {
			return;
		}
		while (true) {
			if (hasWrapper
					&& (wrappedVASTXml == null || !wrappedVASTXml.isReady())) {
				try {
					Thread.sleep(750);
				} catch (Exception e) {
					SdkLog.e(TAG, "Error wraiting for wrapper", e);
				}
				Thread.yield();
			} else {
				return;
			}
		}
	}

	public void setWrapper(VASTXmlParser vastXml) {
		this.wrappedVASTXml = vastXml;
	}

	public interface VASTWrapperListener {
		public void onVASTWrapperFound(String url);
	}

	/**
	 * 
	 * @return null if no wrapped XML is present, a reference to a wrapped VASTXmlParse if it is
	 */
	public VASTXmlParser getWrappedVASTXml() {
		return this.wrappedVASTXml;
	}

	/**
	 * 
	 * @return true if VAST XML contains wrapped VAST
	 */
	public boolean hasWrapper() {
		return hasWrapper;
	}

}
