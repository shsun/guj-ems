package de.guj.ems.mobile.sdk.controllers.backfill;

import android.content.Context;

/**
 * Every adapter to an additional SDK which is used for backfilling must
 * implement this interface.
 * 
 * An adapter is chose be the BackfillDelegator class depending on the main
 * adserver's response.
 * 
 * The execute method is then called on the adapter
 * 
 * @see de.guj.ems.mobile.sdk.controllers.backfill.controllers.BackfillDelegator
 * @see de.guj.ems.mobile.sdk.controllers.backfill.controllers.BackfillDelegator.BackfillCallback
 * @author stein16
 * 
 */
public interface BackfillAdapter {

	/**
	 * The execute method of the adapter is called by the BackfillDelegator
	 * class depending on the main adserver's response.
	 * 
	 * @param context
	 *            Android application context
	 * @param callback
	 *            Callback implementation handling events such as errors or
	 *            impressions
	 * @param data
	 *            the original response by the main adserver
	 * @param userAgent
	 *            the backfill request's user agent
	 */
	void execute(Context context,
			final BackfillDelegator.BackfillCallback callback,
			BackfillDelegator.BackfillData bfData);

}
