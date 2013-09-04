package de.guj.ems.mobile.sdk.controllers;

import java.io.Serializable;

public interface IOnAdErrorListener extends Serializable {
	
	void onAdError(String msg, Throwable t);
	
	void onAdError(String msg);

}
