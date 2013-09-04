package de.guj.ems.mobile.sdk.controllers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import com.MASTAdView.MASTAdDelegate.AdDownloadEventHandler;
import com.MASTAdView.MASTAdView;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;

import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;
import de.guj.ems.mobile.sdk.views.GuJEMSAdView;

/**
 * Delegates requests to optimobile and possibly other networks
 * if a premium campaign is not available for the current adview/slot.
 * 
 * Backfill is initially configured with GuJEMSAdView by adding
 * an additional optimobile site and zone ID to the view
 * 
 * If 3rd party network backfill like admob is configured in
 * optimobile, the request is also handled here by passing
 * the metadata returned from optimobile to the admob sdk.
 * 
 * @author stein16
 *
 */
public class OptimobileDelegator {

	private final static String TAG = "OptimobileDelegator";
	
	private MASTAdView optimobileView;
	
	private GuJEMSAdView emsMobileView;
	
	private Handler handler;
	
	private Context context;
	
	/**
	 * Default constructor
	 * 
	 * Initially creates an optimobile adview which an be added to the layout.
	 * The optimobile view uses callbacks for error handling etc. and also
	 * for a possible backfill. If a 3rd party network is active, the optimobile
	 * ad view will actually be removed and replaced by the network's view.
	 * 
	 * @param context App/Activity context
	 * @param adView original (first level) adview
	 * @param settings settings of original adview 
	 */
	public OptimobileDelegator(Context context, GuJEMSAdView adView, final IAdServerSettingsAdapter settings) {
		this.context = context;
		this.emsMobileView = adView;
		this.handler = this.emsMobileView.getHandler();
		this.optimobileView = initOptimobileView(context, settings, 0); 
	}

	@SuppressWarnings("deprecation")
	private MASTAdView initOptimobileView(Context context, final IAdServerSettingsAdapter settings, int color) {
		AdMobHandler delegator = new AdMobHandler();
		final MASTAdView view = new MASTAdView(context,
				Integer.valueOf(settings
						.getDirectBackfill().getSiteId()),
				Integer.valueOf(settings
						.getDirectBackfill().getZoneId()));
		view.setLayoutParams(
				new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		view.setId(emsMobileView.getId());
		view.setBackgroundDrawable(emsMobileView.getBackground());			
		view.setLocationDetection(true);
		view.setVisibility(View.GONE);
		view.setUseInternalBrowser(true);
		view.setUpdateTime(0);
		view.getAdDelegate().setThirdPartyRequestHandler(delegator);
		view.getAdDelegate().setAdDownloadHandler(new AdDownloadEventHandler() {
			
			@Override
			public void onDownloadError(MASTAdView arg0, String arg1) {
				if (arg1 != null && arg1.startsWith("No ads")) {
					if (settings.getOnAdEmptyListener() != null) {
						settings.getOnAdEmptyListener().onAdEmpty();
					}
					else {
						SdkLog.i(TAG, "optimobile: " + arg1);
					}
				}
				else {
					if (settings.getOnAdErrorListener() != null) {
						settings.getOnAdErrorListener().onAdError("optimobile: " + arg1);
					}
					else {
						SdkLog.w(TAG, "optimobile: " + arg1);
					}
				}
				handler.post(new Runnable() {
					@Override
					public void run() {
						view.setVisibility(View.GONE);
						
					}
				});				
			}
			
			@Override
			public void onDownloadEnd(MASTAdView arg0) {
			}
			
			@Override
			public void onDownloadBegin(MASTAdView arg0) {
			}
			
			@Override
			public void onAdViewable(MASTAdView arg0) {
				if (settings.getOnAdSuccessListener() != null) {
					settings.getOnAdSuccessListener().onAdSuccess();
				}
				else {
					SdkLog.d(TAG, "optimobile Ad loaded.");
				}								
				handler.post(new Runnable() {
					@Override
					public void run() {
						view.setVisibility(View.VISIBLE);
						
					}
				});
			}
		});
		
		return view;
	}
	
	/**
	 * Get an instance of the initially created optimobile adview
	 * for backfilling
	 * @return mOcean/optimobile adview
	 */
	public MASTAdView getOptimobileView() {
		return this.optimobileView;
	}
	
	private final class AdMobHandler implements com.MASTAdView.MASTAdDelegate.ThirdPartyEventHandler {

		private String pubId;
		
		private AdRequest adRequest;
		
		protected Location getLocation() {
			LocationManager lm = (LocationManager) SdkUtil.getContext()
					.getSystemService(Context.LOCATION_SERVICE);
			List<String> providers = lm.getProviders(true);
			Iterator<String> provider = providers.iterator();
			Location lastKnown = null;
			long age = 0;
			while (provider.hasNext()) {
				lastKnown = lm.getLastKnownLocation(provider.next());
				if (lastKnown != null) {
					age = System.currentTimeMillis() - lastKnown.getTime();
					if (age <= AdServerSettingsAdapter.EMS_LOCATION_MAXAGE_MS) {
						break;
					}
					else {
						SdkLog.d(TAG, "Location [" + lastKnown.getProvider() + "] is " + (age / 60000) + " min old. [max = " + AdServerSettingsAdapter.EMS_LOCATION_MAXAGE_MIN + "]");
					}
				}
			}

			if (lastKnown != null && age <= AdServerSettingsAdapter.EMS_LOCATION_MAXAGE_MS) {
				return lastKnown;
			}

			return null;			
		}
		
		@Override
		public void onThirdPartyEvent(MASTAdView arg0, HashMap<String, String> arg1) {
			
			String type = arg1.get("type");	
			
			if (type != null && "admob".equals(type)) {
				String zip = arg1.get("zip");
				String lon = arg1.get("long");
				String lat = arg1.get("lat");
				this.pubId = arg1.get("publisherid");
				SdkLog.i(TAG, "optimobile: AdMob backfill detected. [" + zip + ", " + lat + ", " + lon + ", " + pubId + "]");
				this.adRequest = new AdRequest();
				this.adRequest.addTestDevice(AdRequest.TEST_EMULATOR);
				this.adRequest.setLocation(getLocation());
				handler.post(new Runnable() {
					
					@SuppressWarnings("deprecation")
					@Override
					public void run() {
						SdkLog.i(TAG, "Performing google admob request...");
						AdView admobAdView = new AdView((Activity)context, AdSize.BANNER, pubId);
						admobAdView.setId(emsMobileView.getId());
						admobAdView.setGravity(Gravity.CENTER_HORIZONTAL);
						admobAdView.setBackgroundDrawable(emsMobileView.getBackground());		
						((ViewGroup)emsMobileView.getParent()).removeView(optimobileView);
						((ViewGroup)emsMobileView.getParent()).addView(admobAdView,((ViewGroup)emsMobileView.getParent()).indexOfChild(emsMobileView) + 1);

						admobAdView.loadAd(adRequest);
						
					}
				});
			}
			else {
				SdkLog.w(TAG, "Unknown third party ad stream detected [" + type + "]");
			}
		}

    	
    }
	
}
