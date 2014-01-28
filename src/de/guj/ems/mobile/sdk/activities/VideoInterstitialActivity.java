package de.guj.ems.mobile.sdk.activities;

import java.util.List;

import org.ormma.view.Browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;
import de.guj.ems.mobile.sdk.R;
import de.guj.ems.mobile.sdk.controllers.IAdResponseHandler;
import de.guj.ems.mobile.sdk.controllers.adserver.IAdResponse;
import de.guj.ems.mobile.sdk.util.SdkLog;
import de.guj.ems.mobile.sdk.util.SdkUtil;
import de.guj.ems.mobile.sdk.util.VASTXmlParser;
import de.guj.ems.mobile.sdk.util.VASTXmlParser.Tracking;

/**
 * This activity is executed when a VAST for video interstitial was delivered
 * from the adserver.
 * 
 * The activity displays native video player with a text hinting at the
 * playtime.
 * 
 * Additionally there is a close button (after the defined skip time of the
 * video).
 * 
 * When the close button is pressed, a target activity is launched if the intent
 * for this activity is passed as an extra called "target". Otherwise the video
 * interstitial activity just finishes.
 * 
 * This activity may not be executed directly but via the
 * InterstitialSwitchReceiver which determines whether there actually is a video
 * interstitial booked, first.
 * 
 * @author stein16
 * 
 */
public final class VideoInterstitialActivity extends Activity implements
		VASTXmlParser.VASTWrapperListener, IAdResponseHandler {

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

	private TextView videoText;

	private MediaPlayer mediaPlayer;

	private int videoWidth;

	private int videoHeight;

	private int videoLength;

	private float videoProportion;

	private boolean muted;

	private double percentPlayed;

	private VASTXmlParser vastXml;

	private volatile boolean videoReady = false;

	private final static int CLOSED = 1;

	private final static int FINISHED = 3;

	private final static int SUSPENDED = 2;

	private final static String TAG = "VideoInterstitialActivity";

	private VideoView videoView;

	private RelativeLayout root;

	private ProgressBar spinner;

	private int status = -1;

	private Intent target;

	private InterstitialThread updateThread;

	private void initFromVastXml() {

		// configure video interstitial adview

		this.videoView = (VideoView) findViewById(R.id.emsVideoView);
		this.videoView
				.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

					@Override
					public void onPrepared(MediaPlayer mp) {
						mediaPlayer = mp;
						mediaPlayer.setVolume(muted ? 0.0f : 1.0f, muted ? 0.0f
								: 1.0f);
						videoReady = true;
					}
				});
		this.videoView
				.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

					@Override
					public void onCompletion(MediaPlayer mp) {
						if (percentPlayed > 0.75) {
							List<String> tx = vastXml
									.getTrackingByType(Tracking.EVENT_COMPLETE);
							SdkLog.i(TAG, "Triggering " + tx.size()
									+ " event_complete tracking requests");
							if (tx != null && tx.size() > 0) {
								String[] txS = new String[tx.size()];
								SdkUtil.httpRequests(tx.toArray(txS));
							}
						} else {
							SdkLog.w(TAG,
									"onCompletion but no 100% played, skipping event_complete.");
						}

						try {
							((AudioManager) getSystemService(Context.AUDIO_SERVICE))
									.abandonAudioFocus(null);
						} catch (Exception e) {
							SdkLog.w(TAG,
									"Could not abandon audio manager focus");
						}

						if (target != null) {
							startActivity(target);
						} else {
							SdkLog.d(TAG,
									"Video interstitial without target. Returning to previous view.");
						}

						finish();
					}
				});

		// onClick handler for video
		videoView.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent e) {

				if (e.getAction() == MotionEvent.ACTION_UP) {
					if (vastXml.getClickThroughUrl() != null) {
						Intent i = new Intent(VideoInterstitialActivity.this,
								Browser.class);
						SdkLog.d(TAG, "open:" + vastXml.getClickThroughUrl());
						i.putExtra(Browser.URL_EXTRA,
								vastXml.getClickThroughUrl());
						i.putExtra(Browser.SHOW_BACK_EXTRA, true);
						i.putExtra(Browser.SHOW_FORWARD_EXTRA, true);
						i.putExtra(Browser.SHOW_REFRESH_EXTRA, true);
						i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

						List<String> tr = vastXml.getClickTrackingUrl();
						SdkLog.i(TAG, "Triggering " + tr.size()
								+ " click tracking requests");
						if (tr != null && tr.size() > 0) {
							String[] trS = new String[tr.size()];
							SdkUtil.httpRequests(tr.toArray(trS));
						}
						startActivity(i);
					} else {
						SdkLog.w(TAG, "Video is not clickable.");
					}
				}
				return true;
			}
		});

		try {
			// parse VAST xml
			this.vastXml = new VASTXmlParser(this, this, getIntent()
					.getExtras().getString("data"));

			if (!this.vastXml.hasWrapper()) {

				SdkLog.i(TAG, "Direct VAST xml response.");

				this.videoView.setVideoURI(Uri.parse(this.vastXml
						.getMediaFileUrl()));
				List<String> im = this.vastXml.getImpressionTrackerUrl();
				SdkLog.i(TAG, "Triggering " + im.size()
						+ " impression tracking requests");
				if (im != null && im.size() > 0) {
					String[] imS = new String[im.size()];
					SdkUtil.httpRequests(im.toArray(imS));
				}

			}

		} catch (Exception e) {
			SdkLog.e(TAG, "Error parsing VAST xml from adserver", e);
			if (this.target != null) {
				startActivity(target);
			}
			finish();

		}

	}

	private void createView(Bundle savedInstanceState) {
		boolean muteTest = getIntent().getExtras().getBoolean("unmuted");
		SdkLog.d(TAG, "Sound settings forced=" + muteTest + ", headset="
				+ SdkUtil.isHeadsetConnected());
		this.muted = !(muteTest || SdkUtil.isHeadsetConnected());

		// (1) set view layout
		setContentView(R.layout.video_interstitial);

		// (2) get views for display and hiding
		this.spinner = (ProgressBar) findViewById(R.id.emsVidIntSpinner);
		this.root = (RelativeLayout) findViewById(R.id.emsVidIntLayout);

		// (4) configure close button
		ImageButton b = (ImageButton) findViewById(R.id.emsVidIntButton);
		b.setVisibility(View.INVISIBLE);
		b.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				try {
					((AudioManager) getSystemService(Context.AUDIO_SERVICE))
							.abandonAudioFocus(null);
				} catch (Exception e) {
					SdkLog.w(TAG, "Could not abandon audio manager focus");
				}

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
								"Video interstitial without target. Returning to previous view.");
					}
					finish();
				}
			}
		});

		// configure sound button
		final ImageButton s = (ImageButton) findViewById(R.id.emsVidIntSndButton);
		s.setVisibility(View.INVISIBLE);
		s.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				muted = !muted; // switch
				mediaPlayer.setVolume(muted ? 0.0f : 1.0f, muted ? 0.0f : 1.0f);
				s.setImageResource(muted ? R.drawable.sound_button_off
						: R.drawable.sound_button_on);
				List<String> tr = muted ? vastXml
						.getTrackingByType(VASTXmlParser.Tracking.EVENT_MUTE)
						: vastXml
								.getTrackingByType(VASTXmlParser.Tracking.EVENT_UNMUTE);
				SdkLog.i(TAG, "Triggering " + tr.size()
						+ (muted ? " event_mute " : " event_unmute ")
						+ "tracking requests");
				if (tr != null && tr.size() > 0) {
					String trS[] = new String[tr.size()];
					SdkUtil.httpRequests(tr.toArray(trS));
				}
			}
		});

		// configure text
		videoText = (TextView) findViewById(R.id.emsVideoText);

		// (3) init video
		this.initFromVastXml();

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (SdkUtil.getContext() == null) {
			SdkUtil.setContext(getApplicationContext());
		}
		if (status < 0) {
			// this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
			this.target = (Intent) getIntent().getExtras().get("target");
			createView(savedInstanceState);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (updateThread != null && updateThread.isAlive()
				&& !InterstitialThread.PAUSED) {
			if (mediaPlayer != null && (mediaPlayer.isPlaying())) {
				try {
					mediaPlayer.pause();
				} catch (IllegalStateException e) {
					SdkLog.w(TAG, "MediaPlayer already released.");
				}
			}
			updateThread.pause();
		}
		if (status != CLOSED && status != SUSPENDED) {
			status = SUSPENDED;
			SdkLog.i(TAG, "Suspending video interstitial activity.");
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		SdkLog.w(TAG, "onRestoreInstanceState!");
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (status == SUSPENDED) {
			try {
				if (mediaPlayer != null && (mediaPlayer.isPlaying())) {
					try {
						mediaPlayer.pause();
					} catch (IllegalStateException e) {
						SdkLog.w(TAG, "MediaPlayer already released.");
					}
					SdkLog.d(TAG, "MediaPlayer stopped.");
				}
			} catch (IllegalStateException e) {
				SdkLog.w(TAG, "Media player already released.");
				mediaPlayer = null;
			}
			if (target != null) {
				SdkLog.d(TAG,
						"Video interstitial resume from suspended mode with target set.");
				startActivity(target);
			} else {
				SdkLog.d(
						TAG,
						"Video interstitial resume from suspended mode without target. Returning to previous view.");
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
			SdkLog.d(TAG, "Video interstitial resume with paused thread.");
			if (mediaPlayer != null) {
				mediaPlayer.start();
				SdkLog.d(TAG, "MediaPlayer resumed.");
			}
			updateThread.unpause();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		SdkLog.w(TAG, "onSaveInstanceState!");
		super.onSaveInstanceState(outState);
	}

	private int getStatusBarHeight() {
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height",
				"dimen", "android");
		try {
			if (resourceId > 0) {
				result = getResources().getDimensionPixelSize(resourceId);
			}
		} catch (Exception e) {
			result = 0;
		}
		SdkLog.d(TAG, "status bar height is " + result + " dpi");
		return result;
	}

	private void adjustVideoView(Configuration newConfig) {

		int offset = getStatusBarHeight()
				+ findViewById(R.id.emsVidIntSndButton).getMeasuredHeight()
				+ findViewById(R.id.emsVideoText).getMeasuredHeight();
		int viewWidth = SdkUtil.getScreenWidth(); // = match_parent
		int viewHeight = videoView.getMeasuredHeight();
		float viewProportion = (float) viewWidth / (float) viewHeight;
		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) videoView
				.getLayoutParams();

		SdkLog.d(TAG, "Text / buttons offset " + offset);
		SdkLog.d(TAG, "View dimensions: " + viewWidth + "x" + viewHeight + " ["
				+ viewProportion + "]");
		SdkLog.d(TAG, "Video dimensions: " + videoWidth + "x" + videoHeight
				+ " [" + videoProportion + "]");

		// view keeps previous height when rotating
		if (viewHeight + offset < SdkUtil.getScreenHeight()
				&& newConfig != null) {
			viewHeight = SdkUtil.getScreenHeight() - offset;
			// recalculate proportion
			viewProportion = (float) viewWidth / (float) viewHeight;
			SdkLog.d(TAG, "Device rotation - corrected view height to "
					+ viewHeight);
		} else if (viewHeight + offset > SdkUtil.getScreenHeight()) {
			viewHeight = SdkUtil.getScreenHeight() - offset;
			// recalculate proportion
			viewProportion = (float) viewWidth / (float) viewHeight;
			SdkLog.d(TAG, "Corrected view height to " + viewHeight);
		}

		if (videoProportion > viewProportion) {
			lp.width = viewWidth;
			lp.height = (int) (viewWidth / videoProportion);
			SdkLog.d(TAG,
					"Adjusted video view height to reflect media aspect ratio. ["
							+ viewHeight + "->" + lp.height + "]");
		} else if (viewProportion > videoProportion) {
			lp.width = (int) (videoProportion * viewHeight);
			lp.height = viewHeight;
			SdkLog.d(TAG,
					"Adjusted video view width to reflect media aspect ratio. ["
							+ viewWidth + "->" + lp.width + "]");
		}

		videoView.setLayoutParams(lp);

	}

	@Override
	protected void onStart() {
		super.onStart();

		if (status < 0) {
			SdkLog.d(TAG, "Create and start new control thread.");
			this.updateThread = new InterstitialThread(new Runnable() {
				boolean q1 = false;
				boolean q2 = false;
				boolean q3 = false;

				private void videoInit() {

					root.getHandler().post(new Runnable() {
						@Override
						public void run() {
							ImageButton sndButton = (ImageButton) root
									.findViewById(R.id.emsVidIntSndButton);
							spinner.setVisibility(View.GONE);

							List<String> tx = vastXml
									.getTrackingByType(Tracking.EVENT_START);
							SdkLog.i(TAG, "Triggering " + tx.size()
									+ " event_start tracking requests");
							if (tx != null && tx.size() > 0) {
								String[] txS = new String[tx.size()];
								SdkUtil.httpRequests(tx.toArray(txS));
							}

							if (muted) {
								List<String> tr = vastXml
										.getTrackingByType(VASTXmlParser.Tracking.EVENT_MUTE);
								SdkLog.i(TAG, "Triggering " + tr.size()
										+ " event_mute tracking requests");
								if (tr != null && tr.size() > 0) {
									String[] trS = new String[tr.size()];
									SdkUtil.httpRequests(tr.toArray(trS));
								}
							}

							videoLength = mediaPlayer.getDuration();
							videoWidth = mediaPlayer.getVideoWidth();
							videoHeight = mediaPlayer.getVideoHeight();
							videoProportion = (float) videoWidth
									/ (float) videoHeight;
							adjustVideoView(null);

							if (mediaPlayer != null) {
								try {
									((AudioManager) getSystemService(Context.AUDIO_SERVICE))
											.requestAudioFocus(
													null,
													AudioManager.STREAM_MUSIC,
													AudioManager.AUDIOFOCUS_GAIN);
								} catch (Exception e) {
									SdkLog.w(TAG,
											"Could not request audio manager focus");
								}
								mediaPlayer.start();
								SdkLog.d(TAG, "MediaPlayer started.");
							}
							sndButton
									.setImageResource(muted ? R.drawable.sound_button_off
											: R.drawable.sound_button_on);
							sndButton.setVisibility(View.VISIBLE);
							if (vastXml.getSkipOffset() <= 0) {
								SdkLog.w(TAG, "skipOffset not set in VAST xml!");
								((ImageButton) root
										.findViewById(R.id.emsVidIntButton))
										.setVisibility(View.VISIBLE);
							}
							SdkLog.d(TAG,
									"Video Interstitial loaded, starting video ["
											+ videoLength + " ms]");
						}
					});
				}

				private void updateView(final boolean canClose,
						final String bottomText) {
					if (root != null && root.getHandler() != null) {
						root.getHandler().post(new Runnable() {

							@Override
							public void run() {
								if (canClose
										&& ((ImageButton) root
												.findViewById(R.id.emsVidIntButton))
												.getVisibility() == View.INVISIBLE) {
									SdkLog.i(TAG,
											"Enabling video cancel button.");
									((ImageButton) root
											.findViewById(R.id.emsVidIntButton))
											.setVisibility(View.VISIBLE);
								}
								videoText.setText(bottomText);
							}
						});
					}
				}

				private void trackEvent() {
					if (percentPlayed >= 25.0 && !q1) {
						List<String> tx = vastXml.getTrackingByType(Tracking.EVENT_FIRSTQ);
						q1 = true;
						SdkLog.i(TAG, "Triggering " + tx.size()
								+ " event_firstq tracking requests");
						if (tx != null && tx.size() > 0) {
							String[] txS = new String[tx.size()];
							SdkUtil.httpRequests(tx.toArray(txS));
						}
					}
					if (percentPlayed >= 50.0 && !q2) {
						List<String> tx = vastXml
								.getTrackingByType(Tracking.EVENT_MID);
						q2 = true;
						SdkLog.i(TAG, "Triggering " + tx.size()
								+ " event_mid tracking requests");
						if (tx != null && tx.size() > 0) {
							String[] txS = new String[tx.size()];
							SdkUtil.httpRequests(tx.toArray(txS));
						}
					}
					if (percentPlayed >= 75.0 && !q3) {
						List<String> tx = vastXml
								.getTrackingByType(Tracking.EVENT_THIRDQ);
						q3 = true;
						SdkLog.i(TAG, "Triggering " + tx.size()
								+ " event_thirdq tracking requests");
						if (tx != null && tx.size() > 0) {
							String[] txS = new String[tx.size()];
							SdkUtil.httpRequests(tx.toArray(txS));
						}
						return;
					}
				}

				@Override
				public void run() {
					boolean loaded = false;
					while (InterstitialThread.SHOW) {
						if (!loaded && videoReady) {
							videoInit();
							loaded = true;
						} else if (loaded && !InterstitialThread.PAUSED) {
							percentPlayed = ((double) videoView
									.getCurrentPosition() / videoLength) * 100.0d;
							String text = getResources().getString(
									R.string.videoRunning);
							boolean close = false;
							if (vastXml.getSkipOffset() > 0) {
								close = (percentPlayed >= vastXml
										.getSkipOffset());
								if (!close) {
									int t = (int) ((vastXml.getSkipOffset() - percentPlayed) / 100.0 * (videoLength / 1000.0));
									text = getResources().getString(
											R.string.videoSkip).replaceAll("#",
											String.valueOf(t));
								}
							} else {
								close = true;
							}

							updateView(close, text);
							trackEvent();

						}
						try {
							Thread.sleep(250);
						} catch (InterruptedException e) {
							SdkLog.e(TAG, "Sleep interrupted while sleeping.",
									e);
						}
					}
					SdkLog.d(TAG, "Terminating control thread.");

					if (mediaPlayer != null && (mediaPlayer.isPlaying())) {
						try {
							mediaPlayer.pause();
						} catch (IllegalStateException e) {
							SdkLog.w(TAG, "MediaPlayer already released.");
						}
						SdkLog.d(TAG, "MediaPlayer paused.");
					}
					if (target != null) {
						startActivity(target);
					} else {
						SdkLog.d(TAG,
								"Video interstitial without target. Returning to previous view.");
					}
					finish();
				}
			}, "Video Interstitial-[" + target + "]");

			updateThread.beforeStart();
			updateThread.start();
			SdkLog.d(TAG, "Video interstitial timer started.");
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
	public void onVASTWrapperFound(final String url) {
		SdkLog.d(TAG, "Wrapped VAST xml response [" + url + "].");
		SdkUtil.adRequest(this).execute(url);
	}

	@Override
	public void processResponse(IAdResponse response) {
		try {
			VASTXmlParser vast = vastXml;
			while (vast.getWrappedVASTXml() != null) {
				vast = vast.getWrappedVASTXml();
			}
			vast.setWrapper(new VASTXmlParser(getApplicationContext(), null,
					response.getResponse()));
			this.videoView
					.setVideoURI(Uri.parse(this.vastXml.getMediaFileUrl()));
			List<String> im = this.vastXml.getImpressionTrackerUrl();
			SdkLog.i(TAG, "Triggering " + im.size()
					+ " impression tracking requests");
			if (im != null && im.size() > 0) {
				String[] imS = new String[im.size()];
				SdkUtil.httpRequests(im.toArray(imS));
			}

		} catch (Exception e) {
			SdkLog.e(TAG, "Error forcing VAST xml response", e);
		}
	}

	@Override
	public void processError(String msg) {
		SdkLog.e(TAG, "Error fetching wrapped VAST xml: " + msg);
	}

	@Override
	public void processError(String msg, Throwable t) {
		SdkLog.e(TAG, "Error fetching wrapped VAST xml: " + msg, t);

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mediaPlayer != null && (mediaPlayer.isPlaying())) {
			try {
				mediaPlayer.pause();
			} catch (IllegalStateException e) {
				SdkLog.w(TAG, "Problem pausing media player.");
			}
			// if switched to landscape we trigger fullscreen event
			if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				List<String> tx = vastXml
						.getTrackingByType(Tracking.EVENT_FULLSCREEN);
				SdkLog.i(TAG, "Triggering " + tx.size()
						+ " event_fullscreen tracking requests");
				if (tx != null && tx.size() > 0) {
					String[] txS = new String[tx.size()];
					SdkUtil.httpRequests(tx.toArray(txS));
				}
			}
			adjustVideoView(newConfig);
			try {
				mediaPlayer.start();
			} catch (IllegalStateException e) {
				SdkLog.w(TAG, "Problem resuming media player.");
			}
		}
	}

}
