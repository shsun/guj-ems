package de.guj.ems.mobile.sdk.controllers;

import java.io.Serializable;

/**
 * Interface providing listener for when an ad request returned an emtpy ad.
 * 
 * @author stein16
 * 
 */
public interface IOnAdEmptyListener extends Serializable {

	/**
	 * Listener method called when ad was empty
	 */
	void onAdEmpty();

}
