package de.guj.ems.mobile.sdk.controllers.adserver;

import android.os.AsyncTask;
import android.os.Build;
import de.guj.ems.mobile.sdk.controllers.IAdResponseHandler;
import de.guj.ems.mobile.sdk.util.SdkLog;

/**
 * Performs HTTP communication in the background, i.e. off the UI thread.
 * 
 * Pass the URL to the execute-Method when actually fetching an ad.
 * 
 * @author stein16
 * 
 */
public abstract class AdRequest extends AsyncTask<String, Void, IAdResponse> {

	private final String TAG = "AdRequest";

	private IAdResponseHandler responseHandler;

	private Throwable lastError;

	@SuppressWarnings("unused")
	private AdRequest() {

	}

	/**
	 * Standard constructor
	 * 
	 * @param handler
	 *            instance of a class handling ad server responses (like
	 *            GuJEMSAdView, InterstitialSwitchActivity)
	 * 
	 */
	public AdRequest(IAdResponseHandler handler) {
		this.responseHandler = handler;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
			System.setProperty("http.keepAlive", "false");
		}
	}

	protected abstract IAdResponse httpGet(String url);

	@Override
	protected IAdResponse doInBackground(String... urls) {
		IAdResponse response = null;
		for (String url : urls) {
			SdkLog.d(TAG, "Request: " + url);
			if (response != null) {
				SdkLog.w(TAG,
						"Multiple URLs in adserver request task. Ignoring "
								+ url);
			}
			response = httpGet(url);
		}
		return response;
	}

	@Override
	protected void onPostExecute(IAdResponse response) {
		if (this.responseHandler != null && lastError == null) {
			SdkLog.d(TAG, "Passing to handler " + responseHandler);
			this.responseHandler.processResponse(response);
		} else if (this.responseHandler != null && lastError != null) {
			SdkLog.d(TAG, "Passing to handler " + responseHandler);
			this.responseHandler
					.processError(lastError.getMessage(), lastError);
		} else if (lastError != null) {
			SdkLog.e(TAG, "Error post processing request", lastError);
		} else {
			SdkLog.d(TAG, "No response handler");
		}
	}

	protected IAdResponseHandler getResponseHandler() {
		return this.responseHandler;
	}

	protected Throwable getLastError() {
		return this.lastError;
	}

	protected void setLastError(Throwable t) {
		this.lastError = t;
	}

}