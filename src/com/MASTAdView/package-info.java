/** 
 * Welcome to the Mocean Mobile SDK package for Android, version 3.0.1.<BR>Copyright (C) 2011, 2012, 2013 Mocean Mobile. All Rights Reserved. 
 * <P>
 * Mocean Mobile is unlike any other mobile ad serving platform available. Developed specifically for mobile
 * devices, the Mocean Mobile Ad Serving Technology streamlines the many moving parts in mobile advertising
 * for publishers, app stores, and networks.  Mocean Mobile was built by mobile advertising experts so that
 * the real opportunity of this exciting new media could be fully harnessed.
 * <P>
 * Using this SDK, you can monetize mobile apps with higher potential rates of engagement than simple "turnkey"
 * non-contextual mobile advertising platforms. Mojiva's smart technology gives you unprecedented control over
 * the level of interaction with many campaign parameters that can help you improve yields. Embedded mobile app
 * advertising lets you engage a self-selected audience in a frequently-accessed medium. 
 * <P>
 * The Android SDK is distributed in java source code form, and includes a getting started manual to guide you
 * through integrating the SDK into your Android project. Once the SDK is installed, you can begin adding mobile
 * ads to your application. As an Android developer, your primary interface is with the MASTAdView class which
 * implements a custom container for displaying mobile text, image, or MRAID based rich media mobile ads.
 * <P>
 * Getting started is easy as instantiating this view and adding it to your activity. As usual with Android
 * views, you can do this in one of two ways: dynamically by creating the view in code and adding it to a
 * layout, or in an XML layout definition. A brief sample of each follows:
 * <P>
 * Creating an ad view at runtime:
 * <table border="0" cellpadding="1" cellspacing="1" width="90%" style="background-color:#98AFC7"><tr><td><pre> 
 * // Construct view using site and zone registered with mocean mobile web site
 * int myAdSite = 11111; // NOTE: sample value, you must use values provided by your account representative
 * int myAdZone = 22222; // NOTE: sample value, you must use values provided by your account representative
 * MASTAdView adserverView = new MASTAdView(this, myAdSite, myAdZone);
 * //
 * // Set layout: full width of screen, 100 pixels tall
 * adserverView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 100));
 * //
 * // Update view to fetch first ad
 * adserverView.update();
 * //
 * // Add this view to application UI activity
 * linearLayout.addView(adserverView);
 * </pre></td></tr></table>
 * <P>
 * Creating an ad view in an XML layout:
 * <table border="0" cellpadding="1" cellspacing="1" width="90%" style="background-color:#98AFC7"><tr><td><pre>
 * &lt;com.MASTAdView.MASTAdView
 *   android:id="@+id/adViewer1"
 *   android:layout_width="fill_parent"
 *   android:layout_height="100dip"
 *   site="11111"
 *   zone="22222"
 * /&gt;
 * </pre></td></tr></table>
 * <P>
 * @see MASTAdView See the MASTAdView class documentation 
 * for more information and details about customizing the appearance and content of ads for your application.
 */ 
package com.MASTAdView;

//
// Copyright (C) 2011, 2012, 2013 Mocean Mobile. All Rights Reserved. 
//

// NO CODE HERE!!!
// This package exists to provide the package level javadoc documentation for the SDK.

// This space intentionally left blank
