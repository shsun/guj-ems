package de.guj.ems.mobile.sdk.controllers;

import de.guj.ems.mobile.sdk.R;
import de.guj.ems.mobile.sdk.views.GuJEMSAdView;
import de.guj.ems.mobile.sdk.views.GuJEMSListAdView;

public class AdViewConfiguration {

	public static interface IViewConfiguration {
		
		public int getErrorListenerId();

		public int getEmptyListenerId();

		public int getGeoId();

		public int getKeywordsId();

		public int getLatId();

		public int getLonId();

		public int getNKeywordsId();

		public int getSiteIdId();

		public int getBackfillSiteIdId();

		public int getSuccessListenerId();

		public int getUuidId();

		public int getZoneIdId();

		public int getBackfillZoneIdId();
	};

	private static class WebViewConfiguration implements IViewConfiguration {

		@Override
		public int getErrorListenerId() {
			return R.styleable.GuJEMSAdView_ems_onAdError;
		}

		@Override
		public int getEmptyListenerId() {
			return R.styleable.GuJEMSAdView_ems_onAdEmpty;
		}

		@Override
		public int getGeoId() {
			return R.styleable.GuJEMSAdView_ems_geo;
		}

		@Override
		public int getKeywordsId() {
			return R.styleable.GuJEMSAdView_ems_kw;
		}

		@Override
		public int getLatId() {
			return R.styleable.GuJEMSAdView_ems_lat;
		}

		@Override
		public int getLonId() {
			return R.styleable.GuJEMSAdView_ems_lon;
		}

		@Override
		public int getNKeywordsId() {
			return R.styleable.GuJEMSAdView_ems_nkw;
		}

		@Override
		public int getSiteIdId() {
			return R.styleable.GuJEMSAdView_ems_siteId;
		}

		@Override
		public int getBackfillSiteIdId() {
			return R.styleable.GuJEMSAdView_ems_bfSiteId;
		}

		@Override
		public int getSuccessListenerId() {
			return R.styleable.GuJEMSAdView_ems_onAdSuccess;
		}

		@Override
		public int getUuidId() {
			return R.styleable.GuJEMSAdView_ems_uid;
		}

		@Override
		public int getZoneIdId() {
			return R.styleable.GuJEMSAdView_ems_zoneId;
		}

		@Override
		public int getBackfillZoneIdId() {
			return R.styleable.GuJEMSAdView_ems_bfZoneId;
		}
	}

	private static class NativeViewConfiguration implements IViewConfiguration {

		@Override
		public int getErrorListenerId() {
			return R.styleable.GuJEMSNativeAdView_ems_onAdError;
		}

		@Override
		public int getEmptyListenerId() {
			return R.styleable.GuJEMSNativeAdView_ems_onAdEmpty;
		}

		@Override
		public int getGeoId() {
			return R.styleable.GuJEMSNativeAdView_ems_geo;
		}

		@Override
		public int getKeywordsId() {
			return R.styleable.GuJEMSNativeAdView_ems_kw;
		}

		@Override
		public int getLatId() {
			return R.styleable.GuJEMSNativeAdView_ems_lat;
		}

		@Override
		public int getLonId() {
			return R.styleable.GuJEMSNativeAdView_ems_lon;
		}

		@Override
		public int getNKeywordsId() {
			return R.styleable.GuJEMSNativeAdView_ems_nkw;
		}

		@Override
		public int getSiteIdId() {
			return R.styleable.GuJEMSNativeAdView_ems_siteId;
		}

		@Override
		public int getBackfillSiteIdId() {
			return R.styleable.GuJEMSNativeAdView_ems_bfSiteId;
		}

		@Override
		public int getSuccessListenerId() {
			return R.styleable.GuJEMSNativeAdView_ems_onAdSuccess;
		}

		@Override
		public int getUuidId() {
			return R.styleable.GuJEMSNativeAdView_ems_uid;
		}

		@Override
		public int getZoneIdId() {
			return R.styleable.GuJEMSNativeAdView_ems_zoneId;
		}

		@Override
		public int getBackfillZoneIdId() {
			return R.styleable.GuJEMSNativeAdView_ems_bfZoneId;
		}
	}

	private final static WebViewConfiguration WEBVIEWCONFIG = new WebViewConfiguration();

	private final static NativeViewConfiguration NATIVEVIEWCONFIG = new NativeViewConfiguration();

	public final static IViewConfiguration getConfig(Class<?> viewClass) {
		if (viewClass.equals(GuJEMSAdView.class)
				|| viewClass.equals(GuJEMSListAdView.class)) {
			return WEBVIEWCONFIG;
		}
		return NATIVEVIEWCONFIG;
	}

}
