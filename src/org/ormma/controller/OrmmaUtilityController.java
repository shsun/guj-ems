/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package org.ormma.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ormma.view.OrmmaView;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;
import android.text.TextUtils;
import android.webkit.JavascriptInterface;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import de.guj.ems.mobile.sdk.util.SdkLog;

/**
 * The Class OrmmaUtilityController. Main ormma controller. initiates the
 * others.
 */
public class OrmmaUtilityController extends OrmmaController {

	/**
	 * The Constant TAG.
	 */
	private static final String SdkLog_TAG = "OrmmaUtilityController";

	// other controllers
	private OrmmaAssetController mAssetController;
	private OrmmaDisplayController mDisplayController;
	private OrmmaLocationController mLocationController;
	private OrmmaNetworkController mNetworkController;
	private OrmmaSensorController mSensorController;

	// android calendar handling projection array
	@SuppressLint("InlinedApi")
	private static final String[] EVENT_PROJECTION = new String[] {
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ? Calendars._ID : "0", // 0
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ? Calendars.ACCOUNT_NAME : "1", // 1
			Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH ? Calendars.CALENDAR_DISPLAY_NAME : "2" };

	// android calendar handling content provider uris below API 14
	private final static Uri CALENDAR_PROVIDER_URI = Build.VERSION.SDK_INT >= 8 ? Uri
			.parse("content://com.android.calendar/calendars") : Uri
			.parse("content://calendar/calendars");

	private final static Uri EVENTS_PROVIDER_URI = Build.VERSION.SDK_INT >= 8 ? Uri
			.parse("content://com.android.calendar/events") : Uri
			.parse("content://calendar/events");

	private final static Uri REMINDERS_PROVIDER_URI = Build.VERSION.SDK_INT >= 8 ? Uri
			.parse("content://com.android.calendar/reminders") : Uri
			.parse("content://calendar/reminders");

	/**
	 * Instantiates a new ormma utility controller.
	 * 
	 * @param adView
	 *            the ad view
	 * @param context
	 *            the context
	 */
	public OrmmaUtilityController(OrmmaView adView, Context context) {

		super(adView, context);

		mAssetController = new OrmmaAssetController(adView, context);
		mDisplayController = new OrmmaDisplayController(adView, context);
		mLocationController = new OrmmaLocationController(adView, context);
		mNetworkController = new OrmmaNetworkController(adView, context);
		mSensorController = new OrmmaSensorController(adView, context);

		adView.addJavascriptInterface(mAssetController,
				"ORMMAAssetsControllerBridge");
		adView.addJavascriptInterface(mDisplayController,
				"ORMMADisplayControllerBridge");
		adView.addJavascriptInterface(mLocationController,
				"ORMMALocationControllerBridge");
		adView.addJavascriptInterface(mNetworkController,
				"ORMMANetworkControllerBridge");
		adView.addJavascriptInterface(mSensorController,
				"ORMMASensorControllerBridge");
	}

	/**
	 * Inits the controller. injects state info
	 * 
	 * @param density
	 *            the density
	 */
	public void init(float density) {
		String injection = "window.ormmaview.fireChangeEvent({ state: \'default\',"
				+ " network: \'"
				+ mNetworkController.getNetwork()
				+ "\',"
				+ " size: "
				+ mDisplayController.getSize()
				+ ","
				+ " maxSize: "
				+ mDisplayController.getMaxSize()
				+ ","
				+ " screenSize: "
				+ mDisplayController.getScreenSize()
				+ ","
				+ " defaultPosition: { x:"
				+ (int) (mOrmmaView.getLeft() / density)
				+ ", y: "
				+ (int) (mOrmmaView.getTop() / density)
				+ ", width: "
				+ (int) (mOrmmaView.getWidth() / density)
				+ ", height: "
				+ (int) (mOrmmaView.getHeight() / density)
				+ " },"
				+ " orientation:"
				+ mDisplayController.getOrientation()
				+ ","
				+ getSupports() + " });";
		SdkLog.d(SdkLog_TAG, "init: injection: " + injection);
		mOrmmaView.injectJavaScript(injection);

	}

	/**
	 * Gets the supports object. Examines application permissions
	 * 
	 * @return the supports
	 */
	private String getSupports() {
		String supports = "supports: [ 'level-1', 'level-2', 'level-3', 'screen', 'orientation', 'network', 'heading'";

		boolean p = mLocationController.allowLocationServices()
				&& ((mContext
						.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) || (mContext
						.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED));
		if (p)
			supports += ", 'location'";

		p = mContext
				.checkCallingOrSelfPermission(android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
		if (p)
			supports += ", 'sms'";

		p = mContext
				.checkCallingOrSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED;
		if (p)
			supports += ", 'phone'";

		p = ((mContext
				.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) && (mContext
				.checkCallingOrSelfPermission(android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED));
		if (p)
			supports += ", 'calendar'";

		supports += ", 'video'";

		supports += ", 'audio'";

		supports += ", 'map'";

		supports += ", 'email' ]";
		SdkLog.d(SdkLog_TAG, "getSupports: " + supports);
		return supports;

	}

	/**
	 * Ready.
	 */
	@JavascriptInterface
	public void ready() {
		mOrmmaView.injectJavaScript("Ormma.setState(\"" + mOrmmaView.getState()
				+ "\");");
		mOrmmaView.injectJavaScript("ORMMAReady();");
	}

	/**
	 * Send an sms.
	 * 
	 * @param recipient
	 *            the recipient
	 * @param body
	 *            the body
	 */
	@JavascriptInterface
	public void sendSMS(String recipient, String body) {
		SdkLog.d(SdkLog_TAG, "sendSMS: recipient: " + recipient + " body: " + body);
		Intent sendIntent = new Intent(Intent.ACTION_VIEW);
		sendIntent.putExtra("address", recipient);
		sendIntent.putExtra("sms_body", body);
		sendIntent.setType("vnd.android-dir/mms-sms");
		sendIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(sendIntent);
	}

	/**
	 * Send an email.
	 * 
	 * @param recipient
	 *            the recipient
	 * @param subject
	 *            the subject
	 * @param body
	 *            the body
	 */
	@JavascriptInterface
	public void sendMail(String recipient, String subject, String body) {
		SdkLog.d(SdkLog_TAG, "sendMail: recipient: " + recipient + " subject: "
				+ subject + " body: " + body);
		Intent i = new Intent(Intent.ACTION_SEND);
		i.setType("plain/text");
		i.putExtra(android.content.Intent.EXTRA_EMAIL,
				new String[] { recipient });
		i.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
		i.putExtra(android.content.Intent.EXTRA_TEXT, body);
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mContext.startActivity(i);
	}
	
	@JavascriptInterface
	public void storePicture(String url) {
		mAssetController.storePicture(url);
	}

	/**
	 * Creates the tel url.
	 * 
	 * @param number
	 *            the number
	 * @return the string
	 */
	private String createTelUrl(String number) {
		if (TextUtils.isEmpty(number)) {
			return null;
		}

		StringBuilder buf = new StringBuilder("tel:");
		buf.append(number);
		return buf.toString();
	}

	/**
	 * Make call.
	 * 
	 * @param number
	 *            the number
	 */
	@JavascriptInterface
	public void makeCall(String number) {
		SdkLog.d(SdkLog_TAG, "makeCall: number: " + number);
		String url = createTelUrl(number);
		if (url == null) {
			mOrmmaView.raiseError("Bad Phone Number", "makeCall");
		} else {
			Intent i = new Intent(Intent.ACTION_CALL, Uri.parse(url.toString()));
			i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mContext.startActivity(i);
		}
	}

	/**
	 * Creates a calendar event.
	 * 
	 * @param date
	 *            the date
	 * @param title
	 *            the title
	 * @param body
	 *            the body
	 */
	@SuppressLint("NewApi")
	@JavascriptInterface
	public void createEvent(final String date, final String title,
			final String body) {
		SdkLog.d(SdkLog_TAG, "createEvent: date: " + date + " title: " + title
				+ " body: " + body);
		try {
			final ContentResolver cr = mContext.getContentResolver();
			Cursor cursor = cr.query(
					Build.VERSION.SDK_INT >= 14 ? Calendars.CONTENT_URI
							: CALENDAR_PROVIDER_URI, EVENT_PROJECTION, null,
					null, null);
			if (cursor != null && cursor.moveToFirst()) {
				final String[] calNames = new String[cursor.getCount()];
				final int[] calIds = new int[cursor.getCount()];
				final List<Map<String, String>> entries = new ArrayList<Map<String, String>>();

				for (int i = 0; i < cursor.getCount(); i++) {
					Map<String, String> entry = new HashMap<String, String>();
					entry.put("ID", cursor.getString(0));
					entry.put("NAME", cursor.getString(2));
					entry.put("EMAILID", cursor.getString(1));
					entries.add(entry);
					calIds[i] = cursor.getInt(0);
					calNames[i] = cursor.getString(2);
					cursor.moveToNext();
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
				builder.setTitle("Auswahl Kalender:");
				ListAdapter adapter = new SimpleAdapter(mContext, entries,
						android.R.layout.two_line_list_item, new String[] {
								"NAME", "EMAILID" }, new int[] { android.R.id.text1, android.R.id.text2 });
				
				builder.setSingleChoiceItems(adapter, -1,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								addCalendarEvent(calIds[which], date, title,
										body);
								dialog.cancel();
							}

						});

				builder.create().show();
			} else {
				Toast.makeText(mContext, "Kein aktiver Kalender gefunden!",
						Toast.LENGTH_SHORT).show();
			}
			if (cursor != null) {
				cursor.close();
			}
		} catch (Exception e) {
			SdkLog.e(SdkLog_TAG, "Fehler beim Erzeugen des Kalendereintrags.", e);
		}
	}

	/**
	 * Add event into Calendar
	 * 
	 * @param calendarID
	 *            the callId
	 * @param date
	 *            the date
	 * @param title
	 *            the title
	 * @param body
	 *            the body
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@JavascriptInterface
	private void addCalendarEvent(final int callId, final String date,
			final String title, final String body) {
		long dtStart = Long.parseLong(date);
		long dtEnd = dtStart + 60 * 1000 * 60;
		final ContentResolver cr = mContext.getContentResolver();
		ContentValues cv = new ContentValues();
		cv.put(Events.CALENDAR_ID, callId);
		cv.put(Events.TITLE, title);
		cv.put(Events.DTSTART, dtStart);
		cv.put(Events.EVENT_TIMEZONE, Events.CALENDAR_TIME_ZONE);
		cv.put(Events.DESCRIPTION, body);
		cv.put(Events.DTEND, dtEnd);

		try {
			Uri newEvent = cr.insert(
					Build.VERSION.SDK_INT >= 14 ? Events.CONTENT_URI
							: EVENTS_PROVIDER_URI, cv);
			if (newEvent != null) {
				long id = Long.parseLong(newEvent.getLastPathSegment());
				ContentValues values = new ContentValues();
				values.put(Reminders.EVENT_ID, id);
				values.put(Reminders.METHOD, Reminders.METHOD_ALERT);
				values.put(Reminders.MINUTES, 15); // 15 minutes
				cr.insert(
						Build.VERSION.SDK_INT >= 14 ? Reminders.CONTENT_URI
								: REMINDERS_PROVIDER_URI, values);
	
				Toast.makeText(mContext, "Danke! Termin mit Erinnerung in Kalender eingetragen.",
						Toast.LENGTH_SHORT).show();
	
			} else {
				Toast.makeText(mContext, "Der Termin konnte leider nicht eingetragen werden.",
						Toast.LENGTH_SHORT).show();
			}
		}
		catch (Exception e) {
			SdkLog.e(SdkLog_TAG, "Error inserting event in to calendar.", e);
		}
	}

	/**
	 * Copy text from jar into asset dir.
	 * 
	 * @param alias
	 *            the alias
	 * @param source
	 *            the source
	 * @return the string
	 */
	public String copyTextFromJarIntoAssetDir(String alias, String source) {
		return mAssetController.copyTextFromJarIntoAssetDir(alias, source);
	}

	/**
	 * Sets the max size.
	 * 
	 * @param w
	 *            the w
	 * @param h
	 *            the h
	 */
	@JavascriptInterface
	public void setMaxSize(int w, int h) {
		mDisplayController.setMaxSize(w, h);
	}

	/**
	 * Write to disk wrapping with ormma stuff.
	 * 
	 * @param is
	 *            the iinput stream
	 * @param currentFile
	 *            the file to write to
	 * @param storeInHashedDirectory
	 *            store in a directory based on a hash of the input
	 * @param injection
	 *            and additional javascript to insert
	 * @param bridgePath
	 *            the path the ormma javascript bridge
	 * @param ormmaPath
	 *            the ormma javascript
	 * @return the string
	 * @throws IllegalStateException
	 *             the illegal state exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String writeToDiskWrap(InputStream is, String currentFile,
			boolean storeInHashedDirectory, String injection,
			String bridgePath, String ormmaPath) throws IllegalStateException,
			IOException {
		return mAssetController.writeToDiskWrap(is, currentFile,
				storeInHashedDirectory, injection, bridgePath, ormmaPath);
	}

	/**
	 * Write to disk wrapping with ormma stuff.
	 * 
	 * @param data
	 *            raw data
	 * @param currentFile
	 *            the file to write to
	 * @param storeInHashedDirectory
	 *            store in a directory based on a hash of the input
	 * @param injection
	 *            and additional javascript to insert
	 * @param bridgePath
	 *            the path the ormma javascript bridge
	 * @param ormmaPath
	 *            the ormma javascript
	 * @return the string
	 * @throws IllegalStateException
	 *             the illegal state exception
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public String writeToDiskWrap(String data, String currentFile,
			boolean storeInHashedDirectory, String injection,
			String bridgePath, String ormmaPath) throws IllegalStateException,
			IOException {
		return mAssetController.writeToDiskWrap(data, currentFile,
				storeInHashedDirectory, injection, bridgePath, ormmaPath);
	}

	/**
	 * Activate a listener
	 * 
	 * @param event
	 *            the event
	 */
	@JavascriptInterface
	public void activate(String event) {
		SdkLog.d(SdkLog_TAG, "activate: " + event);
		if (event.equalsIgnoreCase(Defines.Events.NETWORK_CHANGE)) {
			mNetworkController.startNetworkListener();
		} else if (mLocationController.allowLocationServices()
				&& event.equalsIgnoreCase(Defines.Events.LOCATION_CHANGE)) {
			mLocationController.startLocationListener();
		} else if (event.equalsIgnoreCase(Defines.Events.SHAKE)) {
			mSensorController.startShakeListener();
		} else if (event.equalsIgnoreCase(Defines.Events.TILT_CHANGE)) {
			mSensorController.startTiltListener();
		} else if (event.equalsIgnoreCase(Defines.Events.HEADING_CHANGE)) {
			mSensorController.startHeadingListener();
		} else if (event.equalsIgnoreCase(Defines.Events.ORIENTATION_CHANGE)) {
			mDisplayController.startConfigurationListener();
		}
	}

	/**
	 * Deactivate a listener
	 * 
	 * @param event
	 *            the event
	 */
	@JavascriptInterface
	public void deactivate(String event) {
		SdkLog.d(SdkLog_TAG, "deactivate: " + event);
		if (event.equalsIgnoreCase(Defines.Events.NETWORK_CHANGE)) {
			mNetworkController.stopNetworkListener();
		} else if (event.equalsIgnoreCase(Defines.Events.LOCATION_CHANGE)) {
			mLocationController.stopAllListeners();
		} else if (event.equalsIgnoreCase(Defines.Events.SHAKE)) {
			mSensorController.stopShakeListener();
		} else if (event.equalsIgnoreCase(Defines.Events.TILT_CHANGE)) {
			mSensorController.stopTiltListener();
		} else if (event.equalsIgnoreCase(Defines.Events.HEADING_CHANGE)) {
			mSensorController.stopHeadingListener();
		} else if (event.equalsIgnoreCase(Defines.Events.ORIENTATION_CHANGE)) {
			mDisplayController.stopConfigurationListener();
		}

	}

	/**
	 * Delete old ads.
	 */
	public void deleteOldAds() {
		mAssetController.deleteOldAds();
	}
	
	/**
	 * Delete old ad.
	 */
	public void deleteOldAds(String localPath) {
		mAssetController.deleteOldAds(localPath);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.ormma.controller.OrmmaController#stopAllListeners()
	 */
	@Override
	public void stopAllListeners() {
		try {
			mAssetController.stopAllListeners();
			mDisplayController.stopAllListeners();
			mLocationController.stopAllListeners();
			mNetworkController.stopAllListeners();
			mSensorController.stopAllListeners();
		} catch (Exception e) {
			SdkLog.e(SdkLog_TAG, "Error stopping listeners.", e);
		}
	}

	@JavascriptInterface
	public void showAlert(final String message) {
		SdkLog.e(SdkLog_TAG, message);
	}
	
	@JavascriptInterface
	public void addAsset(String url, String alias) {
		mAssetController.addAsset(url, alias);
	}
	
	@JavascriptInterface
	public void removeAsset(String alias) {
		mAssetController.removeAsset(alias);
	}

}
