package de.guj.ems.mobile.sdk.controllers;

import android.content.Context;

import com.video.adsdk.VideoAdSDK;
import com.video.adsdk.VideoAdSDKListener;

import de.guj.ems.mobile.sdk.util.SdkLog;

/**
 * Backfill adapter implementation for the Smartstream Video SDK
 * This is used for video interstitials. If a new placement is passed to
 * the adapter, it re-registers at the Video SDK and updates the callbacks.
 * 
 * Other than that the adapter simply passes the correct placement ID and 
 * triggers loading and playing of the video (if there is any in the SDK response). 
 * 
 * @author stein16
 *
 */
public class SmartstreamAdapter implements BackfillAdapter {

	private final static String TAG = "SmartstreamAdapter";

	private static String lastData = null;

	private static BackfillDelegator.BackfillCallback callback;

	@Override
	public void execute(Context context,
			final BackfillDelegator.BackfillCallback callback, final BackfillDelegator.BackfillData bfData) {
		SmartstreamAdapter.callback = callback;
		if (!bfData.getData().equals(lastData)) {
			VideoAdSDK.registerWithPublisherID(context, bfData.getData(),
					new VideoAdSDKListener() {
						public void onAdvertisingIsReadyToPlay() {
							// autoplay when the video is prepared and
							// buffered
							VideoAdSDK.startAdvertising();
						}

						public void onAdvertisingClicked() {
							// 3rd party tracking for the click event on the
							// video layer
							// TODO click event
						}

						public void onAdvertisingEventTracked(String arg0) {
							// 3rd party tracking for VAST events incl.
							// ViewTime
							// TODO view time event
							SdkLog.d(TAG, "Smartstream Advertising Event: "
									+ arg0);
							if (arg0.toLowerCase().equals("impression")) {
								SmartstreamEvents.processEvent(bfData.getUserAgent(), bfData.getZoneId(), bfData.getData(), SmartstreamEvents.SMARTSTREAM_EVENT_IMPRESSION, false);
							}
							else if (arg0.toLowerCase().equals("start")) {
								SmartstreamEvents.processEvent(bfData.getUserAgent(), bfData.getZoneId(), bfData.getData(), SmartstreamEvents.SMARTSTREAM_EVENT_PLAY, false);								
							}
							else if (arg0.toLowerCase().equals("firstquartile")) {
								SmartstreamEvents.processEvent(bfData.getUserAgent(), bfData.getZoneId(), bfData.getData(), SmartstreamEvents.SMARTSTREAM_EVENT_QUARTILE_1, false);								
							}
							else if (arg0.toLowerCase().equals("midpoint")) {
								SmartstreamEvents.processEvent(bfData.getUserAgent(), bfData.getZoneId(), bfData.getData(), SmartstreamEvents.SMARTSTREAM_EVENT_MID, false);								
							}
							else if (arg0.toLowerCase().equals("thirdquartile")) {
								SmartstreamEvents.processEvent(bfData.getUserAgent(), bfData.getZoneId(), bfData.getData(), SmartstreamEvents.SMARTSTREAM_EVENT_QUARTILE_3, false);
							}
							else if (arg0.toLowerCase().equals("complete")) {
								SmartstreamEvents.processEvent(bfData.getUserAgent(), bfData.getZoneId(), bfData.getData(), SmartstreamEvents.SMARTSTREAM_EVENT_FINISH, false);								
							}
						}

						public void onAdvertisingFailedToLoad(Exception arg0) {
							// exception handler for preloading or playback
							// issues
							SmartstreamEvents.processEvent(bfData.getUserAgent(), bfData.getZoneId(), bfData.getData(), SmartstreamEvents.SMARTSTREAM_EVENT_FAIL, false);
							SdkLog.e(
									TAG,
									"Smartstream Backfill failed. Starting original intent.",
									arg0);
							SmartstreamAdapter.callback.adFailedCallback(arg0);
						}

						public void onAdvertisingNotAvailable() {
							// poor connectivity or no video advertising
							SdkLog.e(TAG,
									"Smartstream Backfill unavailable. Starting original intent.");
							SmartstreamAdapter.callback.noAdCallback();
						}

						public void onAdvertisingPrefetchingDidComplete() {
							// prefetching (asynchronous background process)
							// completed
						}

						public void onPrefetcherProgress(double arg0) {
							// current status of prefetching (debugging
							// only)
						}

						public void onAdvertisingWillShow() {
							// the advertising appears in fullscreen mode
							SmartstreamAdapter.callback.receivedAdCallback();
						}

						public void onAdvertisingDidHide() {
							// the advertising activity is released
							SdkLog.d(TAG,
									"Smartstream Ad finished. Starting original intent.");
							SmartstreamAdapter.callback.finishedCallback();
						}
					});
					SmartstreamAdapter.lastData = bfData.getData();
		}
		//VideoAdSDK.startPrefetching();
		VideoAdSDK.playAdvertising();
	}
}
