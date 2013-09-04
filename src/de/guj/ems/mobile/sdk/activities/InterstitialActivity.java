package de.guj.ems.mobile.sdk.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import de.guj.ems.mobile.sdk.R;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.views.GuJEMSAdView;

/**
 * This activity is executed when an interstitial was delivered from the
 * adserver.
 * 
 * The activity displays the interstitial html and optionally launches a
 * progress bar which runs for "time" seconds.
 * 
 * Additionally there is a close button.
 * 
 * When the progressbar finishes or the close button is pressed, a target
 * activity is launched if the intent for this activity is passed as an extra
 * called "target". Otherwise the interstitial activity just finishes.
 * 
 * This activity may not be executed directly but via the
 * InterstitialSwitchActivity which determines whether there actually is an
 * interstitial booked, first.
 * 
 * @see de.guj.ems.mobile.sdk.activities.InterstititalActivityde.rtv
 * 
 * @author stein16
 * 
 */
public final class InterstitialActivity extends Activity {

	static class InterstitialThread extends Thread {

		static volatile boolean PAUSED = false;

		static volatile boolean SHOW = true;

		public InterstitialThread(Runnable r, String name) {
			super(r, name);
		}

		public void beforeStart() {
			unpause();
			SHOW = true;
		}

		public void beforeStop() {
			SHOW = false;
		}

		public void pause() {
			PAUSED = true;
		}

		public void unpause() {
			PAUSED = false;
		}
	}

	private final static int CLOSED = 1;

	private final static int FINISHED = 3;

	private final static int SUSPENDED = 2;

	private final static String TAG = "InterstitialActivity";

	private GuJEMSAdView adView;

	private ProgressBar progressBar;

	private RelativeLayout root;

	private ProgressBar spinner;

	private int status = -1;

	private Intent target;

	private int time = -1;
	
	private boolean withProgress = false;

	private InterstitialThread updateThread;

	private void createView(Bundle savedInstanceState) {
		String adData = getIntent().getExtras().getString("data");
		this.withProgress = this.time > 0 && !adData.startsWith(getResources().getString(R.string.connectAd));
		// (1) set view layout
		setContentView(this.withProgress ? R.layout.interstitial_progress
				: R.layout.interstitial_noprogress);

		// (2) get views for display and hiding
		this.spinner = (ProgressBar) findViewById(this.withProgress ? R.id.emsIntSpinner
				: R.id.emsIntSpinner2);
		this.root = (RelativeLayout) findViewById(this.withProgress ? R.id.emsIntProgLayout
				: R.id.emsIntLayout);
		this.adView = new GuJEMSAdView(InterstitialActivity.this);

		// (3) configure interstitial adview
		
		adView.loadData(adData, "text/html",
				"utf-8");
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		lp.addRule(RelativeLayout.BELOW, this.withProgress ? R.id.emsIntCloseButton
				: R.id.emsIntCloseButton2);
		adView.setLayoutParams(lp);

		// (4) configure close button
		ImageButton b = (ImageButton) findViewById(this.withProgress ? R.id.emsIntCloseButton
				: R.id.emsIntCloseButton2);
		b.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (updateThread != null && updateThread.isAlive()) {
					try {
						updateThread.beforeStop();
						updateThread.join(100);
						status = CLOSED;
					} catch (InterruptedException e) {
						;
					}
				} else {
					if (target != null) {
						startActivity(target);
					} else {
						SdkLog.d(TAG,
								"Interstitial without target. Returning to previous view.");
					}
					finish();
				}
			}
		});

		// (5) find progress bar if existant
		progressBar = (ProgressBar) findViewById(R.id.emsIntProgBar);
		if (progressBar != null) {
			progressBar.setMax(time);
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (status < 0) {
			//this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
			this.target = (Intent) getIntent().getExtras().get("target");
			Integer time = (Integer) getIntent().getExtras().get("timeout");
			if (time != null) {
				SdkLog.i(TAG, "Creating interstitial with " + time.intValue()
						+ " ms progress bar.");
				this.time = time.intValue();
			} else {
				SdkLog.i(TAG, "Creating interstitial without progress bar.");
			}
			createView(savedInstanceState);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (updateThread != null && updateThread.isAlive()
				&& !InterstitialThread.PAUSED) {
			updateThread.pause();
		}
		if (status != CLOSED && status != SUSPENDED) {
			status = SUSPENDED;
			SdkLog.i(TAG, "Suspending interstitial activity.");
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (status == SUSPENDED) {
			if (target != null) {
				SdkLog.d(TAG,
						"Interstitial resume from suspended mode with target set.");
				startActivity(target);
			} else {
				SdkLog.d(
						TAG,
						"Interstitial resume from suspended mode without target. Returning to previous view.");
			}
			status = FINISHED;
			if (this.updateThread != null && this.updateThread.isAlive()) {
				try {
					updateThread.beforeStop();
					updateThread.join(100);
				} catch (InterruptedException e) {
					;
				}
			}
			finish();
		} else if (updateThread != null && updateThread.isAlive() && status > 0) {
			SdkLog.d(TAG, "Interstitial resume with paused thread.");
			updateThread.unpause();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onStart() {
		super.onStart();

		if (status < 0) {
			SdkLog.d(TAG, "Create and start new control thread.");
			this.updateThread = new InterstitialThread(new Runnable() {

				private volatile long t0 = -1l;

				private void fetchTime() {
					t0 = System.currentTimeMillis();
				}

				public void run() {
					boolean loaded = false;
					while (InterstitialThread.SHOW) {
						if (!loaded && adView.isPageFinished()) {
							
							if (root == null) {
								SdkLog.e(TAG, "This should not happen... interstitial layout is null");
								SdkLog.w(TAG, "Interstitial Thread = " + InterstitialThread.PAUSED + "/" + InterstitialThread.SHOW);
								SdkLog.w(TAG, "status = " + status);
								break;
							}
							
							loaded = true;
							//TODO NPE when rotating device while loading 
							root.getHandler().post(new Runnable() {
								@Override
								public void run() {
									SdkLog.w(
											TAG,
											"root is " + root);
									root.removeView(spinner);
									root.addView(adView);
									fetchTime();
								}
							});
						} else if (loaded && !InterstitialThread.PAUSED) {
							if (withProgress && t0 > 0) {
								int t1 = (int) (System.currentTimeMillis() - t0);
								if (progressBar != null) {
									progressBar.setProgress(t1);
								}
								if (t1 >= time) {
									SdkLog.d(TAG,
											"Interstitial timer finished. [t0="
													+ t0 + " ,t1=" + t1 + "]");
									status = FINISHED;
									break;
								}
							} else if (time <= 0) {
								SdkLog.d(TAG,
										"Interstitial display without timer.");
								return;
							}
						} else {
							Thread.yield();
						}
					}
					SdkLog.d(TAG, "Terminating control thread.");
					if (target != null) {
						startActivity(target);
					} else {
						SdkLog.d(TAG,
								"Interstitial without target. Returning to previous view.");
					}
					t0 = 0;
					finish();
				}
			}, "Interstitial-[" + target + "]-" + time + "ms-"
					+ (int) (Math.random() * 1000.0d));

			updateThread.beforeStart();
			updateThread.start();
			SdkLog.d(TAG, "Interstitial timer started.");
		}

	}

	@Override
	protected void onStop() {
		super.onStop();

		if (updateThread != null && updateThread.isAlive()
				&& status != SUSPENDED && !InterstitialThread.PAUSED) {
			try {
				updateThread.beforeStop();
				updateThread.join(100);
			} catch (InterruptedException e) {
				;
			}
		}
		if (status == FINISHED || status == CLOSED) {
			SdkLog.i(TAG, "Finishing interstitial activity.");
		}
	}
	
	@Override
	public void onBackPressed() {
		if (updateThread != null && updateThread.isAlive()) {
			try {
				updateThread.beforeStop();
				updateThread.join(100);
				status = CLOSED;
			} catch (InterruptedException e) {
				;
			}
		}
		super.onBackPressed();
	}
	

}
