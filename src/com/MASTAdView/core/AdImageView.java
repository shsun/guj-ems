package com.MASTAdView.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.DisplayMetrics;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.MASTAdView.MASTAdConstants;
import com.MASTAdView.MASTAdDelegate;
import com.MASTAdView.MASTAdLog;
import com.MASTAdView.MASTAdView;


@SuppressLint("SetJavaScriptEnabled")
public class AdImageView extends WebView
{	
	final private MASTAdLog adLog;
	final private AdViewContainer adViewContainer;
	private JavascriptInterface javascriptInterface;
	private MraidInterface mraidInterface;
	//private String mraidScript;
	//private DisplayMetrics metrics;
	//final private boolean launchBrowserOnClicks;
	private AdClickHandler adClickHandler = null;
	
	
	public AdImageView(AdViewContainer parent, MASTAdLog log, DisplayMetrics metrics, boolean handleClicks)
	{
		super(parent.getContext());
		
		adViewContainer = parent;
		adLog = log;
		//this.metrics = metrics;
		//launchBrowserOnClicks = handleClicks; 
		
		// Clients for javascript and other integration
		setWebChromeClient(new AdWebChromeClient());
		setWebViewClient(new AdWebViewClient(parent.getContext()));
		
		// Customize settings for web view
		WebSettings webSettings = getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setSavePassword(false);
		webSettings.setSaveFormData(false);
		webSettings.setSupportZoom(false);
		webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
		
		// apply standard properties
		setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
				
		if (handleClicks)
		{
			adClickHandler = new AdClickHandler(adViewContainer);
		}
	}
	
	
	public JavascriptInterface getJavascriptInterface()
	{
		return javascriptInterface;
	}
	
	
	public MraidInterface getMraidInterface()
	{
		return mraidInterface;
	}
	
	
	synchronized public void resetForNewAd()
	{
		stopLoading();
		clearView();
	}
	
	
	final private class AdWebChromeClient extends WebChromeClient
	{
		@Override
		public boolean onJsAlert(WebView view, String url, String message, JsResult result)
		{
			// Handle alert message from javascript
			System.out.println("JSAlert: " + message);
			return super.onJsAlert(view, url, message, result);
		}
	}
		
	
	protected void defaultOnAdClickHandler(AdImageView viev, String url)
	{
		if (adClickHandler != null)
		{
			adClickHandler.openUrlForBrowsing(getContext(), url);
		}
	}
	
	
	final private class AdWebViewClient extends WebViewClient
	{
		public AdWebViewClient(Context context)
		{
			super();
		}
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
			try
			{
				adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "OverrideUrlLoading",url);
				MASTAdDelegate delegate = adViewContainer.getAdDelegate();
				if (delegate != null)
				{
					MASTAdDelegate.AdActivityEventHandler clickHandler = delegate.getAdActivityEventHandler();
					 
					if ( clickHandler != null)
					{
						if (clickHandler.onAdClicked((MASTAdView)adViewContainer, url) == false)
						{
							// If click() method returns false, continue with default logic
							defaultOnAdClickHandler((AdImageView)view, url);
						}
					}
					else
					{
						defaultOnAdClickHandler((AdImageView)view, url);
					}
				}
				else
				{
					defaultOnAdClickHandler((AdImageView)view, url);
				}
			}
			catch(Exception e)
			{
				adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "shouldOverrideUrlLoading", e.getMessage());
			}
			
			return true;
		}
		
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon)
		{
			adLog.log(MASTAdLog.LOG_LEVEL_DEBUG, "onPageStarted", "loading image");
						
			super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url)
		{	
			//if(isAutoCollapse) setAdVisibility(View.VISIBLE);
			
			MASTAdDelegate delegate = adViewContainer.getAdDelegate();
			if (delegate != null)
			{
				MASTAdDelegate.AdDownloadEventHandler downloadHandler = delegate.getAdDownloadHandler(); 
				if ( downloadHandler != null)
				{
					downloadHandler.onAdViewable((MASTAdView)adViewContainer);
				}
			}
			
			super.onPageFinished(view, url);
		}

		
		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
		{
			super.onReceivedError(view, errorCode, description, failingUrl);
		
			MASTAdDelegate delegate = adViewContainer.getAdDelegate();
			if (delegate != null)
			{
				MASTAdDelegate.AdDownloadEventHandler downloadHandler = delegate.getAdDownloadHandler();
				if (downloadHandler != null)
				{
					downloadHandler.onDownloadError((MASTAdView)adViewContainer, description);
				}
			}
		}
	}
	
	
	public void setImage(AdData ad)
	{ 
		StringBuffer data = new StringBuffer("<html>");
		data.append("<head><meta name=\"viewport\" content=\"target-densityDpi=device-dpi\"><style>body{margin:0; padding:0; width: 100%; height: 100%; display: table;} div{display: table-cell; vertical-align: middle; text-align:center;}</style>");
		data.append("<script language=\"javascript\">function AutoScale() {");
		data.append("var normWidth  = document.body.clientWidth  / document.getElementById(\"ADIMAGE\").naturalWidth;");
		data.append("var normHeight = document.body.clientHeight / document.getElementById(\"ADIMAGE\").naturalHeight;");
		data.append("var scaleFactor = normWidth; if (normWidth > normHeight) scaleFactor = normHeight;");
		data.append("if (scaleFactor > 1 && scaleFactor != 0) {");
		data.append("document.getElementById(\"ADIMAGE\").style.width = document.getElementById(\"ADIMAGE\").naturalWidth * scaleFactor;");
		data.append("document.getElementById(\"ADIMAGE\").style.height = document.getElementById(\"ADIMAGE\").naturalHeight * scaleFactor;");
		data.append("}}</script></head>");
		data.append("<body onload=\"javascript:AutoScale();\" onresize=\"javascript:AutoScale();\" style=\"background-color:#");
		data.append("" + MASTAdConstants.DEFAULT_COLOR + "\">");
		data.append("<div id=\"adwrap\"><A HREF=\"" + ad.clickUrl + "\"><IMG ID=\"ADIMAGE\" SRC=\"" + ad.imageUrl + "\"></A></div>");
		data.append("</body></html>");
		
		//System.out.println("AdImageView: injecting code: " + data.toString());
        loadDataWithBaseURL(null, data.toString(), "text/html", "UTF-8", null);
	}
}
