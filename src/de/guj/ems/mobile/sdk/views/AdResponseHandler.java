package de.guj.ems.mobile.sdk.views;


public interface AdResponseHandler {
	
	public void processResponse(String response);
	
	public void processError(String msg);
	
	public void processError(String msg, Throwable t);
	
}
