package de.guj.ems.mobile.sdk.util;

import java.text.DecimalFormat;

import de.guj.ems.mobile.sdk.R;

public class SdkGlobals {

	/**
	 * Format for geo position formatting
	 */
	public final static DecimalFormat TWO_DIGITS_DECIMAL = new DecimalFormat(
			"#.##");

	/**
	 * xml layout attributes prefix
	 */
	public final static String EMS_ATTRIBUTE_PREFIX = SdkUtil.getContext()
			.getString(R.string.attributePrefix);

	/**
	 * xml layout attributes prefix for listeners
	 */
	public final static String EMS_LISTENER_PREFIX = EMS_ATTRIBUTE_PREFIX
			+ "onAd";

	/**
	 * Global attribute name for listener which reacts to empty ad
	 */
	public final static String EMS_ERROR_LISTENER = SdkUtil.getContext()
			.getString(R.string.onAdError);

	/**
	 * Global attribute name for listener which reacts to empty ad
	 */
	public final static String EMS_EMPTY_LISTENER = SdkUtil.getContext()
			.getString(R.string.onAdEmpty);

	/**
	 * Global attribute name for allowing geo localization for a placement
	 */
	public final static String EMS_GEO = SdkUtil.getContext().getString(
			R.string.geo);

	/**
	 * Global attribute name for identifying keywords add to the request
	 */
	public final static String EMS_KEYWORDS = SdkUtil.getContext().getString(
			R.string.keyword);

	/**
	 * Global attribute name for the geographical latitude
	 */
	public final static String EMS_LAT = SdkUtil.getContext().getString(
			R.string.latitude);
	/**
	 * Global attribute name for the geographical longitude
	 */
	public final static String EMS_LON = SdkUtil.getContext().getString(
			R.string.longitude);

	/**
	 * Global attribute name for identifying non-keywords to the request
	 */
	public final static String EMS_NKEYWORDS = SdkUtil.getContext().getString(
			R.string.nkeyword);

	/**
	 * Global attribute name for identifying a site
	 */
	public final static String EMS_SITEID = SdkUtil.getContext().getString(
			R.string.siteId);

	/**
	 * Global attribute name for identifying a site for backfill
	 */
	public final static String EMS_BACKFILL_SITEID = SdkUtil.getContext()
			.getString(R.string.backfillSiteId);

	/**
	 * xml layout attribute for success listener
	 */
	public final static String EMS_SUCCESS_LISTENER = SdkUtil.getContext()
			.getString(R.string.onAdSuccess);

	/**
	 * Global attribute name for identifying a unique user/device id
	 */
	public final static String EMS_UUID = SdkUtil.getContext().getString(
			R.string.deviceId);

	/**
	 * Global attribute name for identifying a placement
	 */
	public final static String EMS_ZONEID = SdkUtil.getContext().getString(
			R.string.zoneId);

	/**
	 * Global attribute name for identifying a placement for backfill
	 */
	public final static String EMS_BACKFILL_ZONEID = SdkUtil.getContext()
			.getString(R.string.backfillZoneId);

	/**
	 * name of http security header
	 */
	public final static String EMS_SECURITY_HEADER_NAME = SdkUtil.getContext()
			.getString(R.string.securityHeaderName);

}
