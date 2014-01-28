package de.guj.ems.mobile.sdk.controllers;

import java.io.Serializable;

/**
 * Interface providing listener for when an error occured during an ad request
 * 
 * @author stein16
 * 
 */
public interface IOnAdErrorListener extends Serializable {

	/**
	 * Listener method called upon request error including exception
	 * 
	 * @param msg
	 *            Error message
	 * @param t
	 *            Exception thrown
	 */
	void onAdError(String msg, Throwable t);

	/**
	 * Listener method called upon request error
	 * 
	 * @param msg
	 *            Error message
	 */
	void onAdError(String msg);

}
