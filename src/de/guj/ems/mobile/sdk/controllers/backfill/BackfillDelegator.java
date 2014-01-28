package de.guj.ems.mobile.sdk.controllers.backfill;

import java.io.Serializable;

import android.content.Context;
import android.os.Build;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;

/**
 * The BackfillDelegator handles adserver responses which contain a backfill ad.
 * A backfill ad contains information on how to delegate the request to an
 * additional SDK such as Smartstream or AdMob.
 * 
 * The response consists of an opening tag naming the backfill partner, any data
 * which will be parsed by an adapter for the backfill partner and a closing
 * tag.
 * 
 * As of this version there is only Smartstream configured as a known partner
 * 
 * @author stein16
 * 
 */
public class BackfillDelegator {

	/**
	 * ID of backfill partner Smartstream for Video Starters and Interstitials
	 */
	public final static int SMARTSTREAM_ID = 0;

	private final static String TAG = "BackfillDelegator";

	private final static String[] BACK_FILL_OPEN = { "<smartstream>" };

	private final static String[] BACK_FILL_CLOSE = { "</smartstream>" };

	private final static BackfillAdapter[] adapters = { new SmartstreamAdapter() };

	/**
	 * This exception is thrown in case something went wrong with the backfill
	 * 
	 * @author stein16
	 * 
	 */
	public static class BackfillException extends Exception {

		private static final long serialVersionUID = -8483291797322099567L;

		public BackfillException(String msg) {
			super(msg);
		}

	}

	/**
	 * This class contains data for the backfill adapter
	 * 
	 * @author stein16
	 * 
	 */
	public static class BackfillData implements Serializable {

		private static final long serialVersionUID = 9154813314077555112L;

		String data;

		String siteId;

		String zoneId;

		String userAgent;

		int id;

		/**
		 * Default constructor
		 * 
		 * @param zone
		 *            id of the ad placement
		 * @param data
		 *            data retrieved from the main adserver
		 * @param id
		 *            id of the backfill partner (0 = Smartstream)
		 */
		public BackfillData(String zone, String data, int id) {
			this.data = data;
			this.userAgent = SdkUtil.getUserAgent();
			this.zoneId = zone;
			this.id = id;
		}

		/**
		 * Custom constructor
		 * 
		 * @param site
		 *            id of the app/site
		 * @param zone
		 *            id of the ad placement
		 * @param data
		 *            data retrieved from the main adserver
		 * @param id
		 *            id of the backfill partner (0 = Smartstream)
		 */
		public BackfillData(String site, String zone, String data, int id) {
			this.data = data;
			this.userAgent = SdkUtil.getUserAgent();
			this.siteId = site;
			this.zoneId = zone;
			this.id = id;
		}

		/**
		 * Get the data returned from the main adserver
		 * 
		 * @return string containing data to pass to the backfill adapter
		 */
		public String getData() {
			return this.data;
		}

		/**
		 * Get the ID identified by the backfill partner in the main adserver
		 * response
		 * 
		 * @return
		 */
		public int getId() {
			return this.id;
		}

		/**
		 * Get the request's user-agent
		 * 
		 * @return the request's user-agent
		 */
		public String getUserAgent() {
			return this.userAgent;
		}

		/**
		 * Get the request's original adspace ID
		 * 
		 * @return the request's original adspace ID
		 */
		public String getZoneId() {
			return this.zoneId;
		}

		/**
		 * Get the request's original site ID
		 * 
		 * @return the request's original site ID
		 */
		public String getSiteId() {
			return this.siteId;
		}

	}

	/**
	 * The callback is used upon processing a backfill request
	 * 
	 * Callbacks must be implemented by classing using the BackfillDelegator to
	 * handle events in the views or activities of an Android App
	 * 
	 * @see de.guj.ems.mobile.sdk.activities.InterstitialSwitchActivity
	 * @author stein16
	 * 
	 */
	public static interface BackfillCallback {

		public void receivedAdCallback();

		/**
		 * Called when any event with content @arg0 has been triggered
		 * 
		 * @param arg0
		 */
		public void trackEventCallback(String arg0);

		/**
		 * Called when an exception occured during backfill processing
		 * 
		 * @param e
		 *            the exception
		 */
		public void adFailedCallback(Exception e);

		/**
		 * Called when no ad was returned by the backfill partner
		 */
		public void noAdCallback();

		/**
		 * Called when the ad from the backfill partner has finished (e.g. a
		 * movie)
		 */
		public void finishedCallback();

	}

	/**
	 * Parses the main adserver response for backfill information and returns an
	 * appropriate BackfillData object if something was actually found
	 * 
	 * @param data
	 *            the main adserver response body
	 * @return null if no backfill was detected, a valid BackfillData object
	 *         otherwise
	 * @de.guj.ems.mobile.sdk.controllers.BackfillDelegator.BackfillData
	 */
	public final static BackfillData isBackfill(String adSpace, String data) {

		// TODO remove when Smartstream is fixed for 4.4
		if (Build.VERSION.SDK_INT >= 19) {
			return null;
		}
		if (data == null || data.startsWith("<div") || data.startsWith("<!DOC")
				|| data.startsWith("<html") || data.startsWith("<VAST")
				|| data.startsWith("<!-- <connectad>")) {
			return null;
		}
		SdkLog.d(TAG, "Checking string " + data + " for backfill...");
		for (int i = 0; i < BACK_FILL_OPEN.length; i++) {
			if (data.startsWith(BACK_FILL_OPEN[i])) {
				return new BackfillData(adSpace, data.substring(
						BACK_FILL_OPEN[i].length(),
						data.indexOf(BACK_FILL_CLOSE[i])), i);
			}
		}

		return null;

	}

	/**
	 * Find the appropriate adapter for a backfill partner and delegates the
	 * data received from the main adserver to it. Additionally callbacks are
	 * triggered upon any events happing during the delegation
	 * 
	 * @param context
	 *            Android application context
	 * @param bfData
	 *            valid BackfillData object with backfill partner ID and data
	 *            received from the main adserver
	 * @param callback
	 *            class implementing callback methods so that an application can
	 *            react to backfill events
	 * @throws BackfillException
	 *             if any error occurs during triggering of the backfill adapter
	 */
	public final static void process(Context context, BackfillData bfData,
			BackfillCallback callback) throws BackfillException {
		if (callback != null && bfData != null
				&& bfData.getId() < adapters.length
				&& adapters[bfData.getId()] != null) {
			adapters[bfData.getId()].execute(context, callback, bfData);
		} else {
			if (callback == null) {
				throw new BackfillException(
						"BackfillDelegator.process was called with callback == null");
			}
			if (bfData == null) {
				throw new BackfillException(
						"BackfillDelegator.process was called with data == null");
			}
			if (bfData.getId() >= adapters.length
					|| adapters[bfData.getId()] != null) {
				throw new BackfillException("No Adapter found for id="
						+ bfData.getId() + ", data=" + bfData.getData());
			}
		}
	}

}
