/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package org.ormma.controller.listeners;

import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import org.ormma.controller.OrmmaSensorController;

import de.guj.ems.mobile.sdk.util.SdkLog;

/**
 * The listener interface for receiving accelerometer events.
 * The class that is interested in processing a accelerometer
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addAccelListener<code> method. When
 * the accel event occurs, that object's appropriate
 * method is invoked.
 *
 * @see AccelEvent
 */
public class AccelListener implements SensorEventListener {
	
	private final static String TAG = "AccelListener";

//constants for determining events
	private static final int FORCE_THRESHOLD = 1000;
	private static final int TIME_THRESHOLD = 100;
	private static final int SHAKE_TIMEOUT = 500;
	private static final int SHAKE_DURATION = 2000;
	private static final int SHAKE_COUNT = 2;
	

	//parent controller
	OrmmaSensorController mSensorController;
	String mKey;

	//counts of registered listeners
	int registeredTiltListeners = 0;
	int registeredShakeListeners = 0;
	int registeredHeadingListeners = 0;

	private SensorManager sensorManager;
	private int mSensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
	private long mLastForce;
	private int mShakeCount;
	private long mLastTime;
	private long mLastShake;
	private float[] mMagVals;
	private float[] mAccVals = { 0, 0, 0 };
	private boolean bMagReady;
	private boolean bAccReady;
	private float[] mLastAccVals = { 0, 0, 0 };
	private float[] mActualOrientation = { -1, -1, -1 };

	/**
	 * Instantiates a new accel listener.
	 *
	 * @param ctx the ctx
	 * @param sensorController the sensor controller
	 */
	public AccelListener(Context ctx, OrmmaSensorController sensorController) {
		// mCtx = ctx;
		mSensorController = sensorController;
		sensorManager = (SensorManager) ctx.getSystemService(Context.SENSOR_SERVICE);

	}

	/**
	 * Sets the sensor delay.
	 *
	 * @param delay the new sensor delay
	 */
	public void setSensorDelay(int delay) {
		mSensorDelay = delay;
		if ((registeredTiltListeners > 0) || (registeredShakeListeners > 0)) {
			stop();
			start();
		}
	}

	/**
	 * Start tracking tilt.
	 */
	public void startTrackingTilt() {
		if (registeredTiltListeners == 0) 
			start();
		registeredTiltListeners++;
	}

	/**
	 * Stop tracking tilt.
	 */
	public void stopTrackingTilt() {
		if (registeredTiltListeners > 0 && --registeredTiltListeners == 0) {
				stop();
		}
	}

	/**
	 * Start tracking shake.
	 */
	public void startTrackingShake() {
		if (registeredShakeListeners == 0) {
			setSensorDelay(SensorManager.SENSOR_DELAY_GAME);
			start();
		}
		registeredShakeListeners++;
	}

	/**
	 * Stop tracking shake.
	 */
	public void stopTrackingShake() {
		if (registeredShakeListeners > 0 && --registeredShakeListeners == 0) {
				setSensorDelay(SensorManager.SENSOR_DELAY_NORMAL);
				stop();
			}
	}

	/**
	 * Start tracking heading.
	 */
	public void startTrackingHeading() {
		if (registeredHeadingListeners == 0)
			startMag();
		registeredHeadingListeners++;
	}

	/**
	 * Start mag.
	 */
	private void startMag() {
		List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if (list.size() > 0) {
			this.sensorManager.registerListener(this, list.get(0), mSensorDelay);
			start();
		} else {
			// Call fail
		}
	}

	/**
	 * Stop tracking heading.
	 */
	public void stopTrackingHeading() {
		if (registeredHeadingListeners > 0 && --registeredHeadingListeners == 0) {
			stop();
		}
	}

	/**
	 * Start.
	 */
	private void start() {
		List<Sensor> list = this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (list.size() > 0) {
			this.sensorManager.registerListener(this, list.get(0), mSensorDelay);
		} else {
			// Call fail
		}
	}

	/**
	 * Stop.
	 */
	public void stop() {
		if ((registeredHeadingListeners == 0) && (registeredShakeListeners == 0) && (registeredTiltListeners == 0)) {
			sensorManager.unregisterListener(this);
		}
	}

	/* (non-Javadoc)
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
	 */
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	/* (non-Javadoc)
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	public void onSensorChanged(SensorEvent event) {
		switch (event.sensor.getType()) {
		case Sensor.TYPE_MAGNETIC_FIELD:
			// SdkLog.d("xxx", "mag");
			mMagVals = event.values.clone();
			bMagReady = true;
			break;
		case Sensor.TYPE_ACCELEROMETER:
			// SdkLog.d("xxx", "acc");
			mLastAccVals = mAccVals;
			mAccVals = event.values.clone();
			bAccReady = true;
			break;
		}
		if (mMagVals != null && mAccVals != null && bAccReady && bMagReady) {
			bAccReady = false;
			bMagReady = false;
			float[] R = new float[9];
			float[] I = new float[9];
			SensorManager.getRotationMatrix(R, I, mAccVals, mMagVals);

			mActualOrientation = new float[3];

			SensorManager.getOrientation(R, mActualOrientation);
			mSensorController.onHeadingChange(mActualOrientation[0]);
		}
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			long now = System.currentTimeMillis();

			if ((now - mLastForce) > SHAKE_TIMEOUT) {
				mShakeCount = 0;
			}

			if ((now - mLastTime) > TIME_THRESHOLD) {
				long diff = now - mLastTime;
				
				float speed = Math.abs(mAccVals[0] + mAccVals[1]
						+ mAccVals[2] - mLastAccVals[0]
						- mLastAccVals[1] - mLastAccVals[2])
						/ diff * 10000;
				if (speed > FORCE_THRESHOLD) {

					if ((++mShakeCount >= SHAKE_COUNT) && (now - mLastShake > SHAKE_DURATION)) {
						mLastShake = now;
						mShakeCount = 0;
						mSensorController.onShake();
					}
					mLastForce = now;
				}
				mLastTime = now;
				mSensorController.onTilt(mAccVals[0], mAccVals[1],
						mAccVals[2]);

			}
		}
	}

	/**
	 * Gets the heading.
	 *
	 * @return the heading
	 */
	public float getHeading() {
		return mActualOrientation[0];
	}

	/**
	 * Stop all listeners.
	 */
	public void stopAllListeners() {
		registeredTiltListeners = 0;
		registeredShakeListeners = 0;
		registeredHeadingListeners = 0;
		try {
			stop();
		} catch (Exception e) {
			SdkLog.e(TAG, "Error stopping acceleration listeners.", e);
		}
	}

}
