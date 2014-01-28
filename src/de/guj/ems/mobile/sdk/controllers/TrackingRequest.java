package de.guj.ems.mobile.sdk.controllers;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.os.Build;
import de.guj.ems.mobile.sdk.controllers.adserver.AdRequest;
import de.guj.ems.mobile.sdk.controllers.adserver.IAdResponse;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;

/**
 * Simple class for tracking requests / simple http requests.
 * 
 * Requests are implemented as asynchronous tasks.
 * 
 * Usage: (new TrackingRequest()).execute(url);
 * 
 * Tracking Requests have no response handlers
 * 
 * @author stein16
 * 
 */
public class TrackingRequest extends AdRequest {

	private final static String TAG = "TrackingRequest";

	private final static boolean USE_HTTPURLCONNECTION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;

	private final static String USER_AGENT_HEADER_NAME = "User-Agent";

	private TrackingRequest(String securityHeader, int securityHash,
			IAdResponseHandler handler) {
		super(handler);
	}

	private TrackingRequest(IAdResponseHandler handler) {
		super(handler);
	}

	public TrackingRequest() {
		super(null);
	}

	@Override
	protected IAdResponse httpGet(String url) {
		// from Gingerbread on it is recommended to use HttpUrlConnection
		if (TrackingRequest.USE_HTTPURLCONNECTION) {
			SdkLog.d(TAG, "Younger than Froyo - using HttpUrlConnection.");
			HttpURLConnection con = null;
			try {
				URL uUrl = new URL(url);
				con = (HttpURLConnection) uUrl.openConnection();
				con.setRequestProperty(USER_AGENT_HEADER_NAME,
						SdkUtil.getUserAgent());
				con.setReadTimeout(2500);
				con.setConnectTimeout(2500);
				BufferedInputStream in = new BufferedInputStream(
						con.getInputStream());
				if (con.getResponseCode() != 200) {
					throw new Exception("Server returned HTTP "
							+ con.getResponseCode());
				}
				in.close();
			} catch (Exception e) {
				setLastError(e);
			} finally {
				if (con != null) {
					con.disconnect();
					SdkLog.d(TAG, "Request finished.");
				}
			}
		}
		// before Gingerbread, DefaultHttpClient should be used
		else {
			SdkLog.d(TAG, "Older than Gingerbread - using DefaultHttpClient.");
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 500);
			HttpConnectionParams.setSoTimeout(httpParameters, 1000);
			DefaultHttpClient client = new DefaultHttpClient(httpParameters);
			HttpGet httpGet = new HttpGet(url);
			httpGet.setHeader(USER_AGENT_HEADER_NAME, SdkUtil.getUserAgent());
			try {
				HttpResponse execute = client.execute(httpGet);
				if (execute.getStatusLine().getStatusCode() != 200) {
					throw new Exception("Server returned HTTP "
							+ execute.getStatusLine().getStatusCode());
				}
			} catch (Exception e) {
				setLastError(e);
			}
			SdkLog.d(TAG, "Request finished.");
		}
		return null;
	}

}
