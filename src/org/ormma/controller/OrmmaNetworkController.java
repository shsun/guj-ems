/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package org.ormma.controller;

import org.ormma.controller.util.OrmmaNetworkBroadcastReceiver;
import org.ormma.view.OrmmaView;

import android.Manifest.permission;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.webkit.JavascriptInterface;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;

/**
 * The Class OrmmaNetworkController.  OrmmaController for interacting with network states
 */
public class OrmmaNetworkController extends OrmmaController {
	
	private static final String SdkLog_TAG = "OrmmaNetworkController";
	
	private ConnectivityManager mConnectivityManager;
	private int mNetworkListenerCount;
	private OrmmaNetworkBroadcastReceiver mBroadCastReceiver;
	private IntentFilter mFilter;

	/**
	 * Instantiates a new ormma network controller.
	 *
	 * @param adView the ad view
	 * @param context the context
	 */
	public OrmmaNetworkController(OrmmaView adView, Context context) {
		super(adView, context);
		mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
	}

	/**
	 * Gets the network.
	 *
	 * @return the network
	 */
	@JavascriptInterface
	public String getNetwork() {
		
		Context c = SdkUtil.getContext();
		if (c.getPackageManager().checkPermission(permission.ACCESS_NETWORK_STATE, c.getPackageName()) != PackageManager.PERMISSION_GRANTED) {
			SdkLog.w(SdkLog_TAG, "Access Network State not granted in Manifest - assuming ONLINE.");
			return "unknown";
		}
		
		NetworkInfo ni = mConnectivityManager.getActiveNetworkInfo();
        String networkType = "unknown";
		if (ni == null){
			networkType = "offline";
		}
		else{
			switch (ni.getState()) {
			case UNKNOWN:
				networkType = "unknown";
				break;
			case DISCONNECTED:
				networkType = "offline";
				break;
			default:
				int type = ni.getType();
				if (type == ConnectivityManager.TYPE_MOBILE)
					networkType = "cell";
				else if (type == ConnectivityManager.TYPE_WIFI)
					networkType = "wifi";
			}
		}
		SdkLog.d(SdkLog_TAG, "getNetwork: " + networkType);
		return networkType;
	}

	/**
	 * Start network listener.
	 */
	@JavascriptInterface
	public void startNetworkListener() {
		if (mNetworkListenerCount == 0) {
			mBroadCastReceiver = new OrmmaNetworkBroadcastReceiver(this);
			mFilter = new IntentFilter();
			mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

		}
		mNetworkListenerCount++;
		mContext.registerReceiver(mBroadCastReceiver, mFilter);
	}

	/**
	 * Stop network listener.
	 */
	@JavascriptInterface
	public void stopNetworkListener() {
		mNetworkListenerCount--;
		if (mNetworkListenerCount == 0) {
			mContext.unregisterReceiver(mBroadCastReceiver);
			mBroadCastReceiver = null;
			mFilter = null;

		}
	}

	/**
	 * On connection changed.
	 */
	public void onConnectionChanged() {
		String script = "window.ormmaview.fireChangeEvent({ network: \'" + getNetwork() + "\'});";
		SdkLog.d(SdkLog_TAG, script );
		mOrmmaView.injectJavaScript(script);
	}

	/* (non-Javadoc)
	 * @see com.ormma.controller.OrmmaController#stopAllListeners()
	 */
	@Override
	public void stopAllListeners() {
		mNetworkListenerCount = 0;
		try {
			mContext.unregisterReceiver(mBroadCastReceiver);
		} catch (Exception e) {
		}
	}

}
