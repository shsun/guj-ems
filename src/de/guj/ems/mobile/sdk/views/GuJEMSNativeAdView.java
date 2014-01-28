package de.guj.ems.mobile.sdk.views;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Map;

import org.ormma.view.Browser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import de.guj.ems.mobile.sdk.R;
import de.guj.ems.mobile.sdk.controllers.IAdResponseHandler;
import de.guj.ems.mobile.sdk.controllers.IOnAdEmptyListener;
import de.guj.ems.mobile.sdk.controllers.IOnAdErrorListener;
import de.guj.ems.mobile.sdk.controllers.IOnAdSuccessListener;
import de.guj.ems.mobile.sdk.controllers.adserver.AdResponseParser;
import de.guj.ems.mobile.sdk.controllers.adserver.AmobeeSettingsAdapter;
import de.guj.ems.mobile.sdk.controllers.adserver.IAdResponse;
import de.guj.ems.mobile.sdk.controllers.adserver.IAdServerSettingsAdapter;
import de.guj.ems.mobile.sdk.controllers.adserver.OptimobileAdResponse;
import de.guj.ems.mobile.sdk.controllers.backfill.OptimobileDelegator;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;

/**
 * WARNING: BETA
 * 
 * The native adview class implements an imageview to display JPEG,PNG or GIF
 * files.
 * 
 * In case of animated GIFs, the file is loaded as a movie.
 * 
 * The view behaves all like a webview but cannot handle any javascript or html
 * markup.
 * 
 * It is intended for performance improvements in table or listviews.
 * 
 * !Not indented for production use!
 * 
 * @author stein16
 * 
 */
public class GuJEMSNativeAdView extends ImageView implements IAdResponseHandler {

	private boolean testMode = false;

	private Bitmap stillImage;

	private Movie animatedGif;

	private long movieStart = 0;

	private boolean play = false;

	private Paint testPaint;

	private class DownloadImageTask extends AsyncTask<String, Void, Object> {
		private final WeakReference<ImageView> viewRef;

		public DownloadImageTask(ImageView view) {
			this.viewRef = new WeakReference<ImageView>(view);
		}

		@Override
		protected Object doInBackground(String... urls) {
			String urldisplay = urls[0];

			setTag(urldisplay);

			InputStream in = null;
			try {
				in = new java.net.URL(urldisplay).openStream();

				if (urldisplay.toLowerCase(Locale.getDefault()).endsWith("gif")) {
					byte[] raw = streamToBytes(in);
					animatedGif = Movie.decodeByteArray(raw, 0, raw.length);
				} else {
					stillImage = BitmapFactory.decodeStream(in);
				}

			} catch (Exception e) {
				SdkLog.e(TAG, e.getMessage(), e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (Exception e) {
						SdkLog.e(TAG, "Error closing image input stream.", e);
					}
				}
			}
			return stillImage != null ? stillImage : animatedGif;
		}

		private byte[] streamToBytes(InputStream is) {
			ByteArrayOutputStream os = new ByteArrayOutputStream(1024);
			byte[] buffer = new byte[1024];
			int len;
			try {
				while ((len = is.read(buffer)) >= 0) {
					os.write(buffer, 0, len);
				}
			} catch (java.io.IOException e) {
				SdkLog.e(TAG, "Error streaming image to bytes.", e);
			}
			return os.toByteArray();
		}

		@Override
		protected void onPostExecute(Object result) {
			Movie movie = null;
			Bitmap bitmap = null;
			if (result != null) {

				if (Movie.class.equals(result.getClass())) {
					movie = (Movie) result;
					play = true;
					SdkLog.d(TAG, "Animation downloaded. [" + movie.width()
							+ "x" + movie.height() + ", " + movie.duration()
							+ "s]");
				} else {
					bitmap = (Bitmap) result;
					SdkLog.d(TAG, "Image downloaded. [" + bitmap.getWidth()
							+ "x" + bitmap.getHeight() + "]");
				}

				ImageView view = viewRef.get();
				if (view != null) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
							&& movie != null) {
						disableHWAcceleration();
					} else if (bitmap != null
							&& view.getTag().equals(parser.getImageUrl())) {
						Drawable d = view.getDrawable();
						if (d instanceof BitmapDrawable) {
							BitmapDrawable bd = (BitmapDrawable) d;
							Bitmap bm = bd.getBitmap();
							if (bm != null) {
								bm.recycle();
							}
							SdkLog.i(TAG,
									"Recycled bitmap of view " + view.getId());
						}
						view.setImageBitmap(bitmap);
					}

					view.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							if (parser != null && parser.getClickUrl() != null) {
								Intent i = new Intent(getContext(),
										Browser.class);
								SdkLog.d(TAG, "open:" + parser.getClickUrl());
								i.putExtra(Browser.URL_EXTRA,
										parser.getClickUrl());
								i.putExtra(Browser.SHOW_BACK_EXTRA, true);
								i.putExtra(Browser.SHOW_FORWARD_EXTRA, true);
								i.putExtra(Browser.SHOW_REFRESH_EXTRA, true);
								getContext().startActivity(i);
							}
						}
					});

					LayoutParams lp = view.getLayoutParams();
					if (movie != null) {
						lp.height = (int) (movie.height() * SdkUtil
								.getDensity());
					} else {
						lp.height = (int) (bitmap.getHeight() * SdkUtil
								.getDensity());
					}
					view.setLayoutParams(lp);
					view.setVisibility(VISIBLE);

				}
			}

		}
	}

	private AdResponseParser parser;

	private IAdServerSettingsAdapter settings;

	private final String TAG = "GuJEMSNativeAdView";

	public GuJEMSNativeAdView(Context context) {
		super(context);
		this.preLoadInitialize(context, null);
	}

	/**
	 * Initialize view implicitly from layout
	 * 
	 * @param context
	 *            android application context
	 * @param resId
	 *            resource ID of the XML layout file to inflate from
	 */
	public GuJEMSNativeAdView(Context context, AttributeSet attrs) {
		this(context, attrs, true);
	}

	/**
	 * Initialize view implicitly from layout
	 * 
	 * @param context
	 *            android application context
	 * @param resId
	 *            resource ID of the XML layout file to inflate from
	 * @param load
	 *            if set to true, the adview loads implicitity, if false, call
	 *            load by yourself
	 */
	public GuJEMSNativeAdView(Context context, AttributeSet attrs, boolean load) {
		super(context, attrs);
		this.preLoadInitialize(context, attrs);
		if (load) {
			this.load();
		}
	}

	/**
	 * Initialize view from XML resource
	 * 
	 * @param context
	 *            android application context
	 * @param resId
	 *            resource ID of the XML layout file to inflate from
	 */
	public GuJEMSNativeAdView(Context context, int resId) {
		this(context, resId, true);

	}

	/**
	 * Initialize view from XML resource
	 * 
	 * @param context
	 *            android application context
	 * @param resId
	 *            resource ID of the XML layout file to inflate from
	 * @param load
	 *            if set to true, the adview loads implicitity, if false, call
	 *            load by yourself
	 */
	public GuJEMSNativeAdView(Context context, int resId, boolean load) {
		super(context);
		AttributeSet attrs = inflate(resId);
		this.preLoadInitialize(context, attrs);
		this.handleInflatedLayout(attrs);
		if (load) {
			this.load();
		}

	}

	/**
	 * Initialize view from XML and add any custom parameters to the request
	 * 
	 * @param context
	 *            android application context
	 * @param customParams
	 *            map of custom param names and their values
	 * @param resId
	 *            resource ID of the XML layout file to inflate from
	 */
	public GuJEMSNativeAdView(Context context, Map<String, ?> customParams,
			int resId) {
		this(context, customParams, resId, true);
	}

	/**
	 * Initialize view from XML and add any custom parameters to the request
	 * 
	 * @param context
	 *            android application context
	 * @param customParams
	 *            map of custom param names and their values
	 * @param resId
	 *            resource ID of the XML layout file to inflate from
	 * @param load
	 *            if set to true, the adview loads implicitity, if false, call
	 *            load by yourself
	 */
	public GuJEMSNativeAdView(Context context, Map<String, ?> customParams,
			int resId, boolean load) {
		super(context);
		AttributeSet attrs = inflate(resId);
		this.preLoadInitialize(context, attrs);
		this.settings.addCustomParams(customParams);
		this.handleInflatedLayout(attrs);
		if (load) {
			this.load();
		}
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
	public GuJEMSNativeAdView(Context context, Map<String, ?> customParams,
			String[] kws, String nkws[], int resId) {
		this(context, customParams, kws, nkws, resId, true);
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
	 * @param load
	 *            if set to true, the adview loads implicitity, if false, call
	 *            load by yourself
	 */
	public GuJEMSNativeAdView(Context context, Map<String, ?> customParams,
			String[] kws, String nkws[], int resId, boolean load) {
		super(context);
		AttributeSet attrs = inflate(resId);
		this.preLoadInitialize(context, attrs, kws, nkws);
		this.settings.addCustomParams(customParams);
		this.handleInflatedLayout(attrs);
		if (load) {
			this.load();
		}
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
	public GuJEMSNativeAdView(Context context, String[] kws, String nkws[],
			int resId) {
		this(context, kws, nkws, resId, true);
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
	 * @param load
	 *            if set to true, the adview loads implicitity, if false, call
	 *            load by yourself
	 */
	public GuJEMSNativeAdView(Context context, String[] kws, String nkws[],
			int resId, boolean load) {
		super(context);
		AttributeSet attrs = inflate(resId);
		this.preLoadInitialize(context, attrs, kws, nkws);
		this.handleInflatedLayout(attrs);
		if (load) {
			this.load();
		}
	}

	protected ViewGroup.LayoutParams getNewLayoutParams(int w, int h) {
		return new ViewGroup.LayoutParams(w, h);
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
		if (!testMode) {
			if (getLayoutParams() != null) {
				getLayoutParams().width = w;
				getLayoutParams().height = h;
			} else {
				setLayoutParams(getNewLayoutParams(w, h));
			}
		}
		if (bk != null) {
			setBackgroundColor(Color.parseColor(bk));
		}
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
						"de.guj.ems.mobile.sdk.views.GuJEMSNativeAdView")
						|| parser
								.getName()
								.equals("de.guj.ems.mobile.sdk.views.GuJEMSNativeListAdView")) {
					as = Xml.asAttributeSet(parser);
					break;
				} else {
					SdkLog.d(TAG, parser.getName());
				}
			}
		} while (state != XmlPullParser.END_DOCUMENT);

		return as;
	}

	/**
	 * Perform the actual request. Should only be invoked if a constructor with
	 * the boolean load flag was used and it was false
	 */
	public final void load() {

		if (settings != null && !testMode && !isInEditMode()) {

			// Construct request URL
			final String url = this.settings.getRequestUrl();
			if (SdkUtil.isOnline()) {

				SdkLog.i(TAG, "START async. AdServer request [" + this.getId()
						+ "]");
				SdkUtil.adRequest(this).execute(
						new String[] { url });
			}
			// Do nothing if offline
			else {
				SdkLog.i(TAG, "No network connection - not requesting ads.");
				setVisibility(GONE);
				processError("No network connection.");
			}
		} else if (testMode || isInEditMode()) {

			if (!isInEditMode()) {
				SdkLog.w(TAG, "AdView is in test mode");
				setLayoutParams(getNewLayoutParams(
						(int) (300.0 * SdkUtil.getDensity()),
						(int) (50.0 * SdkUtil.getDensity())));
			}
			setVisibility(VISIBLE);
			if (this.settings != null
					&& this.settings.getOnAdSuccessListener() != null) {
				this.settings.getOnAdSuccessListener().onAdSuccess();
			}
		} else {
			SdkLog.w(TAG, "AdView has no settings.");
		}
	}

	private void preLoadInitialize(Context context, AttributeSet set) {
		setImageDrawable(null);
		testMode = getResources().getBoolean(R.bool.ems_test_mode);
		if (set != null && !isInEditMode()) {
			this.settings = new AmobeeSettingsAdapter(context, getClass(), set);
		} else {
			SdkLog.w(TAG,
					"No attribute set found from resource id (ok with interstitials).");
		}

	}

	private void preLoadInitialize(Context context, AttributeSet set,
			String[] kws, String[] nkws) {
		setImageDrawable(null);
		testMode = getResources().getBoolean(R.bool.ems_test_mode);
		if (set != null && !isInEditMode()) {
			this.settings = new AmobeeSettingsAdapter(context, getClass(), set,
					kws, nkws);
		} else {
			SdkLog.w(TAG,
					"No attribute set found from resource id (ok with interstitials).");
		}

	}

	@Override
	public void processError(String msg) {
		if (this.settings.getOnAdErrorListener() != null) {
			this.settings.getOnAdErrorListener().onAdError(msg);
		} else {
			SdkLog.e(TAG, msg);
		}
	}

	@Override
	public void processError(String msg, Throwable t) {
		if (this.settings.getOnAdErrorListener() != null) {
			this.settings.getOnAdErrorListener().onAdError(msg, t);
		} else {
			SdkLog.e(TAG, msg, t);
		}
	}

	@Override
	public final void processResponse(IAdResponse response) {
		try {
			if (response != null && !response.isEmpty()) {
				SdkLog.d(TAG, "Native view handling response of type "
						+ response.getClass());
				parser = response.getParser();
				new DownloadImageTask(this).execute(parser.getImageUrl());
				if (parser.getTrackingImageUrl() != null) {
					SdkUtil.adRequest(null).execute(
							parser.getTrackingImageUrl());
				}
				SdkLog.i(TAG, "Ad found and loading... [" + this.getId() + "]");
				if (this.settings.getOnAdSuccessListener() != null) {
					this.settings.getOnAdSuccessListener().onAdSuccess();
				}
			} else {
				// TODO setVisibility here?
				// setVisibility(GONE);
				if (this.settings.getDirectBackfill() != null
						&& response != null
						&& !OptimobileAdResponse.class.equals(response
								.getClass())) {
					try {
						SdkLog.i(TAG, "Passing to optimobile delegator. ["
								+ this.getId() + "]");
						new OptimobileDelegator(getContext(), this, settings);
					} catch (final Exception e) {
						if (this.settings.getOnAdErrorListener() != null) {
							getHandler().post(new Runnable() {

								@Override
								public void run() {
									settings.getOnAdErrorListener()
											.onAdError(
													"Error delegating to optimobile",
													e);
								}
							});
						} else {
							SdkLog.e(TAG, "Error delegating to optimobile", e);
						}
					}
				} else if (response == null || response.isEmpty()) {
					if (this.settings.getOnAdEmptyListener() != null) {
						getHandler().post(new Runnable() {

							@Override
							public void run() {
								settings.getOnAdEmptyListener().onAdEmpty();
							}
						});

					} else {
						SdkLog.i(TAG, "No valid ad found. [" + this.getId()
								+ "]");
					}
				}
			}
			SdkLog.i(TAG, "FINISH async. AdServer request [" + this.getId()
					+ "]");
		} catch (Exception e) {
			processError("Error loading ad [" + this.getId() + "]", e);
		}
	}

	/**
	 * Repopulate the adview after a new adserver request 
	 */
	public void reload() {
		if (settings != null && !this.testMode) {
			setVisibility(View.GONE);
			setImageDrawable(null);
			load();

		} else {
			SdkLog.w(
					TAG,
					"AdView has no settings or is in test mode. ["
							+ this.getId() + "]");
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
		SdkLog.d(TAG, "Added onSuccessListener " + l);
		this.settings.setOnAdSuccessListener(l);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		setImageBitmap(null);
		play = false;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (testMode) {
			if (testPaint == null) {
				testPaint = new Paint();
				testPaint.setColor(Color.WHITE);
				testPaint.setStyle(Style.FILL);
			}
			try {
				float dens = SdkUtil.getDensity();
				canvas.scale(dens, dens);
			} catch (Exception e) {
				; // editor mode
			}
			if (settings != null) {
				String txt = settings.toString();
				if (txt.indexOf("uid") >= 0) {
					canvas.drawText(
							txt.substring(0, txt.substring(1).indexOf("&")),
							6.0f, 12.0f, testPaint);
					canvas.drawText(
							txt.substring(txt.substring(1).indexOf("&") + 1),
							6.0f, 24.0f, testPaint);
				} else {
					canvas.drawText(settings.toString(), 6.0f, 12.0f, testPaint);
				}
			} else {
				canvas.drawText("Native AdView Editor Mode", 6.0f, 12.0f,
						testPaint);
			}
			this.invalidate();
		} else if (animatedGif != null && play) {
			long now = android.os.SystemClock.uptimeMillis();
			float dens = SdkUtil.getDensity();

			canvas.drawColor(Color.TRANSPARENT);
			canvas.scale(dens, dens);

			if (movieStart == 0) {
				movieStart = now;
			}

			if (animatedGif.duration() > 0) {
				int relTime = (int) ((now - movieStart) % animatedGif
						.duration());
				animatedGif.setTime(relTime);
			}
			animatedGif.draw(canvas,
					(getWidth() / dens - animatedGif.width()) / 2.0f, 0.0f);
			this.invalidate();
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void disableHWAcceleration() {
		setLayerType(View.LAYER_TYPE_SOFTWARE, null);
		SdkLog.d(TAG,
				"HW Acceleration disabled for AdView (younger than Gingerbread).");
	}

}
