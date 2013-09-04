//
// Copyright (C) 2011, 2012 Mocean Mobile. All Rights Reserved. 
//
package com.MASTAdView.core;


import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Handler;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;


final public class AdDialogFactory
{
	final private Context context;
	private Dialog  dialog;
	final private Handler handler;
	final AdViewContainer adViewContainer;
	
	
	public AdDialogFactory(Context context, AdViewContainer topContainer)
	{
		this.context = context;
		adViewContainer = topContainer;
		
		handler = new Handler();
	}
	
	
	protected ViewGroup.LayoutParams createContainerLayoutParameters(final DialogOptions options)
	{
		return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
	}
	
	
	protected ViewGroup.LayoutParams createAdLayoutParameters(final DialogOptions options)
	{
		if ((options != null) && (options.width != null) && (options.height != null))
		{
			//RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(options.width, options.height);
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);
			layoutParams.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
			layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
			return layoutParams;
		}
		else
		{
			return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
		}
	}
	
	
	protected RelativeLayout.LayoutParams createCloseLayoutParameters(final DialogOptions options)
	{
		RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
		layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.TRUE);
		return layoutParams;
	}
	
	
	public Dialog createDialog(final View ad, final DialogOptions options)
	{
		if ((options != null) && (options.hideTitlebar != null) && (options.hideTitlebar))
		{
			dialog = new Dialog(context, android.R.style.Theme_NoTitleBar_Fullscreen);
		}
		else
		{
			dialog = new Dialog(context, android.R.style.Theme_NoTitleBar);
		}
		
		// remove ad view from any other containers (if needed)
		if (ad.getParent() != null)
		{
			((ViewGroup)ad.getParent()).removeView(ad);
		}
		
		// Create container to hold ad and close button
		final RelativeLayout adContainer = new RelativeLayout(context);
		adContainer.setLayoutParams(createContainerLayoutParameters(options));
		
		// Set background color (if any)
		if ((options != null) && (options.backgroundColor != null))
		{
			adContainer.setBackgroundColor(options.backgroundColor);
		}
		
		// Put ad in new container, and use all available space
		ad.setLayoutParams(createAdLayoutParameters(options));
		adContainer.addView(ad);
		
		final Button closeButton;
		if ((options != null) && (options.noClose != null) && (options.noClose == true))
		{
			// skip close setup (for MRAID open method)
			closeButton = null;
		}
		else
		{
			if ((options != null) && (options.customClose != null) && (options.customClose))
			{
				closeButton = new Button(context);
				closeButton.setText("");
				closeButton.setBackgroundColor(Color.TRANSPARENT);
				
				// Mraid spec requires min. 50 pixel height and width for close area
				closeButton.setMinHeight(50);
				closeButton.setMinWidth(50);

				closeButton.setOnClickListener(createCloseClickListener(ad, options));
			}
			else if (adViewContainer.getCustomCloseButton() != null)
			{
				closeButton = adViewContainer.getCustomCloseButton();
				if (closeButton.getParent() != null)
				{
					((ViewGroup)closeButton.getParent()).removeView(closeButton);
				}
				
				closeButton.setOnClickListener(createCloseClickListener(ad, options));
			}
			else
			{
				// Setup close button
				closeButton = new Button(context);
				
				// Mraid spec requires min. 50 pixel height and width for close area
				closeButton.setMinHeight(50);
				closeButton.setMinWidth(50);
				
				if ((options != null) && (options.closeLabel != null))
				{
					closeButton.setText(options.closeLabel);
				}
				else 
				{
					closeButton.setText("Close"); // XXX string
				}
				
				closeButton.setOnClickListener(createCloseClickListener(ad, options));
			}	
			
			if ((options != null) && (options.showCloseDelay != null) && (options.showCloseDelay > 0))
			{
				if (closeButton != null)
				{
					closeButton.setVisibility(View.INVISIBLE);
					Thread closeThread = new Thread()
					{
						public void run()
						{
							try { Thread.sleep(options.showCloseDelay * 1000); } catch(Exception e) { }
							handler.post(new Runnable()
							{
								public void run()
								{
									closeButton.setVisibility(View.VISIBLE);
								}
							});
						}
					};
					closeThread.setName("[AdDialogFactory] showCloseDelay");
					closeThread.start();
				}
			}
			else
			{
				if (closeButton != null)
				{
					closeButton.setVisibility(View.VISIBLE);
				}
			}
			
			if (closeButton != null)
			{
				closeButton.setLayoutParams(createCloseLayoutParameters(options));
				adContainer.addView(closeButton);
			}
		}

		dialog.setContentView(adContainer);
		dialog.setOnDismissListener(new Dialog.OnDismissListener()
		{
			@Override
			public void onDismiss(DialogInterface dialog)
			{
				//System.out.println("dialog ondismiss: removing views...");
				//adContainer.removeAllViews();
				if (options != null)
				{
					runRunnable(options.dismissRunnable);
				}
			}
		});		

		if ((options != null) && (options.autoCloseDelay != null) && (options.autoCloseDelay > 0))
		{
			Thread closeThread = new Thread()
			{
				public void run()
				{
					try { Thread.sleep(options.autoCloseDelay * 1000); } catch(Exception e) { }
					handler.post(new Runnable()
					{
						public void run()
						{
							closeButton.performClick(); // or dialog.dismiss?
						}
					});
				}
			};
			closeThread.setName("[AdDialogFactory] autoCloseDelay");
			closeThread.start();
		}
		
		return dialog;
	}

	
	private OnClickListener createCloseClickListener(final View ad, final DialogOptions options)
	{
		return new OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				dialog.dismiss();
				
				/*
				AdWebView v;
				if (ad instanceof AdWebView)
				{
					v = (AdWebView)ad;
				}
				else
				{
					v = ((AdViewContainer)ad).getAdWebView();
				}
				
				// Behave as if mraid.close() has been invoked
				//v.injectJavaScript("mraid.close();");
				//v.getMraidInterface().setState(MraidInterface.STATES.DEFAULT);
				 */
				
				adViewContainer.getHandler().sendEmptyMessage(AdMessageHandler.MESSAGE_CLOSE);
				
				if (options != null)
				{
					runRunnable(options.closeRunnable); // XXX not needed???
				}
			}
		};	
	}
	
	
	synchronized private void runRunnable(Runnable worker)
	{
		if (worker != null)
		{
			worker.run();
		}
	}
	
	
	public Dialog getDialog()
	{
		return dialog;
	}
	
	
	final public static class DialogOptions
	{
		Boolean  hideTitlebar		= null; 
		Runnable closeRunnable 		= null;
		Runnable dismissRunnable	= null;
		Integer  backgroundColor	= null;
		Integer	 height 			= null;
		Integer	 width				= null;
		String   closeLabel			= null;
		Boolean  customClose		= null;
		Boolean  noClose			= null;
		//Integer  closeLocation		= null;
		Integer	 showCloseDelay		= null;
		Integer  autoCloseDelay		= null;
	};
}
