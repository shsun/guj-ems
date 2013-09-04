/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package org.ormma.controller.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.ormma.controller.OrmmaDisplayController;

/**
 * The Class OrmmaConfigurationBroadcastReceiver.
 */
public class OrmmaConfigurationBroadcastReceiver extends BroadcastReceiver {

	private OrmmaDisplayController mOrmmaDisplayController;
	
	/**
	 * The m last orientation.
	 */
	private int mLastOrientation;

	/**
	 * Instantiates a new ormma configuration broadcast receiver.
	 *
	 * @param ormmaDisplayController the ormma display controller
	 */
	public OrmmaConfigurationBroadcastReceiver(OrmmaDisplayController ormmaDisplayController) {
		mOrmmaDisplayController = ormmaDisplayController;
		mLastOrientation = mOrmmaDisplayController.getOrientation();
	}

	/* (non-Javadoc)
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
			int orientation = mOrmmaDisplayController.getOrientation();
			if (orientation != mLastOrientation) {
				mLastOrientation = orientation;
				mOrmmaDisplayController.onOrientationChanged(mLastOrientation);
			}
		}
	}

}
