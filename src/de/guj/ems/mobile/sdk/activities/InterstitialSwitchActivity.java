package de.guj.ems.mobile.sdk.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import de.guj.ems.mobile.sdk.R;
import de.guj.ems.mobile.sdk.controllers.AdServerAccess;
import de.guj.ems.mobile.sdk.controllers.AmobeeSettingsAdapter;
import de.guj.ems.mobile.sdk.controllers.BackfillDelegator;
import de.guj.ems.mobile.sdk.controllers.IAdServerSettingsAdapter;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;
import de.guj.ems.mobile.sdk.views.AdResponseHandler;

/**
 * The IntestitialSwitchActivity determines whether an interstitial should and
 * can be shown. If an interstitial is returned by the ad server, it is shown
 * with the InterstitialActivity. If not, the original target activity is shown
 * or the interstitial activity is just finished.
 * 
 * When starting this activity, you may add the original target
 * activity's intent, otherwise this activity will just finish:
 * 
 * Intent i = new Intent(<calling activity class>,
 * InterstitialSwitchActivity.class); i.putExtra("target", <original target
 * activity's intent>); startActivity(i);
 * 
 * NEW Nov 1st 2012:
 * 
 * This version of the class also delegates the main adserver response to the
 * BackfillDelegator class and checks for backfill partners which might be
 * serving ads to display within an interstitial.
 * 
 * @author stein16
 * @deprecated use InterstitialSwitchReceiver instead and broadcast
 * 
 */
public final class InterstitialSwitchActivity extends Activity implements AdResponseHandler {

	private IAdServerSettingsAdapter settings;
	
	private String userAgentString;
	
	private String data;
	
	private Intent target;
	
	private final static String TAG = "InterstitialSwitch";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);


		// Steffen Führes, RTL Interactive
		// getUserAgent hat sonst ein Problem, wenn interstitial 1. Werbemittel
		if (SdkUtil.getContext() == null) {
			SdkUtil.setContext(getApplicationContext());
		}
		
		// determine user-agent
		this.userAgentString = SdkUtil.getUserAgent();

		// original target when interstitial not available
		this.target = (Intent) getIntent().getExtras().get("target");

		// ad space settings
		this.settings = new AmobeeSettingsAdapter(getApplicationContext(),
				getIntent().getExtras());

		
//		SdkLog.i(TAG, " ******* " + ((InterstitialIntent)getIntent()).getOnAdSuccessListener());

		// adserver request
		if (SdkUtil.isOnline()) {
			final String url = this.settings.getRequestUrl();
			SdkLog.i(TAG, "START AdServer request");
			new AdServerAccess(
					this.userAgentString, this).execute(new String[] { url });


		} else {
			SdkLog.i(TAG, "No network connection - not requesting ads.");
			processError("No network connection.");
		}

		// Done
		this.finish();

	}

	@Override
	public void processResponse(String response) {
		SdkLog.i(TAG, "FINISH AdServer request");
		BackfillDelegator.BackfillData bfD;
		this.data = response;
		if (data != null && data.length() > 1
				&& (bfD = BackfillDelegator.isBackfill(
						(String) getIntent().getExtras().get(
								getString(R.string.amobeeAdSpace)), data)) != null) {
			SdkLog.d(TAG,
					"Possible backfill ad detected [id=" + bfD.getId()
							+ ", data=" + bfD.getData() + "]");
			try {

				BackfillDelegator.process(getApplicationContext(), bfD,
						new BackfillDelegator.BackfillCallback() {
							@Override
							public void trackEventCallback(String arg0) {
								SdkLog.d(TAG,
										"Backfill: An event occured ["
												+ arg0 + "]");
							}

							@Override
							public void noAdCallback() {
								SdkLog.d(TAG, "Backfill: empty.");
								if (settings.getOnAdEmptyListener() != null) {
									settings.getOnAdEmptyListener().onAdEmpty();
								}
								if (target != null) {
									startActivity(target);
								}
								else {
									SdkLog.i(TAG,  "No target. Back to previous view.");
									finish();
								}
							}

							@Override
							public void finishedCallback() {
								if (target != null) {
									startActivity(target);
								}
								else {
									SdkLog.i(TAG,  "No target. Back to previous view.");
									finish();
								}
							}

							@Override
							public void adFailedCallback(Exception e) {

								if (settings.getOnAdErrorListener() != null) {
									settings.getOnAdErrorListener().onAdError("Backfill exception", e);
								}
								else {
									SdkLog.e(TAG,
											"Backfill: An exception occured.",
											e);									
								}
								if (target != null) {
									startActivity(target);
								}
								else {
									SdkLog.i(TAG,  "No target. Back to previous view.");
									finish();
								}
							}
							
							@Override
							public void receivedAdCallback() {
								if (settings.getOnAdSuccessListener() != null) {
									settings.getOnAdSuccessListener().onAdSuccess();
								}
							}
						});
				
			} catch (BackfillDelegator.BackfillException bfE) {
				processError("Backfill error thrown.", bfE);
			}
		} else if (data == null || data.length() < 10) {
			// head to original intent
			
			if (this.settings.getOnAdEmptyListener() != null) {
				this.settings.getOnAdEmptyListener().onAdEmpty();
			}
			if (target != null) {
				SdkLog.d(TAG, "No interstitial -> starting original target.");
				startActivity(target);
			}
			else {
				SdkLog.d(TAG, "No interstitial, no target -> back to previous view.");
				finish();
			}
		} else if (data.indexOf("<VAST") < 20) {
			// head to video interstitial intent
			Intent i = new Intent(
					getApplicationContext(),
					VideoInterstitialActivity.class);
			SdkLog.i(TAG, "Found video interstitial -> show");
			// pass banner data and original intent to video interstitial
			i.putExtra("data", data);
			i.putExtra("target", target);
			i.putExtra("unmuted", Boolean.valueOf(getIntent().getExtras().getBoolean("unmuted")));			
			if (this.settings.getOnAdSuccessListener() != null) {
				this.settings.getOnAdSuccessListener().onAdSuccess();
			}
			startActivity(i);			
		}
		else {
			// head to interstitial intent
			Intent i = new Intent(
					getApplicationContext(),
					InterstitialActivity.class);
			SdkLog.i(TAG, "Found interstitial -> show");
			// pass banner data and original intent to interstitial
			i.putExtra("data", data);
			i.putExtra("target", target);
			i.putExtra("timeout",
					(Integer) getIntent().getExtras().get("timeout"));
			if (this.settings.getOnAdSuccessListener() != null) {
				this.settings.getOnAdSuccessListener().onAdSuccess();
			}
			startActivity(i);
		}		
	}
	
	@Override
	public void processError(String msg) {
		if (this.settings.getOnAdErrorListener() != null) {
			this.settings.getOnAdErrorListener().onAdError(msg);
		}
		else {
			SdkLog.e(TAG, msg);
		}
		if (target != null) {
			startActivity(target);
		}
		else {
			SdkLog.i(TAG,  "No target. Back to previous view.");
			finish();
		}
	}

	@Override
	public void processError(String msg, Throwable t) {
		if (this.settings.getOnAdErrorListener() != null) {
			this.settings.getOnAdErrorListener().onAdError(msg, t);
		}
		else {
			SdkLog.e(TAG, msg, t);
		}
		if (target != null) {
			startActivity(target);
		}
		else {
			SdkLog.i(TAG,  "No target. Back to previous view.");
			finish();
		}		
	}

}
