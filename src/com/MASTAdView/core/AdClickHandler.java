package com.MASTAdView.core;

import java.net.HttpURLConnection;
import java.net.URL;

import org.ormma.view.Browser;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import com.MASTAdView.MASTAdLog;


public class AdClickHandler implements View.OnClickListener
{
	final private AdViewContainer parentContainer;
	final private MASTAdLog adLog;
	final private AdData adData;
	
	private OpenUrlThread openUrlThread = null;
	
	
	public AdClickHandler(AdViewContainer parent)
	{
		parentContainer = parent;
		adLog = parentContainer.getLog();
		adData = null;
	}
	
	
	public AdClickHandler(AdViewContainer parent, AdData data)
	{
		parentContainer = parent;
		adLog = parentContainer.getLog();
		adData = data;
	}
	
	
	public void onClick(View v)
	{
		if ((adData != null) && (adData.clickUrl != null))
		{
			 openUrlForBrowsing(parentContainer.getContext(), adData.clickUrl);
			
		}
	}
	
	
	public void openUrlForBrowsing(Context context, String url)
	{
		if (url==null) return;
		
		if ((openUrlThread==null) || (openUrlThread.getState().equals(Thread.State.TERMINATED)))
		{
			openUrlThread = new OpenUrlThread(parentContainer.getContext(), url);
			openUrlThread.start();
		}
		else if (openUrlThread.getState().equals(Thread.State.NEW))
		{
			openUrlThread.start();
		}
	}
	
	
	private class OpenUrlThread extends Thread
	{
		Context context;
		String url;
		
		public OpenUrlThread(Context context, String url)
		{
			this.context =context;
			this.url = url;
		}

		@Override
		public void run() {
			openUrlWorker(context, url);
		}
	}
	
	
	private void openUrlWorker(final Context context, final String url)
	{
		String lastUrl = null;
		String newUrl =  url;
		URL connectURL;
		
		// Follow redirects to final resource location
		while(!newUrl.equals(lastUrl))
		{
			lastUrl = newUrl;
			try
			{					
				connectURL = new URL(newUrl);					
				HttpURLConnection conn = (HttpURLConnection)connectURL.openConnection();
				newUrl = conn.getHeaderField("Location");
				if (newUrl==null) 
				{
					newUrl=conn.getURL().toString();
				}
			}
			catch (Exception e)
			{
				newUrl = lastUrl;
			}				
		}
			
		
		Uri uri = Uri.parse(newUrl);
		if (parentContainer.getUseInternalBrowser() && (uri.getScheme().equals("http") || uri.getScheme().equals("https")))
		{
//			parentContainer.getHandler().post(new Runnable()
//			{			
//				@Override
//				public void run()
//				{
//					try
//					{
//						new InternalBrowser(context, url).show();
//					}
//					catch (Exception e)
//					{
//						adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "openUrlInInternalBrowser", e.getMessage());
//					}
//				}
//			});
			Intent i = new Intent(parentContainer.getContext(), Browser.class);
			//SdkLog.d(SdkLog_TAG, "open:" + url);
			i.putExtra(Browser.URL_EXTRA, newUrl);
			i.putExtra(Browser.SHOW_BACK_EXTRA, true);
			i.putExtra(Browser.SHOW_FORWARD_EXTRA, true);
			i.putExtra(Browser.SHOW_REFRESH_EXTRA, true);
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

			parentContainer.getContext().startActivity(i);			
		}
		else
		{
			try
			{
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(newUrl));
				context.startActivity(intent);
			}
			catch (Exception e)
			{
				adLog.log(MASTAdLog.LOG_LEVEL_ERROR, "openUrlInExternalBrowser","url="+ newUrl+"; error="+e.getMessage());
			}
		}		
	}

}
