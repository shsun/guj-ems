package org.ormma.controller.listeners;

/* License (MIT)
 * Copyright (c) 2008 Nitobi
 * website: http://phonegap.com
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * â€œSoftwareâ€�), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED â€œAS ISâ€�, WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import org.ormma.controller.OrmmaLocationController;

/**
 * The listener interface for receiving location events.
 * The class that is interested in processing a loc
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addLocListener<code> method. When
 * the loc event occurs, that object's appropriate
 * method is invoked.
 *
 * @see LocEvent
 */
public class LocListener implements LocationListener {

	/**
	 * The m ormma location controller.
	 */
	OrmmaLocationController mOrmmaLocationController;
	
	/**
	 * The m loc man.
	 */
	private LocationManager mLocMan;
	// private Location cLoc;
	/**
	 * The m provider.
	 */
	private String mProvider;

	// private long mInterval;

	/**
	 * Instantiates a new loc listener.
	 *
	 * @param c the c
	 * @param interval the interval
	 * @param ormmaLocationController the ormma location controller
	 * @param provider the provider
	 */
	public LocListener(Context c, int interval, OrmmaLocationController ormmaLocationController, String provider) {
		mOrmmaLocationController = ormmaLocationController;
		mLocMan = (LocationManager) c.getSystemService(Context.LOCATION_SERVICE);
		mProvider = provider;
		// mInterval = interval;
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onProviderDisabled(java.lang.String)
	 */
	public void onProviderDisabled(String provider) {
		mOrmmaLocationController.fail();
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onStatusChanged(java.lang.String, int, android.os.Bundle)
	 */
	public void onStatusChanged(String provider, int status, Bundle extras) {
		if (status == 0 && !mOrmmaLocationController.hasLocation()) {
			mOrmmaLocationController.fail();
		}
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onLocationChanged(android.location.Location)
	 */
	public void onLocationChanged(Location location) {
		mOrmmaLocationController.success(location);
	}

	/**
	 * Stop.
	 */
	public void stop() {
		mLocMan.removeUpdates(this);
	}

	/* (non-Javadoc)
	 * @see android.location.LocationListener#onProviderEnabled(java.lang.String)
	 */
	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	/**
	 * Start.
	 */
	public void start() {
		mLocMan.requestLocationUpdates(mProvider, 0, 0, this);
	}

}
