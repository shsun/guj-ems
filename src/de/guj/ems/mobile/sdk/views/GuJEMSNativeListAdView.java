package de.guj.ems.mobile.sdk.views;

import java.util.Map;

import de.guj.ems.mobile.sdk.controllers.IAdResponseHandler;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.AbsListView;

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
public class GuJEMSNativeListAdView extends GuJEMSNativeAdView implements
		IAdResponseHandler {

	public GuJEMSNativeListAdView(Context context) {
		super(context);
	}

	public GuJEMSNativeListAdView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public GuJEMSNativeListAdView(Context context, int resId) {
		super(context, resId);
	}

	public GuJEMSNativeListAdView(Context context, Map<String, ?> customParams,
			int resId) {
		super(context, customParams, resId);
	}

	public GuJEMSNativeListAdView(Context context, Map<String, ?> customParams,
			String[] kws, String[] nkws, int resId) {
		super(context, customParams, kws, nkws, resId);
	}

	public GuJEMSNativeListAdView(Context context, String[] kws, String[] nkws,
			int resId) {
		super(context, kws, nkws, resId);
	}
	
	public GuJEMSNativeListAdView(Context context, AttributeSet attrs,
			boolean load) {
		super(context, attrs, load);
	}

	public GuJEMSNativeListAdView(Context context, int resId, boolean load) {
		super(context, resId, load);
	}

	public GuJEMSNativeListAdView(Context context, Map<String, ?> customParams,
			int resId, boolean load) {
		super(context, customParams, resId, load);
	}

	public GuJEMSNativeListAdView(Context context, Map<String, ?> customParams,
			String[] kws, String[] nkws, int resId, boolean load) {
		super(context, customParams, kws, nkws, resId, load);
	}

	public GuJEMSNativeListAdView(Context context, String[] kws, String[] nkws,
			int resId, boolean load) {
		super(context, kws, nkws, resId, load);
	}

	@Override
	protected ViewGroup.LayoutParams getNewLayoutParams(int w, int h) {
		// SdkLog.i(TAG, getParent().getClass() + " is the parent view class");
		return new AbsListView.LayoutParams(w, h);
	}

	@Override
	public void reload() {
		super.reload();
	}

}
