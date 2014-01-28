package de.guj.ems.mobile.sdk.controllers.adserver;

/**
 * Interface for objects holding ad server responses
 * 
 * A response may contain different types of ads: rich media, image only or
 * empty.
 * 
 * For image only ads, a parser can be used which holds the image url, click url
 * and possible tracking url. Parsers are used for native views, i.e. ImageViews
 * instead of WebViews for better performance.
 * 
 * @author stein16
 * 
 */
public interface IAdResponse {

	/**
	 * Get the actual adserver response
	 * 
	 * @return ad server response as Strgin (html, xml, text, ...)
	 */
	public String getResponse();
	
	/**
	 * Get the response as preformatted HTML
	 * 
	 * @return ad server response as HTML
	 */
	public String getResponseAsHTML();	

	/**
	 * For image only types of responses, a parser may be referenced
	 * 
	 * @return The parser if one could be created
	 */
	public AdResponseParser getParser();

	/**
	 * Check whether the ad is image only
	 * 
	 * @return true if ad is image only and not empty
	 */
	public boolean isImageAd();

	/**
	 * Check whether the ad is an rich ad, i.e. JScript, html etc.
	 * 
	 * @return true if the ad is a rich ad and not empty
	 */
	public boolean isRichAd();

	/**
	 * Check whether the response contained any ad
	 * 
	 * @return true if the response was empty
	 */
	public boolean isEmpty();

	/**
	 * Check whether the ad is a test ad
	 * 
	 * @return true if the ad was a test ad, i.e. no actual ad but a dummy
	 */
	public boolean isTest();

}
