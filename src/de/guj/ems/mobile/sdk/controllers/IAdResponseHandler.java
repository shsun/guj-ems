package de.guj.ems.mobile.sdk.controllers;

import de.guj.ems.mobile.sdk.controllers.adserver.IAdResponse;

/**
 * Interface for ad response processing. In most cases the listener
 * processResponse will be called with an object holding the (possibly empty)
 * response.
 * 
 * If an error occured, it is passed to processError
 * 
 * @author stein16
 * 
 */
public interface IAdResponseHandler {

	public void processResponse(IAdResponse response);

	public void processError(String msg);

	public void processError(String msg, Throwable t);

}
