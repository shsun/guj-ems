package de.guj.ems.mobile.sdk.controllers.adserver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.os.Build;

import de.guj.ems.mobile.sdk.controllers.IAdResponseHandler;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;

public class AmobeeAdRequest extends AdRequest {

	private final static String TAG = "AmobeeAdRequest";

	private final static String NEW_LINE = System.getProperty("line.separator");

	private String securityHeaderName;

	private int securityHeaderValueHash;

	private final static boolean USE_HTTPURLCONNECTION = Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;

	private final static String ACCEPT_HEADER_NAME = "Accept";

	private final static String ACCEPT_HEADER_VALUE = "text/plain,text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";

	private final static String ACCEPT_CHARSET_HEADER_NAME = "Accept-Charset";

	private final static String ACCEPT_CHARSET_HEADER_VALUE = "utf-8;q=0.7,*;q=0.3";

	private final static String USER_AGENT_HEADER_NAME = "User-Agent";

	private final static String ENCODING_STR = "utf-8";

	private final static byte[] EMPTY_BUFFER = new byte[1024];

	public AmobeeAdRequest(String securityHeader, int securityHash,
			IAdResponseHandler handler) {
		super(handler);
		this.securityHeaderName = securityHeader;
		this.securityHeaderValueHash = securityHash;
	}

	public AmobeeAdRequest(IAdResponseHandler handler) {
		super(handler);
	}

	@Override
	protected IAdResponse httpGet(String url) {
		StringBuilder rBuilder = new StringBuilder();
		boolean richAd = false;

		// from Gingerbread on it is recommended to use HttpUrlConnection
		if (AmobeeAdRequest.USE_HTTPURLCONNECTION) {
			SdkLog.d(TAG, "Younger than Froyo - using HttpUrlConnection.");
			HttpURLConnection con = null;
			try {
				boolean ok = true;
				URL uUrl = new URL(url);
				con = (HttpURLConnection) uUrl.openConnection();
				con.setRequestProperty(USER_AGENT_HEADER_NAME,
						SdkUtil.getUserAgent());
				con.setRequestProperty(ACCEPT_HEADER_NAME, ACCEPT_HEADER_VALUE);
				con.setRequestProperty(ACCEPT_CHARSET_HEADER_NAME,
						ACCEPT_CHARSET_HEADER_VALUE);
				con.setReadTimeout(2500);
				con.setConnectTimeout(750);
				BufferedInputStream in = new BufferedInputStream(
						con.getInputStream());
				if (this.securityHeaderName != null) {
					ok = (con.getHeaderField(this.securityHeaderName) != null && con
							.getHeaderField(this.securityHeaderName).hashCode() == this.securityHeaderValueHash);
				}
				richAd = con.getHeaderField("Richmedia") != null;
				if (ok && con.getResponseCode() == 200
						&& this.getResponseHandler() != null) {
					byte[] buffer = new byte[1024];
					int l = 0;
					while ((l = in.read(buffer)) > 0) {
						rBuilder.append(new String(buffer, ENCODING_STR), 0, l);
						buffer = EMPTY_BUFFER;
					}
				} else if (con.getResponseCode() != 200) {

					throw new Exception("AdServer returned HTTP "
							+ con.getResponseCode());
				} else if (!ok) {
					throw new Exception(
							"WARNING: AdServer response is missing required header. This is most likely a security breach! Response code is not being executed.");
				}
				in.close();
			} catch (Exception e) {
				setLastError(e);
			} finally {
				if (con != null) {
					con.disconnect();
					SdkLog.d(TAG, "Request finished. [" + rBuilder.length()
							+ "]");
				}
			}
		}
		// before Gingerbread, DefaultHttpClient should be used
		else {
			SdkLog.d(TAG, "Older than Gingerbread - using DefaultHttpClient.");
			HttpParams httpParameters = new BasicHttpParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 750);
			HttpConnectionParams.setSoTimeout(httpParameters, 2500);
			DefaultHttpClient client = new DefaultHttpClient(httpParameters);
			HttpGet httpGet = new HttpGet(url);
			httpGet.setHeader(USER_AGENT_HEADER_NAME, SdkUtil.getUserAgent());
			httpGet.setHeader(ACCEPT_HEADER_NAME, ACCEPT_HEADER_VALUE);
			httpGet.setHeader(ACCEPT_CHARSET_HEADER_NAME,
					ACCEPT_CHARSET_HEADER_VALUE);
			try {
				boolean ok = true;
				HttpResponse execute = client.execute(httpGet);
				if (this.securityHeaderName != null) {
					Header secHd = execute
							.getLastHeader(this.securityHeaderName);
					if (secHd == null) {
						ok = false;
					} else {
						ok = secHd != null
								&& secHd.getValue() != null
								&& secHd.getValue().hashCode() == this.securityHeaderValueHash;
					}
				}
				richAd = execute.getLastHeader("Richmedia") != null;
				if (ok && execute.getStatusLine().getStatusCode() == 200
						&& getResponseHandler() != null) {
					BufferedReader buffer = new BufferedReader(
							new InputStreamReader(execute.getEntity()
									.getContent(), ENCODING_STR));
					String line;
					while ((line = buffer.readLine()) != null) {
						rBuilder.append(line + AmobeeAdRequest.NEW_LINE);
					}
					buffer.close();
				} else if (execute.getStatusLine().getStatusCode() != 200) {

					throw new Exception("AdServer returned HTTP "
							+ execute.getStatusLine().getStatusCode());
				} else if (!ok) {
					throw new Exception(
							"WARNING: AdServer response is missing required header. This is most likely a security breach! Response code is not being executed.");
				}

			} catch (Exception e) {
				setLastError(e);
			}
			SdkLog.d(TAG, "Request finished. [" + rBuilder.length() + "]");
		}
		return new AmobeeAdResponse(rBuilder.toString(), richAd);
	}

}
