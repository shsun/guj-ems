package de.guj.ems.mobile.sdk.views;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import org.ormma.view.OrmmaView;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import de.guj.ems.mobile.sdk.controllers.AdServerAccess;
import de.guj.ems.mobile.sdk.controllers.AmobeeSettingsAdapter;
import de.guj.ems.mobile.sdk.controllers.EMSInterface;
import de.guj.ems.mobile.sdk.controllers.IAdServerSettingsAdapter;
import de.guj.ems.mobile.sdk.controllers.IOnAdEmptyListener;
import de.guj.ems.mobile.sdk.controllers.IOnAdErrorListener;
import de.guj.ems.mobile.sdk.controllers.IOnAdSuccessListener;
import de.guj.ems.mobile.sdk.controllers.OptimobileDelegator;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;

/**
 * The webview uses as container to display an ad. Derived from the ORMMA
 * reference implementaton of an ad view container.
 * 
 * This class adds folowing capabilites to the reference implementation: -
 * loading data with an asynchronous HTTP request - initializing the view from
 * XML by passing a resource ID - adding custom view-specific parameters to a
 * placement's ad request (runtime) - adding matching or non-matching keywords
 * to a placement's ad request (runtime) - adding the javascript interface
 * EMSInterface to the view
 * 
 * ONLY USE THIS CLASS IF YOU WANT TO ADD THE VIEW PROGRAMMATICALLY INSTEAD OF
 * DEFINING IT WITHIN A LAYOUT.XML FILE!
 * 
 * @author stein16
 * 
 */
public class GuJEMSAdView extends OrmmaView implements AdResponseHandler {

	private Handler handler = new Handler();

	private IAdServerSettingsAdapter settings;

	private final String TAG = "GuJEMSAdView";

	/**
	 * Initialize view without configuration
	 * 
	 * @param context
	 *            android application context
	 */
	public GuJEMSAdView(Context context) {
		super(context);
		this.preLoadInitialize(context, null);
	}

	/**
	 * Initialize view with attribute set (this is the common constructor)
	 * 
	 * @param context
	 *            android application context
	 * @param resId
	 *            resource ID of the XML layout file to inflate from
	 */
	public GuJEMSAdView(Context context, AttributeSet set) {
		super(context, set);
		this.preLoadInitialize(context, set);
		this.load();
	}

	/**
	 * Initialize view from XML
	 * 
	 * @param context
	 *            android application context
	 * @param resId
	 *            resource ID of the XML layout file to inflate from
	 */
	public GuJEMSAdView(Context context, int resId) {
		super(context);
		AttributeSet attrs = inflate(resId);
		this.preLoadInitialize(context, attrs);
		this.handleInflatedLayout(attrs);
		this.load();
	}

	/**
	 * Initialize view from XML and add any custom parameters to the request
	 * 
	 * @param context
	 *            android application context
	 * @param customParams
	 *            map of custom param names and thiur values
	 * @param resId
	 *            resource ID of the XML layout file to inflate from
	 */
	public GuJEMSAdView(Context context, Map<String, ?> customParams, int resId) {
		super(context);
		AttributeSet attrs = inflate(resId);
		this.preLoadInitialize(context, attrs);
		this.addCustomParams(customParams);
		this.handleInflatedLayout(attrs);
		this.load();
	}

	/**
	 * Initialize view from XML and add matching or non-matching keywords as
	 * well as any custom parameters to the request
	 * 
	 * @param context
	 *            android application context
	 * @param customParams
	 *            map of custom param names and their values
	 * @param kws
	 *            matching keywords
	 * @param nkws
	 *            non-matching keywords
	 * @param resId
	 *            resource ID of the XML layout file to inflate from
	 */
	public GuJEMSAdView(Context context, Map<String, ?> customParams,
			String[] kws, String nkws[], int resId) {
		super(context);
		AttributeSet attrs = inflate(resId);
		this.preLoadInitialize(context, attrs, kws, nkws);
		this.addCustomParams(customParams);
		this.handleInflatedLayout(attrs);
		this.load();
	}

	/**
	 * Initialize view from XML and add matching or non-matching keywords
	 * 
	 * @param context
	 *            android application context
	 * @param kws
	 *            matching keywords
	 * @param nkws
	 *            non-matching keywords
	 * @param resId
	 *            resource ID of the XML layout file to inflate from
	 */
	public GuJEMSAdView(Context context, String[] kws, String nkws[], int resId) {
		super(context);
		AttributeSet attrs = inflate(resId);
		this.preLoadInitialize(context, attrs, kws, nkws);
		this.handleInflatedLayout(attrs);
		this.load();
	}

	private void handleInflatedLayout(AttributeSet attrs) {
		int w = attrs.getAttributeIntValue(
				"http://schemas.android.com/apk/res/android", "layout_width",
				ViewGroup.LayoutParams.MATCH_PARENT);
		int h = attrs.getAttributeIntValue(
				"http://schemas.android.com/apk/res/android", "layout_height",
				ViewGroup.LayoutParams.WRAP_CONTENT);
		String bk = attrs.getAttributeValue(
				"http://schemas.android.com/apk/res/android", "background");
		if (getLayoutParams() != null) {
			getLayoutParams().width = w;
			getLayoutParams().height = h;
		} else {
			setLayoutParams(getNewLayoutParams(w, h));
		}

		if (bk != null) {
			setBackgroundColor(Color.parseColor(bk));
		}
	}

	protected ViewGroup.LayoutParams getNewLayoutParams(int w, int h) {
		return new ViewGroup.LayoutParams(w, h);
	}

	private void addCustomParams(Map<String, ?> params) {
		if (params != null) {
			Iterator<String> mi = params.keySet().iterator();
			while (mi.hasNext()) {
				String param = mi.next();
				Object value = params.get(param);
				if (value.getClass().equals(String.class)) {
					this.settings.addCustomRequestParameter(param,
							(String) value);
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
		} else {
			SdkLog.w(TAG, "Custom params constructor used with null-array.");
		}
	}

	public Handler getHandler() {
		return handler;
	}

	private AttributeSet inflate(int resId) {
		AttributeSet as = null;
		Resources r = getResources();
		XmlResourceParser parser = r.getLayout(resId);

		int state = 0;
		do {
			try {
				state = parser.next();
			} catch (XmlPullParserException e1) {
				e1.printStackTrace();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			if (state == XmlPullParser.START_TAG) {
				if (parser.getName().equals(
						"de.guj.ems.mobile.sdk.views.GuJEMSAdView") || parser.getName().equals(
								"de.guj.ems.mobile.sdk.views.GuJEMSListAdView")) {
					as = Xml.asAttributeSet(parser);
					break;
				} else {
					SdkLog.d(TAG, parser.getName());
				}
			}
		} while (state != XmlPullParser.END_DOCUMENT);

		return as;
	}

	private final void load() {

		if (settings != null) {

			// Construct request URL
			final String url = this.settings.getRequestUrl();
			if (SdkUtil.isOnline()) {

				SdkLog.i(TAG, "START async. AdServer request [" + this.getId() + "]");
				new AdServerAccess(SdkUtil.getUserAgent(), this)
						.execute(new String[] { url });
			}
			// Do nothing if offline
			else {
				SdkLog.i(TAG, "No network connection - not requesting ads.");
				setVisibility(GONE);
				processError("No network connection.");
			}
		} else {
			SdkLog.w(TAG, "AdView has no settings.");
		}
	}

	private void preLoadInitialize(Context context, AttributeSet set) {

		this.addJavascriptInterface(EMSInterface.getInstance(),
				"emsmobile");

		if (set != null && !isInEditMode()) {
			this.settings = new AmobeeSettingsAdapter(context, set);
		} else if (isInEditMode()) {
			super.loadDataWithBaseURL(
					"file:///android_asset/",
					"<!DOCTYPE html><html><head><title>G+J EMS AdView</title></head><body><img src=\"defaultad.png\"></body></html>",
					"text/html", "utf-8", null);
			setVisibility(VISIBLE);
		}
		else {
			SdkLog.e(TAG, "No attribute set found from resource id?");
		}

	}

	private void preLoadInitialize(Context context, AttributeSet set,
			String[] kws, String[] nkws) {

		this.addJavascriptInterface(EMSInterface.getInstance(),
				"emsmobile");

		if (set != null && !isInEditMode()) {
			this.settings = new AmobeeSettingsAdapter(context, set, kws, nkws);
		} else if (isInEditMode()) {
			super.loadDataWithBaseURL(
					"file:///android_asset/",
					"<!DOCTYPE html><html><head><title>G+J EMS AdView</title></head><body><img src=\"defaultad.png\"></body></html>",
					"text/html", "utf-8", null);
			setVisibility(VISIBLE);
		}

	}

	@Override
	public void processError(String msg) {
		SdkLog.w(
				TAG,
				"The following error occured and is being handled by the appropriate listener if available.");
		SdkLog.e(TAG, msg);
		if (this.settings.getOnAdErrorListener() != null) {
			this.settings.getOnAdErrorListener().onAdError(msg);
		}
	}

	@Override
	public void processError(String msg, Throwable t) {
		SdkLog.w(
				TAG,
				"The following error occured and is being handled by the appropriate listener if available.");
		if (msg != null && msg.length() > 0) {
			SdkLog.e(TAG, msg);
		} else {
			SdkLog.e(TAG, "Exception: ", t);
		}
		if (this.settings.getOnAdErrorListener() != null) {
			this.settings.getOnAdErrorListener().onAdError(msg, t);
		}
	}

	public final void processResponse(String response) {
		try {
			if (response != null && response.length() > 0) {
				setTimeoutRunnable(new TimeOutRunnable());
				loadData(response, "text/html", "utf-8");
				SdkLog.i(TAG, "Ad found and loading... [" + this.getId() + "]");
				if (this.settings.getOnAdSuccessListener() != null) {
					this.settings.getOnAdSuccessListener().onAdSuccess();
				}
			} else {
				setVisibility(GONE);
				if (this.settings.getDirectBackfill() != null) {
					try {
						SdkLog.i(TAG, "Passing to optimobile delegator. [" + this.getId() + "]");
						OptimobileDelegator optimobileDelegator = new OptimobileDelegator(
								SdkUtil.getContext(), this, settings);
						((ViewGroup) getParent())
								.addView(optimobileDelegator
										.getOptimobileView(),
										((ViewGroup) getParent())
												.indexOfChild(this) + 1);
						optimobileDelegator.getOptimobileView().update();

					} catch (Exception e) {
						if (this.settings.getOnAdErrorListener() != null) {
							this.settings.getOnAdErrorListener().onAdError(
									"Error delegating to optimobile", e);
						} else {
							SdkLog.e(TAG, "Error delegating to optimobile", e);
						}
					}
				} else {
					if (this.settings.getOnAdEmptyListener() != null) {
						this.settings.getOnAdEmptyListener().onAdEmpty();
					} else {
						SdkLog.i(TAG, "No valid ad found. [" + this.getId() + "]");
					}
				}
			}
			SdkLog.i(TAG, "FINISH async. AdServer request [" + this.getId() + "]");
		} catch (Exception e) {
			processError("Error loading ad [" + 
					 this.getId() + "]", e);
		}
	}

	@Override
	public void reload() {
		if (settings != null) {

			super.clearView();
			setVisibility(View.GONE);

			// Construct request URL
			final String url = this.settings.getRequestUrl();
			if (SdkUtil.isOnline()) {

				SdkLog.i(TAG, "START async. AdServer request [" + this.getId() + "]");
				new AdServerAccess(SdkUtil.getUserAgent(), this)
						.execute(new String[] { url });
			}
			// Do nothing if offline
			else {
				SdkLog.i(TAG, "No network connection - not requesting ads.");
				setVisibility(GONE);
				processError("No network connection.");
			}
		} else {
			SdkLog.w(TAG, "AdView has no settings. [" + this.getId() + "]");
		}
	}

	/**
	 * Add a listener to the view which responds to empty ad responses
	 * 
	 * @param l
	 *            Implemented listener
	 */
	public void setOnAdEmptyListener(IOnAdEmptyListener l) {
		this.settings.setOnAdEmptyListener(l);
	}

	/**
	 * Add a listener to the view which responds to errors while requesting ads
	 * 
	 * @param l
	 *            Implemented listener
	 */
	public void setOnAdErrorListener(IOnAdErrorListener l) {
		this.settings.setOnAdErrorListener(l);
	}

	/**
	 * Add a listener to the view which responds to successful ad requests
	 * 
	 * @param l
	 *            Implemented listener
	 */
	public void setOnAdSuccessListener(IOnAdSuccessListener l) {
		this.settings.setOnAdSuccessListener(l);
	}

}
