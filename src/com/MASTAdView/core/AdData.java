//
// Copyright (C) 2011, 2012 Mocean Mobile. All Rights Reserved. 
//
package com.MASTAdView.core;

import java.io.InputStream;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.MASTAdView.MASTAdConstants;
import com.MASTAdView.MASTAdLog;

final public class AdData
{
	// recognized ad type names
	public static final String typeNameImage 				= "image";
	public static final String typeNameText 				= "text";
	public static final String typeNameRichMedia 			= "richmedia";
	public static final String typeNameThirdParty 			= "thirdparty";
	public static final String typeNameExternalThirdParty 	= "externalthirdparty";
	
	
	public Integer				adType 						= MASTAdConstants.AD_TYPE_UNKNOWN;
	public String 				thirdPartyFeed 				= null;
	public String 				clickUrl 					= null;
	public String 				text 						= null;
	public String 				imageUrl 					= null;
	volatile public Bitmap 		imageBitmap 				= null;
	public String 				trackUrl 					= null;
	public String 				richContent 				= null;
	public String 				error 						= null;
	public Integer				serverErrorCode				= null;
	public List<NameValuePair>	externalCampaignProperties	= null;
	public String 				responseData 				= null;
	
	
	synchronized public boolean hasContent()
	{
		if (adType != null)
		{
			if ((adType == MASTAdConstants.AD_TYPE_TEXT) && (text != null) && (text.length() > 0))
			{
				return true;
			}
			
			if ((adType == MASTAdConstants.AD_TYPE_IMAGE) && ((imageBitmap != null) || (imageUrl != null)))
			{
				return true;
			}
			
			if ((adType == MASTAdConstants.AD_TYPE_RICHMEDIA) && (richContent != null) && (richContent.length() > 0))
			{
				return true;
			}
			
			if ((adType == MASTAdConstants.AD_TYPE_THIRDPARTY) && (richContent != null) && (richContent.length() > 0))
			{
				return true;
			}
			
			if ((adType == MASTAdConstants.AD_TYPE_EXTERNAL_THIRDPARTY) && (externalCampaignProperties != null) && (externalCampaignProperties.size() > 0))
			{
				return true;
			}
		}
		
		return false;
	}
	
	
	synchronized public void setAdTypeByName(String typeName)
	{
		if (typeName.compareTo(typeNameImage) == 0)
		{
			adType = MASTAdConstants.AD_TYPE_IMAGE;
		}
		else if (typeName.compareTo(typeNameText) == 0)
		{
			adType = MASTAdConstants.AD_TYPE_TEXT;
		} 
		else if (typeName.compareTo(typeNameRichMedia) == 0)
		{
			adType = MASTAdConstants.AD_TYPE_RICHMEDIA;
		}
		else if (typeName.compareTo(typeNameThirdParty) == 0)
		{
			adType = MASTAdConstants.AD_TYPE_THIRDPARTY;
		}
		else if (typeName.compareTo(typeNameExternalThirdParty) == 0)
		{
			adType = MASTAdConstants.AD_TYPE_EXTERNAL_THIRDPARTY;
		}
		else
		{
			adType = MASTAdConstants.AD_TYPE_UNKNOWN;
		}
	}
	
	
	synchronized public String getAdTypeName()
	{
		switch(adType)
		{
			case MASTAdConstants.AD_TYPE_IMAGE:
				return typeNameImage;
			case MASTAdConstants.AD_TYPE_TEXT:
				return typeNameText;
			case MASTAdConstants.AD_TYPE_RICHMEDIA:
				return typeNameRichMedia;
			case MASTAdConstants.AD_TYPE_THIRDPARTY:
				return typeNameThirdParty;
			case MASTAdConstants.AD_TYPE_EXTERNAL_THIRDPARTY:
				return typeNameExternalThirdParty;
			default:
				return null;
		}
	}

	
	synchronized public void setImage(String url, boolean prefetch)
	{
		imageUrl = url;
		if (prefetch)
		{
			imageBitmap = fetchImage(url);
		}
	}
	
	
	synchronized public void setImage(String url, Bitmap bitmap)
	{
		imageUrl = url;
		imageBitmap = bitmap;
	}
	
	
	public static InputStream fetchUrl(String fromUrl)
	{
		return fetchUrl(fromUrl, null);
	}
	
	public static InputStream fetchUrl(String fromUrl, String userAgent)
	{		
		try
		{
			//System.out.println("Fetcing URL: " + fromUrl);
			HttpClient httpclient = new DefaultHttpClient();  
    		HttpGet request = new HttpGet(fromUrl);
    
    		// Always include our user agent header, if needed
    		if (userAgent != null)
			{
    			request.setHeader("User-Agent", userAgent);
			}
    		
    		HttpResponse response = httpclient.execute(request);     
    		StatusLine statusLine = response.getStatusLine();    
    		if (statusLine.getStatusCode() == 200)
    		{
				return response.getEntity().getContent();
    		}
		}
		catch(Exception e)
		{
			//System.out.println("Fetcher: generic exception: " + e.getMessage());
			MASTAdLog logger = new MASTAdLog(null);
			logger.log(MASTAdLog.LOG_LEVEL_ERROR, "AdData.fetchUrl exception", e.getMessage());
		}
		
		return null;
	}
	
	
	public static void sendImpressionOnThread(String url, String userAgent)
	{
		if ((url == null) || (url.length() < 1))
		{
			return;
		}
		
		InputStream is = fetchUrl(url, userAgent);
		
		if (is != null)
		{	
			try
			{
				is.close();	
			}
			catch(Exception ex)
			{
				// error closing stream... bleh!
			}
		}
	}
	
	
	public static void sendImpressionInBackground(final String url, final String userAgent)
	{
		if ((url == null) || (url.length() < 1))
		{
			return;
		}
		
		Thread thread = new Thread()
		{
			@Override
			public void run()
			{
				sendImpressionOnThread(url, userAgent);
			}
		};
		thread.setName("[AdData] sendImpression");
		thread.start();
	}
	
	
	public static Bitmap fetchImage(String url)
	{
		InputStream is = fetchUrl(url);
		
		if (is != null)
		{
			Bitmap image = BitmapFactory.decodeStream(is);
			
			try
			{
				is.close();	
			}
			catch(Exception ex)
			{
				// error closing stream... bleh!
			}
			
			/*
			if (image != null)
			{
				System.out.println("fetchImage: Image decoded, size: " + image.getWidth() + "x" + image.getHeight());
			}
			*/
			
			return image;
		}
		
		return null;
	}
	
	
	synchronized public String toString()
	{
		StringBuffer sb = new StringBuffer();
		sb.append("Ad: type=" + getAdTypeName());
		
		if (adType == MASTAdConstants.AD_TYPE_TEXT)
		{
			sb.append(", text=" + text);
		}
		else if (adType == MASTAdConstants.AD_TYPE_IMAGE)
		{
			sb.append(", url=" + imageUrl);
		} 
		else if (adType == MASTAdConstants.AD_TYPE_RICHMEDIA)
		{
			sb.append(", richContent=" + richContent);
		}
		else if (adType == MASTAdConstants.AD_TYPE_THIRDPARTY)
		{
			sb.append(", feed=" + thirdPartyFeed + ", richContent=" + richContent);
		}
		else if (adType == MASTAdConstants.AD_TYPE_EXTERNAL_THIRDPARTY)
		{
			sb.append(", external campaign properties=" + externalCampaignProperties.toString());
		}
		
		if (clickUrl != null)
		{
			sb.append(", clickUrl=" + clickUrl);
		}
		
		if (error != null)
		{
			sb.append(", ERROR=" + error);
		}
		
		return sb.toString();
	}	
}
