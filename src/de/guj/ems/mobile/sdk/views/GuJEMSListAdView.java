package de.guj.ems.mobile.sdk.views;

import java.util.Map;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.AbsListView;

/**
 * Alternative class for webview based adviews within lists
 * 
 * List view elements must have layout params of type AbsListView.LayoutParams
 * which this class provides.
 * 
 * @author stein16
 * 
 */
public class GuJEMSListAdView extends GuJEMSAdView {

	public GuJEMSListAdView(Context context) {
		super(context);
	}

	public GuJEMSListAdView(Context context, AttributeSet set) {
		super(context, set);
	}

	public GuJEMSListAdView(Context context, int resId) {
		super(context, resId);
	}

	public GuJEMSListAdView(Context context, Map<String, ?> customParams,
			int resId) {
		super(context, customParams, resId);
	}

	public GuJEMSListAdView(Context context, Map<String, ?> customParams,
			String[] kws, String[] nkws, int resId) {
		super(context, customParams, kws, nkws, resId);
	}

	public GuJEMSListAdView(Context context, String[] kws, String[] nkws,
			int resId) {
		super(context, kws, nkws, resId);
	}

	public GuJEMSListAdView(Context context, AttributeSet set, boolean load) {
		super(context, set, load);
	}

	public GuJEMSListAdView(Context context, int resId, boolean load) {
		super(context, resId, load);
	}

	public GuJEMSListAdView(Context context, Map<String, ?> customParams,
			int resId, boolean load) {
		super(context, customParams, resId, load);
	}

	public GuJEMSListAdView(Context context, Map<String, ?> customParams,
			String[] kws, String[] nkws, int resId, boolean load) {
		super(context, customParams, kws, nkws, resId, load);
	}

	public GuJEMSListAdView(Context context, String[] kws, String[] nkws,
			int resId, boolean load) {
		super(context, kws, nkws, resId, load);
	}

	@Override
	public ViewGroup.LayoutParams getNewLayoutParams(int w, int h) {
		// SdkLog.i(TAG, getParent().getClass() + " is the parent view class");
		return new AbsListView.LayoutParams(w, 1);
	}

}
