/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package org.ormma.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;
import de.guj.ems.mobile.sdk.R;

/**
 * Activity for implementing Ormma open calls. Configurable via the following
 * extras: URL_EXTRA String : url to load SHOW_BACK_EXTRA boolean (default
 * false) : show the back button SHOW_FORWARD_EXTRA boolean (default false) :
 * show the forward button SHOW_REFRESH_EXTRA boolean (default false) : show the
 * prefresh button
 * 
 * layout is constructed programatically
 */
public class Browser extends Activity {

	/** Extra Constants **/
	public static final String URL_EXTRA = "extra_url";
	public static final String SHOW_BACK_EXTRA = "open_show_back";
	public static final String SHOW_FORWARD_EXTRA = "open_show_forward";
	public static final String SHOW_REFRESH_EXTRA = "open_show_refresh";

	/** Layout Id constants. */
	private static final int ButtonId = 100;
	private static final int WebViewId = 101;
	private static final int ForwardId = 102;
	private static final int BackwardId = 103;

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Build the layout
		RelativeLayout rl = new RelativeLayout(Browser.this);
		WebView webview = new WebView(Browser.this);

		this.getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().setFeatureInt(Window.FEATURE_PROGRESS,
				Window.PROGRESS_VISIBILITY_ON);

		getTheme().setTo(getApplicationContext().getTheme());

		Intent i = getIntent();

		// Build the button bar
		LinearLayout bll = new LinearLayout(this);
		bll.setOrientation(LinearLayout.HORIZONTAL);
		bll.setId(ButtonId);
		bll.setWeightSum(100);
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		lp.addRule(RelativeLayout.ABOVE, ButtonId);
		rl.addView(webview, lp);

		lp = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		rl.addView(bll, lp);

		LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		lp2.weight = 25;
		lp2.gravity = Gravity.CENTER_VERTICAL;

		ImageButton backButton = new ImageButton(Browser.this);
		backButton.setBackgroundColor(getResources().getColor(
				android.R.color.transparent));
		backButton.setId(BackwardId);

		bll.addView(backButton, lp2);
		if (!i.getBooleanExtra(SHOW_BACK_EXTRA, true))

			backButton.setVisibility(View.GONE);

		backButton.setImageResource(R.drawable.leftarrow);
		backButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(android.view.View arg0) {
				WebView wv = (WebView) findViewById(WebViewId);
				if (wv.canGoBack())
					wv.goBack();
				else
					Browser.this.finish();
			}
		});

		ImageButton forwardButton = new ImageButton(Browser.this);
		forwardButton.setBackgroundColor(getResources().getColor(
				android.R.color.transparent));
		forwardButton.setId(ForwardId);
		lp2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.MATCH_PARENT);
		lp2.weight = 25;
		lp2.gravity = Gravity.CENTER_VERTICAL;

		bll.addView(forwardButton, lp2);
		if (!i.getBooleanExtra(SHOW_FORWARD_EXTRA, true))
			forwardButton.setVisibility(View.GONE);
		forwardButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(android.view.View arg0) {
				WebView wv = (WebView) findViewById(WebViewId);
				wv.goForward();
			}
		});

		ImageButton refreshButton = new ImageButton(Browser.this);

		refreshButton.setImageResource(R.drawable.refresh);
		refreshButton.setBackgroundColor(getResources().getColor(
				android.R.color.transparent));
		lp2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		lp2.weight = 25;
		lp2.gravity = Gravity.CENTER_VERTICAL;

		bll.addView(refreshButton, lp2);
		if (!i.getBooleanExtra(SHOW_REFRESH_EXTRA, true))

			refreshButton.setVisibility(View.GONE);
		refreshButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(android.view.View arg0) {
				WebView wv = (WebView) findViewById(WebViewId);
				wv.reload();
			}
		});

		ImageButton closeButton = new ImageButton(Browser.this);

		closeButton.setImageResource(R.drawable.close);
		closeButton.setBackgroundColor(getResources().getColor(
				android.R.color.transparent));
		lp2 = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		lp2.weight = 25;
		lp2.gravity = Gravity.CENTER_VERTICAL;

		bll.addView(closeButton, lp2);
		closeButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(android.view.View arg0) {
				Browser.this.finish();
			}
		});

		// Show progress bar
		getWindow().requestFeature(Window.FEATURE_PROGRESS);

		// Enable cookies
		CookieSyncManager.createInstance(Browser.this);
		CookieSyncManager.getInstance().startSync();
		webview.getSettings().setJavaScriptEnabled(true);
		webview.loadUrl(i.getStringExtra(URL_EXTRA));
		webview.setId(WebViewId);

		webview.setWebViewClient(new WebViewClient() {
			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				Activity a = (Activity) view.getContext();
				Toast.makeText(a, "Ormma Error:" + description,
						Toast.LENGTH_SHORT).show();
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Uri uri = Uri.parse(url);
				try {
					if (url.startsWith("tel:")) {
						Intent intent = new Intent(Intent.ACTION_DIAL, Uri
								.parse(url));
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
						return true;
					}

					if (url.startsWith("mailto:")) {
						Intent intent = new Intent(Intent.ACTION_VIEW, Uri
								.parse(url));
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
						return true;
					}

					if (url.startsWith("http") || url.startsWith("https")) {
						view.loadUrl(url);
						return true;
					}

					Intent intent = new Intent();
					intent.setAction(android.content.Intent.ACTION_VIEW);
					intent.setData(uri);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					startActivity(intent);
					return true;

				} catch (Exception e) {
					try {
						Intent intent = new Intent();
						intent.setAction(android.content.Intent.ACTION_VIEW);
						intent.setData(uri);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(intent);
						return true;
					} catch (Exception e2) {
						return false;
					}
				}

			};

			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				super.onPageStarted(view, url, favicon);
				// ImageButton forwardButton = (ImageButton)
				// findViewById(ForwardId);
				// forwardButton.setImageResource(R.drawable.unrightarrow);
			}

			@Override
			public void onPageFinished(WebView view, String url) {
				super.onPageFinished(view, url);
				ImageButton forwardButton = (ImageButton) findViewById(ForwardId);

				// grey out buttons when appropriate
				if (view.canGoForward()) {
					forwardButton.setImageResource(R.drawable.rightarrow);
				} else {
					forwardButton.setImageResource(0);
				}

			}
		});
		setContentView(rl);

		webview.setWebChromeClient(new WebChromeClient() {

			private String orgTitle = null;

			@Override
			public void onProgressChanged(WebView view, int progress) {
				// show progress bar while loading, url when loaded
				Activity a = (Activity) view.getContext();
				if (orgTitle == null) {
					orgTitle = a.getTitle().toString();
				}
				a.setTitle(R.string.loading);
				a.setProgress(progress * 100);
				if (progress == 100) {
					// TODO Browser: custom layout / title / image?
					a.setTitle(orgTitle);
				}
			}
		});

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		CookieSyncManager.getInstance().stopSync();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		CookieSyncManager.getInstance().startSync();
	}

}
