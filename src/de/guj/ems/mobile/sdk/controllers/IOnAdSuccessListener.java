package de.guj.ems.mobile.sdk.controllers;

import java.io.Serializable;

/**
 * Interface providing listener for when an ad was received
 * 
 * @author stein16
 * 
 */
public interface IOnAdSuccessListener extends Serializable {

	/**
	 * Listener method called upon receiving an ad
	 */
	void onAdSuccess();

}
