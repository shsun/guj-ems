package de.guj.ems.mobile.sdk.controllers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import de.guj.ems.mobile.sdk.R;
import de.guj.ems.mobile.sdk.activities.InterstitialActivity;
import de.guj.ems.mobile.sdk.activities.VideoInterstitialActivity;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;
import de.guj.ems.mobile.sdk.views.AdResponseHandler;

public class InterstitialSwitchReceiver extends BroadcastReceiver implements
		AdResponseHandler {

	private IAdServerSettingsAdapter settings;

	private String userAgentString;

	private String data;

	private Intent target;

	private Intent intent;

	private Context context;

	private final static String TAG = "InterstitialSwitchReceiver";

	public InterstitialSwitchReceiver() {
		super();
	}

	@Override
	public void onReceive(Context arg0, Intent arg1) {

		if (SdkUtil.getContext() == null) {
			SdkUtil.setContext(arg0);
		}

		// determine user-agent
		this.userAgentString = SdkUtil.getUserAgent();

		// original target when interstitial not available
		this.target = (Intent) arg1.getExtras().get("target");
		if (this.target != null) {
			this.target.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		this.intent = arg1;
		this.context = arg0;

		// ad space settings
		this.settings = new AmobeeSettingsAdapter(SdkUtil.getContext(),
				arg1.getExtras());

		// adserver request
		if (SdkUtil.isOnline()) {
			final String url = this.settings.getRequestUrl();
			SdkLog.i(TAG, "START AdServer request");
			new AdServerAccess(this.userAgentString, this)
					.execute(new String[] { url });

		} else {
			SdkLog.i(TAG, "No network connection - not requesting ads.");
			processError("No network connection.");
		}
	}

	@Override
	public void processResponse(String response) {
		SdkLog.i(TAG, "FINISH AdServer request");
		BackfillDelegator.BackfillData bfD;
		this.data = response;
		if (data != null
				&& data.length() > 1
				&& (bfD = BackfillDelegator.isBackfill(
						(String) this.intent.getExtras().get(
								context.getString(R.string.amobeeAdSpace)),
						data)) != null) {
			SdkLog.d(TAG, "Possible backfill ad detected [id=" + bfD.getId()
					+ ", data=" + bfD.getData() + "]");
			try {

				BackfillDelegator.process(this.context, bfD,
						new BackfillDelegator.BackfillCallback() {
							@Override
							public void trackEventCallback(String arg0) {
								SdkLog.d(TAG, "Backfill: An event occured ["
										+ arg0 + "]");
							}

							@Override
							public void noAdCallback() {
								SdkLog.d(TAG, "Backfill: empty.");
								if (settings.getOnAdEmptyListener() != null) {
									settings.getOnAdEmptyListener().onAdEmpty();
								}
								if (target != null) {
									context.startActivity(target);
								} else {
									SdkLog.i(TAG,
											"No target. Back to previous view.");
								}
							}

							@Override
							public void finishedCallback() {
								if (target != null) {
									context.startActivity(target);
								} else {
									SdkLog.i(TAG,
											"No target. Back to previous view.");
								}
							}

							@Override
							public void adFailedCallback(Exception e) {

								if (settings.getOnAdErrorListener() != null) {
									settings.getOnAdErrorListener().onAdError(
											"Backfill exception", e);
								} else {
									SdkLog.e(TAG,
											"Backfill: An exception occured.",
											e);
								}
								if (target != null) {
									context.startActivity(target);
								} else {
									SdkLog.i(TAG,
											"No target. Back to previous view.");
								}
							}

							@Override
							public void receivedAdCallback() {
								if (settings.getOnAdSuccessListener() != null) {
									settings.getOnAdSuccessListener()
											.onAdSuccess();
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
				context.startActivity(target);
			} else {
				SdkLog.d(TAG,
						"No interstitial, no target -> back to previous view.");
			}
		} else if (data.startsWith("<VAST")) {
			
			// head to video interstitial intent
			Intent i = new Intent(context, VideoInterstitialActivity.class);
			SdkLog.i(TAG, "Found video interstitial -> show");
			// pass banner data and original intent to video interstitial
			i.putExtra("data", data);
			i.putExtra("target", target);
			i.putExtra("unmuted", Boolean.valueOf(intent.getExtras().getBoolean("unmuted")));
			if (this.settings.getOnAdSuccessListener() != null) {
				this.settings.getOnAdSuccessListener().onAdSuccess();
			}
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(i);
		} else {
			// head to interstitial intent
			Intent i = new Intent(this.context, InterstitialActivity.class);
			SdkLog.i(TAG, "Found interstitial -> show");
			// pass banner data and original intent to interstitial
			i.putExtra("data", data);
			i.putExtra("target", target);
			i.putExtra("timeout",
				(Integer) this.intent.getExtras().get("timeout"));
			if (this.settings.getOnAdSuccessListener() != null) {
				this.settings.getOnAdSuccessListener().onAdSuccess();
			}
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(i);
		}
	}

	@Override
	public void processError(String msg) {
		if (this.settings.getOnAdErrorListener() != null) {
			this.settings.getOnAdErrorListener().onAdError(msg);
		} else {
			SdkLog.e(TAG, msg);
		}
		if (target != null) {
			this.context.startActivity(target);
		} else {
			SdkLog.i(TAG, "No target. Back to previous view.");
		}
	}

	@Override
	public void processError(String msg, Throwable t) {
		if (this.settings.getOnAdErrorListener() != null) {
			this.settings.getOnAdErrorListener().onAdError(msg, t);
		} else {
			SdkLog.e(TAG, msg, t);
		}
		if (target != null) {
			this.context.startActivity(target);
		} else {
			SdkLog.i(TAG, "No target. Back to previous view.");
		}
	}

}
