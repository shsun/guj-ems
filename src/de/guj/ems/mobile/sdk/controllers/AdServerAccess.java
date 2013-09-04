package de.guj.ems.mobile.sdk.controllers;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.os.AsyncTask;
import android.os.Build;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.views.AdResponseHandler;

/**
 * Performs HTTP communication in the background, i.e. off the UI thread.
 * 
 * Pass the user-agent at construction time. Pass the URL to the get-Method when
 * actually fetching an ad.
 * 
 * @author stein16
 * 
 */
public final class AdServerAccess extends AsyncTask<String, Void, String> {

	private final static String NEW_LINE = System.getProperty("line.separator");
	
	private String userAgentString;

	private final String TAG = "AdServerAccess";
	
	private final static boolean USE_HTTPURLCONNECTION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
		
	private final static String ACCEPT_HEADER_NAME = "Accept";
	
	private final static String ACCEPT_HEADER_VALUE = "text/plain,text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
	
	private final static String ACCEPT_CHARSET_HEADER_NAME = "Accept-Charset";
	
	private final static String ACCEPT_CHARSET_HEADER_VALUE = "utf-8;q=0.7,*;q=0.3";
	
	private final static String USER_AGENT_HEADER_NAME = "User-Agent";
	
	private final static String ENCODING_STR = "utf-8";
	
	private final static byte [] EMPTY_BUFFER = new byte [1024];
	
	private AdResponseHandler responseHandler;
	
	private Throwable lastError;
	
	@SuppressWarnings("unused")
	private AdServerAccess() {

	}

	/**
	 * The only valid constructor.
	 * 
	 * @param handler
	 * 				instance of a class handling ad server responses (like GuJEMSAdView, InterstitialSwitchActivity)
	 * @param userAgentString
	 *            the string to pass as the user-agent
	 *           
	 */
	public AdServerAccess(String userAgentString, AdResponseHandler handler) {
		this.userAgentString = userAgentString;
		this.responseHandler = handler;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
		     System.setProperty("http.keepAlive", "false");
		}
		SdkLog.d(TAG, "User-Agent: " + userAgentString);
	}
	
	private StringBuilder httpGet(String url) {
		StringBuilder rBuilder = new StringBuilder();
		// from Gingerbread on it is recommended to use HttpUrlConnection
		if (AdServerAccess.USE_HTTPURLCONNECTION) {
			SdkLog.d(TAG, "Younger than Froyo - using HttpUrlConnection.");
			HttpURLConnection con = null;
			try {
				URL uUrl = new URL(url);
				con = (HttpURLConnection)uUrl.openConnection();
				con.setRequestProperty(USER_AGENT_HEADER_NAME, userAgentString);
				con.setRequestProperty(ACCEPT_HEADER_NAME, ACCEPT_HEADER_VALUE);
				con.setRequestProperty(ACCEPT_CHARSET_HEADER_NAME, ACCEPT_CHARSET_HEADER_VALUE);
				con.setReadTimeout(2500);
				con.setConnectTimeout(2500);
				BufferedInputStream in = new BufferedInputStream(con.getInputStream());
				if (con.getResponseCode() == 200 && this.responseHandler !=null) {
					byte [] buffer = new byte [1024];
					int l = 0;
					while ((l = in.read(buffer)) > 0) {
						rBuilder.append(new String(buffer, ENCODING_STR), 0, l);
						buffer = EMPTY_BUFFER;
					}
				}
				else if (con.getResponseCode() != 200) {

					throw new Exception("AdServer returned HTTP "
							+ con.getResponseCode());					
				}
				in.close();
			}
			//TODO REMOVE BEFORE DEPLOY
			catch (IndexOutOfBoundsException ie) {
				SdkLog.e(TAG, "THIS SHOULD NOT HAPPEN: ", ie);
				SdkLog.i(TAG, "content: " + rBuilder.toString());
				SdkLog.i(TAG, "length: " + rBuilder.length());
			}
			catch (Exception e) {
				this.lastError = e;
			}
			finally  {
				if (con != null) {
					con.disconnect();
					SdkLog.d(TAG, "Request finished. [" + rBuilder.length() + "]");
				}
			}
		}
		// before Gingerbread, DefaultHttpClient should be used
		else {
			SdkLog.d(TAG, "Older than Gingerbread - using DefaultHttpClient.");
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters , 500);
			HttpConnectionParams.setSoTimeout(httpParameters , 1000);
			DefaultHttpClient client = new DefaultHttpClient(httpParameters);
			HttpGet httpGet = new HttpGet(url);
			httpGet.setHeader(USER_AGENT_HEADER_NAME, userAgentString);
			httpGet.setHeader(ACCEPT_HEADER_NAME, ACCEPT_HEADER_VALUE);
			httpGet.setHeader(ACCEPT_CHARSET_HEADER_NAME, ACCEPT_CHARSET_HEADER_VALUE);
			try {
				HttpResponse execute = client.execute(httpGet);
				if (execute.getStatusLine().getStatusCode() == 200 && this.responseHandler !=null) {
					BufferedReader buffer = new BufferedReader(
							new InputStreamReader(execute.getEntity()
									.getContent(),ENCODING_STR));
					String line;
					while ((line = buffer.readLine()) != null) {
						rBuilder.append(line + AdServerAccess.NEW_LINE);
					}
					buffer.close();
				} else if (execute.getStatusLine().getStatusCode() != 200) {

					throw new Exception("AdServer returned HTTP "
							+ execute.getStatusLine().getStatusCode());
				}

			} catch (Exception e) {
				this.lastError = e;
			}
			SdkLog.d(TAG, "Request finished. [" + rBuilder.length() + "]");
		}
		return rBuilder;		
	}

	@Override
	protected String doInBackground(String... urls) {
		StringBuilder rBuilder = new StringBuilder();
		for (String url : urls) {
			SdkLog.d(TAG, "Request: " + url);
			rBuilder = rBuilder.append(httpGet(url));
		}
		return rBuilder.toString();
	}

	@Override
	protected void onPostExecute(String result) {
		if (this.responseHandler != null && lastError == null) {
			SdkLog.d(TAG, "Passing to handler " + responseHandler);
			this.responseHandler.processResponse(result);
		}
		else if (this.responseHandler != null && lastError != null) {
			SdkLog.d(TAG, "Passing to handler " + responseHandler);
			this.responseHandler.processError(lastError.getMessage(), lastError);
		}
		else if (lastError != null) {
			SdkLog.e(TAG, "Error post processing request", lastError);
		}
		else {
			SdkLog.d(TAG, "No response handler");
		}
	}
	
}